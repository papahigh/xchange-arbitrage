package com.github.papahigh


import info.bitrich.xchangestream.bitfinex.BitfinexStreamingExchange
import info.bitrich.xchangestream.core.StreamingExchangeFactory
import io.reactivex.rxjava3.core.Observable
import org.knowm.xchange.ExchangeFactory
import org.knowm.xchange.ExchangeSpecification
import org.knowm.xchange.bitfinex.BitfinexExchange
import org.knowm.xchange.currency.CurrencyPair
import org.knowm.xchange.dto.marketdata.Ticker
import org.knowm.xchange.instrument.Instrument
import org.knowm.xchange.service.marketdata.params.CurrencyPairsParam


class Exchange private constructor(
    private val exchangeSpec: ExchangeSpecification,
    private val streamingSpec: ExchangeSpecification
) {

    private val exchangeClient: ExchangeClient by lazy { ExchangeFactory.INSTANCE.createExchange(exchangeSpec) }
    private val streamingClient: StreamingClient by lazy {
        val client = StreamingExchangeFactory.INSTANCE.createExchange(streamingSpec)
        client.connect().blockingAwait()
        return@lazy client
    }

    val instruments: List<Instrument> by lazy { exchangeClient.exchangeInstruments }
    val currencyPairs: List<CurrencyPair> by lazy { instruments.filterIsInstance<CurrencyPair>() }

    fun getTicker(pair: CurrencyPair): Observable<Ticker> {
        return streamingClient.streamingMarketDataService.getTicker(pair)
    }

    fun getTickers(): Collection<Ticker> {
        return exchangeClient.marketDataService.getTickers(CurrencyPairTickersRequest())
    }

    private inner class CurrencyPairTickersRequest() : CurrencyPairsParam {
        override fun getCurrencyPairs() = this@Exchange.currencyPairs
    }

    companion object {
        fun bitfinex(): Exchange {
            var exchangeSpec = BitfinexExchange().defaultExchangeSpecification
            var streamingSpec = BitfinexStreamingExchange().defaultExchangeSpecification
            return Exchange(exchangeSpec, streamingSpec)
        }
    }
}

typealias ExchangeClient = org.knowm.xchange.Exchange
typealias StreamingClient = info.bitrich.xchangestream.core.StreamingExchange
