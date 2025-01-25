package com.github.papahigh

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.knowm.xchange.currency.Currency
import org.slf4j.LoggerFactory.getLogger
import kotlin.system.measureNanoTime


interface ArbitrageListener {
    fun onArbitrageFound(source: Currency, arbitrage: Collection<ExchangeRate>)
}

class Arbitrage(
    private val sources: Collection<Currency>,
    private val listener: ArbitrageListener = ArbitrageLogger(),
) : MarketListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onMarketUpdate(graph: SymbolGraph<Currency, ExchangeRate>) {
        log.trace("Received market update")

        sources.forEach { source ->
            scope.launch {
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
            val sb = StringBuilder()
            var it = this.iterator()
            while (it.hasNext()) {
                val curr = it.next()
                sb.append("${curr.fromCurrency} -> ${curr.price} ${curr.toCurrency}")
                if (it.hasNext())
                    sb.append(", ")
            }
            return sb.toString()
        }
    }
}
