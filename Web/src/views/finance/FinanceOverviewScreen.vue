<template>
    <div>
        <div class="section">
            <h2 class="subtitle">Total dynamic</h2>
            <line-chart
                class="finance-overview-chart"
                v-if="totalChartData"
                :data="totalChartData"
            />
        </div>
        <div class="section">
            <h2 class="subtitle">Income/expense dynamic</h2>
            <line-chart
                class="finance-overview-chart"
                v-if="splitChartData"
                :data="splitChartData"
            />
        </div>
    </div>
</template>

<script lang="ts">
import { Component, Inject, Vue, Watch } from "vue-property-decorator";
import moment, { Moment } from "moment";

import {
    FinanceAnalyticGrouping,
    FinanceAnalyticSeriesDto,
    FinanceApi,
    FinanceOperationDirection,
} from "@/api/FinanceApi";
import Storage from "@/storage/Storage";
import LineChart, {
    ChartData,
    ChartLine,
} from "@/components/charts/LineChart.vue";

interface ChartParameters {
    readonly deviceId: string;
    readonly periodStart: Moment;
    readonly periodEnd: Moment;
}

@Component({ components: { LineChart } })
export default class FinanceOverviewScreen extends Vue {
    @Inject()
    private readonly storage!: Storage;

    private api = new FinanceApi();

    private rawPeriodStart = moment()
        .subtract(1, "year")
        .add(1, "month")
        .startOf("month");
    private rawPeriodEnd = moment().endOf("month");

    private rawTotalChartData: FinanceAnalyticSeriesDto | null = null;
    private rawSplitChartData: FinanceAnalyticSeriesDto | null = null;

    private get periodStart() {
        return moment(this.rawPeriodStart).startOf("month");
    }

    private get periodEnd() {
        return moment(this.rawPeriodEnd).endOf("month");
    }

    private get chartParameters(): ChartParameters | null {
        const deviceId = this.storage.devices.selectedDevice;
        if (deviceId == null) {
            return null;
        }

        return {
            deviceId,
            periodStart: this.periodStart,
            periodEnd: this.periodEnd,
        };
    }

    private get totalChartData() {
        const rawTotalChartData = this.rawTotalChartData;
        if (rawTotalChartData == null) {
            return null;
        }

        return {
            xs: rawTotalChartData.timeline,
            lines: [...rawTotalChartData.data].map(([name, values]) => ({
                name,
                values,
            })),
        } as ChartData<Date>;
    }

    private get splitChartData() {
        const rawSplitChartData = this.rawSplitChartData;
        if (rawSplitChartData == null) {
            return null;
        }

        const extractChartLine: (name: string) => ChartLine | null = (name) => {
            const values = rawSplitChartData.data.get(name.toUpperCase());
            if (values == null) {
                return null;
            }

            return {
                name,
                values: values.map((it) => Math.abs(it)),
            };
        };

        const lines: ChartLine[] = [
            extractChartLine(FinanceOperationDirection.INCOME),
            extractChartLine(FinanceOperationDirection.EXPENSE),
        ]
            .filter((it) => it != null)
            .map((it) => it!);

        return {
            xs: rawSplitChartData.timeline,
            lines,
        } as ChartData<Date>;
    }

    @Watch("chartParameters", { immediate: true })
    private async onChartParametersChanged(
        chartParameters: ChartParameters | null
    ) {
        if (chartParameters == null) {
            return;
        }

        const filter = {
            start: chartParameters.periodStart.toDate(),
            end: chartParameters.periodEnd.toDate(),
            direction: null,
        };

        const rawTotalChartDataAsync = this.api.getSeries(
            chartParameters.deviceId,
            null,
            filter
        );

        const rawSplitChartDataAsync = this.api.getSeries(
            chartParameters.deviceId,
            FinanceAnalyticGrouping.DIRECTION,
            filter
        );

        this.rawTotalChartData = await rawTotalChartDataAsync;
        this.rawSplitChartData = await rawSplitChartDataAsync;
    }
}
</script>

<style lang="scss">
.finance-overview-chart {
    height: 16em;
}
</style>