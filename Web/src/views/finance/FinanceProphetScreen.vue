<template>
    <div>
        <div class="section">
            <line-chart
                class="finance-prophet-chart"
                v-if="chartData"
                :data="chartData"
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

    private rawDynamicData: FinanceAnalyticSeriesDto | null = null;

    private get accounts() {
        return this.storage.ofCurrentDevice()?.finance?.accounts ?? [];
    }

    private get periodStart() {
        return moment(this.rawPeriodStart).startOf("month");
    }

    private get periodEnd() {
        return moment(this.rawPeriodEnd).endOf("month");
    }

    private get currentTotalBalance() {
        return this.accounts.reduce((acc, it) => acc + it.balance, 0.0);
    }

    private get realDataParameters(): RealDataParameters | null {
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

    private get realTimeline() {
        return this.rawDynamicData?.timeline;
    }

    private get realDynamicValues() {
        const rawDynamicData = this.rawDynamicData;
        if (rawDynamicData == null) {
            return null;
        }

        const key = [...rawDynamicData.data.keys()][0];
        if (key == null) {
            return null;
        }

        return rawDynamicData.data.get(key);
    }

    private get realSavingsLineValues() {
        const realDynamicValues = this.realDynamicValues;
        if (realDynamicValues == null || realDynamicValues.length == 0) {
            return null;
        }

        let currentTotalBalance = this.currentTotalBalance;

        const line = [currentTotalBalance];
        for (let i = realDynamicValues.length - 1; i > 0; i--) {
            currentTotalBalance -= realDynamicValues[i];
            line.splice(0, 0, currentTotalBalance);
        }
        return line;
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

    @Watch("realDataParameters", { immediate: true })
    private async onRealDataParametersChanged(
        parameters: RealDataParameters | null
    ) {
        if (parameters == null) {
            return;
        }

        this.rawDynamicData = await this.api.getSeries(
            parameters.deviceId,
            null,
            {
                start: parameters.periodStart.toDate(),
                end: parameters.periodEnd.toDate(),
                direction: null,
            }
        );
    }
}
</script>

<style lang="scss">
.finance-prophet-chart {
    height: 24em;
}
</style>
