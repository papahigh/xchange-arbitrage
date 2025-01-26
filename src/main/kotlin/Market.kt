package com.github.papahigh

import org.knowm.xchange.currency.Currency
import org.knowm.xchange.currency.CurrencyPair
import org.knowm.xchange.dto.marketdata.Ticker
import org.knowm.xchange.instrument.Instrument
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.BigDecimal.ONE
import java.math.MathContext
import java.math.RoundingMode
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.math.ln


interface MarketListener {
    fun onMarketUpdate(graph: SymbolGraph<Currency, ExchangeRate>)
}


class ExchangeRate(
    val fromCurrency: Currency,
    val toCurrency: Currency,
    override val from: Int,
    override val to: Int,
    price: BigDecimal
) : WeightedEdge {

    private val _lock = ReentrantReadWriteLock()
    private var _price: BigDecimal = price
    private var _weight: Double = price.toWeight()

    val price: BigDecimal
        get() = _lock.read { _price }

    override val weight: Double
        get() = _lock.read { _weight }

    fun updatePrice(price: BigDecimal) {
        _lock.write {
            _price = price
            _weight = price.toWeight()
        }
    }

    private fun BigDecimal.toWeight(): Double = -ln(this.toDouble())
}


class Market private constructor(
    private val graph: SymbolGraph<Currency, ExchangeRate>,
    private val index: Map<Instrument, ExchangeRates>,
    private val listener: MarketListener?,
) {

    companion object {
        private val log = LoggerFactory.getLogger(Market::class.java)!!
        private val mathContext = MathContext(10, RoundingMode.HALF_UP)

        private fun BigDecimal.invert(): BigDecimal {
            return ONE.divide(this, mathContext)
        }
    }

    fun update(ticker: Ticker) {
        if (ticker.instrument in index) {
            log.debug("Received update for {}", ticker.instrument)
            index[ticker.instrument]?.let {
                it.ask.updatePrice(ticker.ask.invert())
                it.bid.updatePrice(ticker.bid)
            }
            listener?.onMarketUpdate(graph)
        } else {
            log.error("Received update for unknown pair: {}", ticker.instrument)
        }
    }

    private data class ExchangeRates(val ask: ExchangeRate, val bid: ExchangeRate)

    class Builder private constructor(instruments: Collection<CurrencyPair>) {

        companion object {
            fun of(instruments: Collection<CurrencyPair>): Builder {
                return Builder(instruments)
            }
        }

        private val _graph = SymbolGraph.of<Currency, ExchangeRate>(instruments.getSymbols())
        private val _index = mutableMapOf<Instrument, ExchangeRates>()
        private var _listener: MarketListener? = null

        fun withTickers(tickers: Collection<Ticker>): Builder {

            for (ticker in tickers) {

                var pair = ticker.instrument as CurrencyPair
                var base = _graph.indexOf(pair.base)!!
                var counter = _graph.indexOf(pair.counter)!!

                val rate = ExchangeRates(
                    ask = ExchangeRate(
                        from = counter,
                        to = base,
                        fromCurrency = pair.counter,
                        toCurrency = pair.base,
                        price = ticker.ask.invert(),
                    ),
                    bid = ExchangeRate(
                        from = base,
                        to = counter,
                        fromCurrency = pair.base,
                        toCurrency = pair.counter,
                        price = ticker.bid,
                    ),
                )

                _graph.addEdge(rate.ask)
                _graph.addEdge(rate.bid)
                _index[ticker.instrument] = rate
            }

            return this
        }

        fun withListener(listener: MarketListener): Builder {
            _listener = listener
            return this
        }

        fun build(): Market {
            return Market(_graph, _index, _listener)
        }

        private fun Collection<CurrencyPair>.getSymbols(): Collection<Currency> {
            var set = mutableSetOf<Currency>()
            for (pair in this) {
                set.add(pair.base)
                set.add(pair.counter)
            }
            return set
        }
    }
}
