<template>
    <div>
        <div class="section">
            <line-chart
                class="finance-prophet-chart"
                v-if="chartData"
                :data="chartData"
                :valueFormatter="chartValueFormatter"
            />
            <no-data-view v-else />
        </div>
    </div>
</template>

<script lang="ts">
import { Component, Inject, Vue, Watch } from "vue-property-decorator";
import moment, { Moment } from "moment";

import Storage from "@/storage/Storage";
import { makeAutoRegression } from "@/utility/Magic";

import { FinanceAnalyticSeriesDto, FinanceApi } from "@/api/FinanceApi";

import LineChart, { ChartData } from "@/components/charts/LineChart.vue";
import NoDataView from "@/components/NoDataView.vue";

interface RealDataParameters {
    readonly deviceId: string;
    readonly currency: string;
    readonly periodStart: Moment;
    readonly periodEnd: Moment;
}

// todo: customizable values
const REGRESSION_SAMPLE_POINTS = 4;
const PREDICTION_DEPTH = 4;

@Component({ components: { LineChart, NoDataView } })
export default class FinanceProphetScreen extends Vue {
    @Inject()
    private readonly storage!: Storage;

    private readonly api = new FinanceApi();

    private rawPeriodStart = moment().subtract(1, "year").add(1, "month");
    private rawPeriodEnd = moment();

    private rawSavingsData: FinanceAnalyticSeriesDto | null = null;

    private chartDataCurrency: string | null = null;

    private get periodStart() {
        return moment(this.rawPeriodStart).startOf("month");
    }

    private get periodEnd() {
        return moment(this.rawPeriodEnd).endOf("month");
    }

    private get realDataParameters(): RealDataParameters | null {
        const deviceId = this.storage.devices.selectedDevice;
        if (deviceId == null) {
            return null;
        }

        const currency = this.storage.ofDevice(deviceId).finance.selectedCurrency;
        if (currency == null) {
            return null;
        }

        return {
            deviceId,
            currency,
            periodStart: this.periodStart,
            periodEnd: this.periodEnd,
        };
    }

    private get realTimeline() {
        return this.rawSavingsData?.timeline;
    }

    private get realSavingsLineValues() {
        const rawSavingsData = this.rawSavingsData;
        if (rawSavingsData == null) {
            return null;
        }

        const result: number[] = [];
        for (const [_, values] of rawSavingsData.data) {
            for (let i = 0; i < values.length; i++) {
                result[i] = (result[i] ?? 0) + values[i];
            }
        }
        return result;
    }

    private get predictionTimeline() {
        const realTimeline = this.realTimeline;
        if (realTimeline == null || realTimeline.length == 0) {
            return [];
        }

        const end = moment(realTimeline[realTimeline.length - 1]);
        const timeline: Date[] = [];
        for (let i = 0; i < PREDICTION_DEPTH; i++) {
            timeline.push(
                moment(end)
                    .add(i + 1, "months")
                    .toDate()
            ); // todo: unified date format
        }
        return timeline;
    }

    private get predictionSavingsLineValues() {
        const realSavingsLineValues = (this.realSavingsLineValues ?? []).slice(
            0,
            -1
        );

        const result: number[] = [];

        for (
            let i = 0;
            i < realSavingsLineValues.length - REGRESSION_SAMPLE_POINTS;
            i++
        ) {
            const regression = makeAutoRegression(
                realSavingsLineValues.slice(i, i + REGRESSION_SAMPLE_POINTS)
            );
            result.push(regression(0));
        }

        const regression = makeAutoRegression(
            realSavingsLineValues.slice(-REGRESSION_SAMPLE_POINTS)
        );

        const timelineLength = this.predictionTimeline.length + 1; // also include current month
        for (let i = 0; i < timelineLength; i++) {
            result.push(regression(i));
        }
        return result;
    }

    private get chartData() {
        const realTimeline = this.realTimeline;
        if (realTimeline == null) {
            return null;
        }

        const timeline = [...realTimeline, ...this.predictionTimeline];

        const realSavingsLineValues = this.realSavingsLineValues;
        if (realSavingsLineValues == null) {
            return null;
        }

        const predictionSavingsLineValues = [
            ...this.predictionSavingsLineValues,
        ];
        while (predictionSavingsLineValues.length < timeline.length) {
            // @ts-ignore
            predictionSavingsLineValues.splice(0, 0, undefined); // todo: allow undefined
        }

        return {
            xs: timeline,
            lines: [
                {
                    name: "Real",
                    values: realSavingsLineValues,
                },
                {
                    name: "Prediction",
                    values: predictionSavingsLineValues,
                },
            ],
        } as ChartData<Date>;
    }

    private get chartValueFormatter() {
        return (value: number) =>
            `${value.toFixed(0)} ${this.chartDataCurrency}`;
    }

    @Watch("realDataParameters", { immediate: true })
    private async onRealDataParametersChanged(
        parameters: RealDataParameters | null
    ) {
        if (parameters == null) {
            return;
        }

        this.rawSavingsData = await this.api.getSavingsSeries(
            parameters.deviceId,
            parameters.currency,
            parameters.periodStart.toDate(),
            parameters.periodEnd.toDate()
        );
        this.chartDataCurrency = parameters.currency;
    }
}
</script>

<style lang="scss">
.finance-prophet-chart {
    height: 24em;
}
</style>
