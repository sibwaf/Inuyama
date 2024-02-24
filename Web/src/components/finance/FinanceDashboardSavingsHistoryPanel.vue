<template>
    <div>
        <h2 class="title has-text-centered">Savings history</h2>
        <div class="has-text-centered" v-if="state == STATE_LOADING">
            Loading...
        </div>
        <div class="has-text-centered" v-else-if="state == STATE_ERROR">
            Failed to load data
        </div>
        <line-chart v-else class="finance-dashboard-global-savings-chart" :data="chartData" :xFormatter="formatTimestamp"
            :valueFormatter="formatValue" />
    </div>
</template>

<script lang="ts">
import { Component, Prop, Vue, Watch } from "vue-property-decorator";
import moment, { Moment } from "moment";

import Collections from "@/utility/Collections";
import { makeAutoRegression } from "@/utility/Magic";

import { FinanceAnalyticSeriesDto, FinanceApi } from "@/api/FinanceApi";

import LineChart, { ChartData } from "@/components/charts/LineChart.vue";
import NoDataView from "@/components/NoDataView.vue";

interface Parameters {
    readonly deviceId: string;
    readonly currency: string;
    readonly periodStart: Moment;
    readonly periodEnd: Moment;
}

const REGRESSION_SAMPLE_POINTS = 4;
const PREDICTION_DEPTH = 3;

@Component({ components: { LineChart, NoDataView } })
export default class FinanceDashboardSavingsHistoryPanel extends Vue {

    @Prop() private readonly deviceId!: string;
    @Prop() private readonly currency!: string;

    public readonly STATE_OK = "OK";
    public readonly STATE_LOADING = "LOADING";
    public readonly STATE_ERROR = "ERROR";

    private readonly api = new FinanceApi();

    public state = this.STATE_LOADING;

    private rawPeriodStart = moment().subtract(1, "years").add(1, "month");
    private rawPeriodEnd = moment();

    private realSavingsByCurrencySeries: FinanceAnalyticSeriesDto = {
        timeline: [],
        data: new Map(),
    };

    private loadedCurrency = this.currency;

    private get parameters(): Parameters {
        return {
            deviceId: this.deviceId,
            currency: this.currency,
            periodStart: moment(this.rawPeriodStart).startOf("month"),
            periodEnd: moment(this.rawPeriodEnd).endOf("month"),
        };
    }

    private get realValues() {
        const result: number[] = [];
        for (const [_, values] of this.realSavingsByCurrencySeries.data) {
            for (let i = 0; i < values.length; i++) {
                result[i] = (result[i] ?? 0) + values[i];
            }
        }
        return result;
    }

    private get realTimeline() {
        return this.realSavingsByCurrencySeries.timeline;
    }

    private get previousPredictionValues() {
        const previousValues = Collections.head(this.realValues);

        const result: number[] = [];
        if (previousValues.length >= REGRESSION_SAMPLE_POINTS) {
            Collections.slidingWindow(previousValues, REGRESSION_SAMPLE_POINTS)
                .map(window => makeAutoRegression(window)(0))
                .forEach(it => result.push(it));
        }

        while (result.length < this.realValues.length) {
            // @ts-ignore
            result.splice(0, 0, undefined);
        }

        return result;
    }

    private get futurePredictionValues() {
        const realSavings = Collections.head(this.realValues);
        if (realSavings.length < REGRESSION_SAMPLE_POINTS) {
            return [];
        }

        const regression = makeAutoRegression(Collections.takeLast(realSavings, REGRESSION_SAMPLE_POINTS));

        // start at 1 to skip the current month which is included in previousPredictionValues
        const result: number[] = [];
        for (let i = 1; i < PREDICTION_DEPTH; i++) {
            result.push(regression(i));
        }
        return result;
    }

    private get futurePredictionTimeline() {
        const end = Collections.last(this.realTimeline);
        if (end == null) {
            return [];
        }

        const timeline: Date[] = [];
        for (let i = 0; i < this.futurePredictionValues.length; i++) {
            const nextEnd = moment(end).add(i + 1, "months").toDate();
            timeline.push(nextEnd);
        }
        return timeline;
    }

    public get chartData() {
        const timeline = [...this.realTimeline, ...this.futurePredictionTimeline];

        return {
            xs: timeline,
            lines: [
                {
                    name: "Real",
                    values: this.realValues,
                },
                {
                    name: "Prediction",
                    values: [...this.previousPredictionValues, ...this.futurePredictionValues],
                },
            ],
        } as ChartData<Date>;
    }

    public formatValue(value: number): string {
        return `${value.toFixed(0)} ${this.loadedCurrency}`;
    }

    public formatTimestamp(timestamp: any): string {
        return moment(timestamp).format("MMMM YYYY");
    }

    @Watch("parameters", { immediate: true })
    private async onParametersChanged(parameters: Parameters) {
        try {
            this.state = this.STATE_LOADING;

            this.realSavingsByCurrencySeries = await this.api.getSavingsSeries(
                parameters.deviceId,
                parameters.currency,
                parameters.periodStart.toDate(),
                parameters.periodEnd.toDate()
            );
            this.loadedCurrency = parameters.currency;

            this.state = this.STATE_OK;
        } catch (e) {
            console.error(e);
            this.state = this.STATE_ERROR;
        }
    }
}
</script>

<style lang="scss">
.finance-dashboard-global-savings-chart {
    height: 24em;
}
</style>
