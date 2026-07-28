package com.devansh.dhanwidget

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable

@Serializable
data class HoldingReturn(
    val symbol: String,
    val pnl: Double,
    val pnlPct: Double,
)

@Serializable
data class PortfolioSummary(
    val currentValue: Double,
    val investedValue: Double,
    val prevCloseValue: Double,
    val holdingsCount: Int,
    /** Best-performing positions by total return %, descending. Defaulted so older stored JSON still decodes. */
    val top: List<HoldingReturn> = emptyList(),
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
            val returns = mutableListOf<HoldingReturn>()
            rows.filter { (h, _) -> h.isin.startsWith("INF") == isEtf }.forEach { (h, meta) ->
                val positionCost = h.avgCostPrice * h.totalQty
                invested += positionCost
                count++
                if (meta != null) {
                    val positionValue = meta.regularMarketPrice * h.totalQty
                    current += positionValue
                    prevClose += meta.prevClose * h.totalQty
                    // Positions with a failed quote are left out of the ranking: their cost-basis
                    // fallback would show a fake 0.00% and sit mid-table.
                    if (positionCost > 0) {
                        val pnl = positionValue - positionCost
                        returns += HoldingReturn(h.tradingSymbol, pnl, pnl / positionCost * 100)
                    }
                } else {
                    // Quote fetch failed for this symbol — fall back to cost basis so it doesn't skew % change.
                    current += positionCost
                    prevClose += positionCost
                }
            }
            return PortfolioSummary(
                current,
                invested,
                prevClose,
                count,
                top = returns.sortedByDescending { it.pnlPct }.take(5),
            )
        }

        return StoredSummaries(
            stocks = summarize(isEtf = false),
            etfs = summarize(isEtf = true),
            updatedAtMillis = System.currentTimeMillis(),
        )
    }
}
