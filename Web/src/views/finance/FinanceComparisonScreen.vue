<template>
    <div>
        <div class="columns">
            <div class="column is-2">
                <div>
                    <el-select class="is-fullwidth" v-model="granularity">
                        <el-option
                            v-for="granularity in granularityOptions"
                            :key="granularity.code"
                            :value="granularity.code"
                            :label="granularity.name"
                        />
                    </el-select>
                </div>
                <div>
                    <el-date-picker
                        class="is-fullwidth"
                        type="month"
                        :clearable="false"
                        v-model="firstMonth"
                    />
                </div>
                <div>
                    <el-date-picker
                        class="is-fullwidth"
                        type="month"
                        :clearable="false"
                        v-model="secondMonth"
                    />
                </div>
            </div>
            <div class="column is-10">
                <h-bar-chart :data="chartData" />
            </div>
        </div>
    </div>
</template>

<script lang="ts">
import { Component, Inject, Vue, Watch } from "vue-property-decorator";
import moment, { Moment } from "moment";

import {
    FinanceAnalyticGrouping,
    FinanceApi,
    FinanceCategory,
    FinanceOperationDirection,
} from "@/api/FinanceApi";
import Storage from "@/storage/Storage";
import HBarChart from "@/components/charts/HBarChart.vue";

enum Granularity {
    MONTHLY,
    QUATERLY,
    HALF_ANNUAL,
    ANNUAL,
}

interface ChartParameters {
    readonly deviceId: string;

    readonly firstMonth: Moment;
    readonly secondMonth: Moment;
    readonly granularityMonths: number;

    readonly grouping: FinanceAnalyticGrouping;
    readonly direction: FinanceOperationDirection | null;
}

@Component({ components: { HBarChart } })
export default class FinanceComparisonScreen extends Vue {
    @Inject()
    private storage!: Storage;

    private categories: FinanceCategory[] = [];

    private granularity: Granularity = Granularity.MONTHLY;
    private firstMonth: Moment = moment();
    private secondMonth: Moment = moment();

    private direction: FinanceOperationDirection | null =
        FinanceOperationDirection.EXPENSE;

    private chartData: [string, number][] = [];

    created() {
        this.secondMonth = moment().startOf("month");

        this.$nextTick(async () => {
            const deviceId = this.storage.devices.selectedDevice;
            if (deviceId != null) {
                this.categories = await this.getCategories(deviceId);

                this.$watch(
                    "chartParameters",
                    async (chartParameters: ChartParameters) => {
                        if (chartParameters != null) {
                            this.chartData = await this.getChartData(
                                chartParameters
                            );
                        }
                    },
                    {
                        immediate: true,
                    }
                );
            }
        });
    }

    get granularityOptions() {
        return [
            { code: Granularity.MONTHLY, name: "Monthly" },
            { code: Granularity.QUATERLY, name: "Quarterly" },
            { code: Granularity.HALF_ANNUAL, name: "Half-annual" },
            { code: Granularity.ANNUAL, name: "Annual" },
        ];
    }

    get granularityMonths() {
        switch (this.granularity) {
            case Granularity.MONTHLY:
                return 1;
            case Granularity.QUATERLY:
                return 3;
            case Granularity.HALF_ANNUAL:
                return 6;
            case Granularity.ANNUAL:
                return 12;
            default:
                throw Error(`Unknown granularity ${this.granularity}`);
        }
    }

    getCategories(deviceId: string): Promise<FinanceCategory[]> {
        return new FinanceApi().getCategories(deviceId);
    }

    async getChartData(
        parameters: ChartParameters
    ): Promise<[string, number][]> {
        const getDataForPeriod = async (start: Moment) => {
            return new FinanceApi().getSummary(
                parameters.deviceId,
                parameters.grouping,
                {
                    start: start.toDate(),
                    end: start
                        .add(parameters.granularityMonths, "months")
                        .toDate(),
                    direction: parameters.direction,
                }
            );
        };

        const getCategoryById = (id: string) =>
            this.categories.find((it) => it.id == id);

        const firstData = getDataForPeriod(moment(parameters.firstMonth));
        const secondData = getDataForPeriod(moment(parameters.secondMonth));

        const allCategories = new Set([
            ...(await firstData).keys(),
            ...(await secondData).keys(),
        ]);

        const result: [string, number][] = [];
        for (const categoryId of allCategories) {
            const inFirst = (await firstData).get(categoryId) ?? 0;
            const inSecond = (await secondData).get(categoryId) ?? 0;

            result.push([
                getCategoryById(categoryId)?.name ?? categoryId,
                Math.abs(inSecond) - Math.abs(inFirst),
            ]);
        }

        result.sort((first, second) => first[1] - second[1]);
        return result;
    }

    private get chartParameters() {
        const deviceId = this.storage.devices.selectedDevice;
        if (deviceId == null) {
            return null;
        }

        return {
            deviceId,

            firstMonth: moment(this.firstMonth).startOf("month"),
            secondMonth: moment(this.secondMonth).startOf("month"),
            granularityMonths: this.granularityMonths,

            grouping: FinanceAnalyticGrouping.CATEGORY,
            direction: this.direction,
        } as ChartParameters;
    }

    @Watch("granularityMonths")
    private onGranularityMonthsChanged(granularityMonths: number) {
        this.secondMonth = moment().subtract(granularityMonths - 1, "months");
        this.firstMonth = moment(this.secondMonth).subtract(
            granularityMonths,
            "months"
        );
    }

    @Watch("firstMonth")
    private onFirstMonthChanged(firstMonth: any) {
        if (moment(firstMonth) >= moment(this.secondMonth)) {
            this.secondMonth = moment(firstMonth)
                .startOf("month")
                .add(this.granularityMonths, "months");
        }
    }

    @Watch("secondMonth")
    private onSecondMonthChanged(secondMonth: any) {
        if (moment(secondMonth) <= moment(this.firstMonth)) {
            this.firstMonth = moment(secondMonth)
                .startOf("month")
                .subtract(this.granularityMonths, "months");
        }
    }
}
</script>

<style>
</style>
