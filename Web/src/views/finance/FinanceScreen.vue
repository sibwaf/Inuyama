<template>
    <div>
        <div class="secton">
            <el-date-picker
                type="month"
                :clearable="false"
                v-model="rawSelectedMonth"
            />
        </div>
        <div class="section">
            <div class="columns is-centered">
                <div class="column is-3">
                    <h2 class="subtitle has-text-centered">Totals</h2>
                    <doughnut-chart :data="byDirectionData" />
                </div>
                <div class="column is-3">
                    <h2 class="subtitle has-text-centered">Income</h2>
                    <doughnut-chart :data="incomeByCategoryData" />
                </div>
                <div class="column is-3">
                    <h2 class="subtitle has-text-centered">Expenses</h2>
                    <doughnut-chart :data="expenseByCategoryData" />
                </div>
            </div>
        </div>
    </div>
</template>

<script lang="ts">
import { Component, Inject, Vue, Watch } from "vue-property-decorator";
import moment, { Moment } from "moment";

import DoughnutChart from "@/components/charts/DoughnutChart.vue";

import Storage from "@/storage/Storage";
import {
    FinanceOperationDirection,
    FinanceAnalyticGrouping,
    FinanceApi,
} from "@/api/FinanceApi";

interface ByDirectionParameters {
    readonly deviceId: string;
    readonly month: Moment;
}

interface ByCategoryParameters {
    readonly deviceId: string;
    readonly month: Moment;
}

@Component({ components: { DoughnutChart } })
export default class FinanceScreen extends Vue {
    @Inject()
    private storage!: Storage;

    private api = new FinanceApi();

    private rawSelectedMonth = moment();

    private rawByDirectionData: [string, number][] = [];
    private rawIncomeByCategoryData: [string, number][] = [];
    private rawExpenseByCategoryData: [string, number][] = [];

    get selectedMonth() {
        return moment(this.rawSelectedMonth).startOf("month");
    }

    get categories() {
        return this.storage.ofCurrentDevice()?.finance?.categories || [];
    }

    get byDirectionParameters() {
        const deviceId = this.storage.devices.selectedDevice;
        if (deviceId == null) {
            return null;
        }

        return { deviceId, month: this.selectedMonth } as ByDirectionParameters;
    }

    get byDirectionData() {
        return this.rawByDirectionData;
    }

    get byCategoryParameters() {
        const deviceId = this.storage.devices.selectedDevice;
        if (deviceId == null) {
            return null;
        }

        return { deviceId, month: this.selectedMonth } as ByCategoryParameters;
    }

    get incomeByCategoryData() {
        return this.rawIncomeByCategoryData.map(([categoryId, value]) => {
            const category = this.categories.find((it) => it.id == categoryId);
            return [category?.name ?? categoryId, value];
        });
    }

    get expenseByCategoryData() {
        return this.rawExpenseByCategoryData.map(([categoryId, value]) => {
            const category = this.categories.find((it) => it.id == categoryId);
            return [category?.name ?? categoryId, value];
        });
    }

    @Watch("byDirectionParameters", { immediate: true })
    async onByDirectionParametersChanged(
        byDirectionParameters: ByDirectionParameters | null
    ) {
        if (byDirectionParameters == null) {
            return;
        }

        const data = await this.api.getSummary(
            byDirectionParameters.deviceId,
            FinanceAnalyticGrouping.DIRECTION,
            {
                start: byDirectionParameters.month.toDate(),
                end: moment(byDirectionParameters.month)
                    .add(1, "month")
                    .toDate(),
                direction: null,
            }
        );

        this.rawByDirectionData = [...data].map(([direction, amount]) => [
            direction,
            Math.abs(amount),
        ]);
    }

    @Watch("byCategoryParameters", { immediate: true })
    async onByCategoryParametersChanged(
        byCategoryParameters: ByCategoryParameters | null
    ) {
        if (byCategoryParameters == null) {
            return;
        }

        const fetch = async (direction: FinanceOperationDirection) => {
            const data = await this.api.getSummary(
                byCategoryParameters.deviceId,
                FinanceAnalyticGrouping.CATEGORY,
                {
                    start: byCategoryParameters.month.toDate(),
                    end: moment(byCategoryParameters.month)
                        .add(1, "month")
                        .toDate(),
                    direction,
                }
            );

            const result: [string, number][] = [...data].map(
                ([categoryId, amount]) => [categoryId, Math.abs(amount)]
            );

            result.sort((first, second) => second[1] - first[1]);
            return result;
        };

        const incomeDataAsync = fetch(FinanceOperationDirection.INCOME);
        const expenseDataAsync = fetch(FinanceOperationDirection.EXPENSE);

        this.rawIncomeByCategoryData = await incomeDataAsync;
        this.rawExpenseByCategoryData = await expenseDataAsync;
    }
}
</script>
