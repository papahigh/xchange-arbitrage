package com.github.papahigh

import info.bitrich.xchangestream.bitfinex.BitfinexStreamingExchange
import io.reactivex.rxjava3.disposables.Disposable
import org.knowm.xchange.ExchangeFactory
import org.knowm.xchange.ExchangeSpecification
import org.knowm.xchange.bitfinex.BitfinexExchange
import org.knowm.xchange.currency.CurrencyPair
import org.knowm.xchange.dto.marketdata.Ticker
import org.knowm.xchange.service.marketdata.params.CurrencyPairsParam
import info.bitrich.xchangestream.core.StreamingExchange as StreamingClient
import info.bitrich.xchangestream.core.StreamingExchangeFactory as StreamingFactory
import org.knowm.xchange.Exchange as ExchangeClient

class Exchange private constructor(
    private val exchangeSpec: ExchangeSpecification,
    private val streamingSpec: ExchangeSpecification
) {

    val exchangeClient: ExchangeClient by lazy { ExchangeFactory.INSTANCE.createExchange(exchangeSpec) }

    val streamingClient: StreamingClient by lazy {
        val client = StreamingFactory.INSTANCE.createExchange(streamingSpec)
        client.connect().blockingAwait()
        return@lazy client
    }

    fun getTickers(currencyPairs: Collection<CurrencyPair>): Collection<Ticker> {
        return exchangeClient.marketDataService.getTickers(GetTickersRequest(currencyPairs))
    }

    fun subscribe(pair: CurrencyPair, onNext: (ticker: Ticker) -> Unit): Disposable {
        return streamingClient.streamingMarketDataService.getTicker(pair).subscribe(onNext)
    }

    private class GetTickersRequest(val target: Collection<CurrencyPair>) : CurrencyPairsParam {
        override fun getCurrencyPairs() = target
    }

    companion object {

        /**
         * Creates an instance of the Exchange class configured for the Bitfinex exchange platform.
         *
         * @param exchangeSpec The exchange specification for the Bitfinex REST API client. Defaults to the
         *                     default exchange specification for the Bitfinex exchange.
         * @param streamingSpec The exchange specification for the Bitfinex streaming API client. Defaults to
         *                      the default exchange specification for the Bitfinex streaming exchange.
         * @return A new instance of the Exchange class configured for Bitfinex.
         */
        fun bitfinex(
            exchangeSpec: ExchangeSpecification = BitfinexExchange().defaultExchangeSpecification,
            streamingSpec: ExchangeSpecification = BitfinexStreamingExchange().defaultExchangeSpecification
        ) = Exchange(exchangeSpec, streamingSpec)
    }
}

