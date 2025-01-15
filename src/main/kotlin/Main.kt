package com.github.papahigh

fun main() {
    var exchange = Exchange.bitfinex()

    exchange.currencyPairs.forEach {
        exchange.getTicker(it).subscribe {
            println("${it.instrument} ask=${it.ask} bid=${it.bid}")
        }
    }
}