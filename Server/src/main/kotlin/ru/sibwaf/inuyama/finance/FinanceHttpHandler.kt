package ru.sibwaf.inuyama.finance

import io.javalin.Javalin
import io.javalin.json.fromJsonString
import io.javalin.json.jsonMapper
import ru.sibwaf.inuyama.finance.analytics.FinanceAnalyticFilter
import ru.sibwaf.inuyama.finance.analytics.FinanceAnalyticGrouping
import ru.sibwaf.inuyama.finance.analytics.FinanceAnalyticService
import ru.sibwaf.inuyama.http.HttpHandler
import ru.sibwaf.inuyama.http.subroute
import ru.sibwaf.inuyama.http.webSubroute
import java.time.OffsetDateTime
import java.time.ZoneOffset

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

                get("/accounts") { ctx ->
                    val deviceId = ctx.queryParam("deviceId")!!
                    ctx.json(dataProvider.getAccounts(deviceId))
                }

                subroute("/analytics") {
                    get("/operation-summary") { ctx ->
                        val deviceId = ctx.queryParam("deviceId")!!

                        val grouping = ctx.queryParam("grouping")
                            ?.let { FinanceAnalyticGrouping.valueOf(it.uppercase()) }
                        val filter = ctx.queryParam("filter")!!
                            .let { ctx.jsonMapper().fromJsonString<FinanceAnalyticFilter>(it) }
                        val currency = ctx.queryParam("currency")!!

                        ctx.json(
                            financeAnalyticService.queryOperationSummary(
                                deviceId = deviceId,
                                grouping = grouping,
                                filter = filter,
                                targetCurrency = currency,
                            )
                        )
                    }

                    get("/operation-series") { ctx ->
                        val deviceId = ctx.queryParam("deviceId")!!

                        val grouping = ctx.queryParam("grouping")
                            ?.let { FinanceAnalyticGrouping.valueOf(it.uppercase()) }
                        val filter = ctx.queryParam("filter")!!
                            .let { ctx.jsonMapper().fromJsonString<FinanceAnalyticFilter>(it) }
                        val currency = ctx.queryParam("currency")!!
                        val zoneOffset = ctx.queryParam("zoneOffset")!!
                            .let { ZoneOffset.ofTotalSeconds(it.toInt()) }

                        ctx.json(
                            financeAnalyticService.queryOperationSeries(
                                deviceId = deviceId,
                                grouping = grouping,
                                filter = filter,
                                targetCurrency = currency,
                                zoneOffset = zoneOffset,
                            )
                        )
                    }

                    get("/savings-series") { ctx ->
                        val deviceId = ctx.queryParam("deviceId")!!

                        val currency = ctx.queryParam("currency")!!
                        val start = ctx.queryParam("start")!!.let { OffsetDateTime.parse(it) }
                        val end = ctx.queryParam("end")!!.let { OffsetDateTime.parse(it) }
                        val zoneOffset = ctx.queryParam("zoneOffset")!!.let { ZoneOffset.ofTotalSeconds(it.toInt()) }

                        ctx.json(
                            financeAnalyticService.querySavingsSeries(
                                deviceId = deviceId,
                                targetCurrency = currency,
                                start = start,
                                end = end,
                                zoneOffset = zoneOffset
                            )
                        )
                    }
                }
            }
        }
    }
}
