package com.devansh.dhanwidget

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable

@Serializable
data class PortfolioSummary(
    val currentValue: Double,
    val investedValue: Double,
    val prevCloseValue: Double,
    val holdingsCount: Int,
) {
    val dayChangePct: Double
        get() = if (prevCloseValue == 0.0) 0.0 else (currentValue - prevCloseValue) / prevCloseValue * 100

    val overallChangePct: Double
        get() = if (investedValue == 0.0) 0.0 else (currentValue - investedValue) / investedValue * 100
}

@Serializable
data class StoredSummaries(
    val stocks: PortfolioSummary,
    val etfs: PortfolioSummary,
    val updatedAtMillis: Long,
)

class HoldingsRepository(
    private val dhanApi: DhanApi,
    private val yahooApi: YahooFinanceApi,
    private val tokenStore: TokenStore,
) {

    suspend fun fetch(): StoredSummaries {
        val accessToken = tokenStore.accessToken ?: error("No access token configured")
        val holdings = dhanApi.getHoldings(accessToken)

        val rows = coroutineScope {
            holdings.map { h ->
                async {
                    val meta = runCatching {
                        yahooApi.getChart("${h.tradingSymbol}.NS", YahooFinanceApiFactory.USER_AGENT)
                    }.getOrNull()?.chart?.result?.firstOrNull()?.meta
                    h to meta
                }
            }.map { it.await() }
        }

        fun summarize(isEtf: Boolean): PortfolioSummary {
            var current = 0.0
            var invested = 0.0
            var prevClose = 0.0
            var count = 0
            rows.filter { (h, _) -> h.isin.startsWith("INF") == isEtf }.forEach { (h, meta) ->
                val positionCost = h.avgCostPrice * h.totalQty
                invested += positionCost
                count++
                if (meta != null) {
                    current += meta.regularMarketPrice * h.totalQty
                    prevClose += meta.prevClose * h.totalQty
                } else {
                    // Quote fetch failed for this symbol — fall back to cost basis so it doesn't skew % change.
                    current += positionCost
                    prevClose += positionCost
                }
            }
            return PortfolioSummary(current, invested, prevClose, count)
        }

        return StoredSummaries(
            stocks = summarize(isEtf = false),
            etfs = summarize(isEtf = true),
            updatedAtMillis = System.currentTimeMillis(),
        )
    }
}
