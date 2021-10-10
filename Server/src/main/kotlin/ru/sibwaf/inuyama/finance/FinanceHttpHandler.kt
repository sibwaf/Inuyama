package ru.sibwaf.inuyama.finance

import io.javalin.Javalin
import io.javalin.plugin.json.JavalinJson
import ru.sibwaf.inuyama.finance.analytics.FinanceAnalyticFilter
import ru.sibwaf.inuyama.finance.analytics.FinanceAnalyticGrouping
import ru.sibwaf.inuyama.finance.analytics.FinanceAnalyticService
import ru.sibwaf.inuyama.http.HttpHandler
import ru.sibwaf.inuyama.http.subroute
import ru.sibwaf.inuyama.http.webSubroute

class FinanceHttpHandler(
    private val dataProvider: FinanceBackupDataProvider,
    private val financeAnalyticService: FinanceAnalyticService
) : HttpHandler {

    override fun Javalin.install() {
        webSubroute {
            subroute("/finance") {
                get("/categories") { ctx ->
                    val deviceId = ctx.queryParam("deviceId")!!
                    ctx.json(dataProvider.getCategories(deviceId))
                }

                subroute("/analytics") {
                    get("/summary") { ctx ->
                        val deviceId = ctx.queryParam("deviceId")!!

                        val grouping = ctx.queryParam("grouping")
                            ?.let { FinanceAnalyticGrouping.valueOf(it.toUpperCase()) }
                        val filter = ctx.queryParam("filter")!!
                            .let { JavalinJson.fromJson(it, FinanceAnalyticFilter::class.java) }

                        ctx.json(financeAnalyticService.querySummary(deviceId, grouping, filter))
                    }
                }
            }
        }
    }
}