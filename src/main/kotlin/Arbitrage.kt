package com.github.papahigh

import org.knowm.xchange.currency.Currency
import org.slf4j.LoggerFactory.getLogger
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.concurrent.Executors
import kotlin.system.measureNanoTime


interface ArbitrageListener {
    fun onArbitrageFound(source: Currency, arbitrage: Collection<ExchangeRate>)
}

class Arbitrage(
    private val sources: Collection<Currency>,
    private val listener: ArbitrageListener = ArbitrageLogger(),
) : MarketListener {

    private val executor = Executors.newFixedThreadPool(sources.size)

    override fun onMarketUpdate(graph: SymbolGraph<Currency, ExchangeRate>) {
        log.trace("Received market update")

        sources.forEach { source ->
            executor.execute {
                log.debug("Checking arbitrage opportunity for {}", source)

                val nanos = measureNanoTime {
                    val bellmanFord = BellmanFord.of(source, graph)
                    if (bellmanFord.hasNegativeCycle())
                        log.debug("Found negative cycle")
                    else
                        log.debug("No negative cycle found")

                    bellmanFord.negativeCyclePath()?.let {
                        listener.onArbitrageFound(source, it)
                    }
                }

                log.debug("Checking arbitrage opportunity took {} ms", nanos * 1e-6)
            }
        }
    }

    private class ArbitrageLogger : ArbitrageListener {
        override fun onArbitrageFound(source: Currency, arbitrage: Collection<ExchangeRate>) {
            log.info("Found arbitrage opportunity: ${arbitrage.render()}")
        }
    }

    companion object {
        private val log = getLogger(Arbitrage::class.java)!!

        private fun Collection<ExchangeRate>.render(): String {
            var total = BigDecimal.ONE
            val fmt = NumberFormat.getNumberInstance().also { it.maximumFractionDigits = 10 }
            val sb = StringBuilder()
            var it = this.iterator()
            if (it.hasNext()) {
                val curr = it.next()
                total *= curr.price
                sb.append("${curr.fromCurrency} -> ${curr.toCurrency} (${fmt.format(curr.price)})")
            }
            while (it.hasNext()) {
                val curr = it.next()
                total *= curr.price
                sb.append(" -> ${curr.toCurrency} (${fmt.format(curr.price)})")
            }
            sb.append(" | Total product: ${fmt.format(total)}")
            return sb.toString()
        }
    }
}
