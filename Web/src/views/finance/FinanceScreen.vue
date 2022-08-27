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
                    <doughnut-chart
                        v-if="byDirectionData"
                        :data="byDirectionData"
                    />
                    <no-data-view v-else />
                </div>
                <div class="column is-3">
                    <h2 class="subtitle has-text-centered">Income</h2>
                    <doughnut-chart
                        v-if="incomeByCategoryData"
                        :data="incomeByCategoryData"
                    />
                    <no-data-view v-else />
                </div>
                <div class="column is-3">
                    <h2 class="subtitle has-text-centered">Expenses</h2>
                    <doughnut-chart
                        v-if="expenseByCategoryData"
                        :data="expenseByCategoryData"
                    />
                    <no-data-view v-else />
                </div>
            </div>
        </div>
    </div>
</template>

<script lang="ts">
import { Component, Inject, Vue, Watch } from "vue-property-decorator";
import moment, { Moment } from "moment";

import DoughnutChart from "@/components/charts/DoughnutChart.vue";
import NoDataView from "@/components/NoDataView.vue";

import Storage from "@/storage/Storage";
import {
    FinanceOperationDirection,
    FinanceAnalyticGrouping,
    FinanceApi,
} from "@/api/FinanceApi";

interface ByDirectionParameters {
    readonly deviceId: string;
    readonly currency: string;
    readonly month: Moment;
}

interface ByCategoryParameters {
    readonly deviceId: string;
    readonly currency: string;
    readonly month: Moment;
}

@Component({ components: { DoughnutChart, NoDataView } })
export default class FinanceScreen extends Vue {
    @Inject()
    private storage!: Storage;

    private api = new FinanceApi();

    private rawSelectedMonth = moment();

    private rawByDirectionData: [string, number][] | null = null;
    private rawIncomeByCategoryData: [string, number][] | null = null;
    private rawExpenseByCategoryData: [string, number][] | null = null;

    get selectedMonth() {
        return moment(this.rawSelectedMonth).startOf("month");
    }

    get deviceId() {
        return this.storage.devices.selectedDevice;
    }

    get financeStorage() {
        const deviceId = this.deviceId;
        if (deviceId == null) {
            return null;
        }

        return this.storage.ofDevice(deviceId).finance;
    }

    get currency() {
        return this.financeStorage?.selectedCurrency;
    }

    get categories() {
        return this.financeStorage?.categories || [];
    }

    get byDirectionParameters() {
        const deviceId = this.deviceId;
        if (deviceId == null) {
            return null;
        }

        const currency = this.currency;
        if (currency == null) {
            return null;
        }

        return {
            deviceId,
            currency,
            month: this.selectedMonth,
        } as ByDirectionParameters;
    }

    get byDirectionData() {
        const data = this.rawByDirectionData;
        return data != null && data.length > 0 ? data : null;
    }

    get byCategoryParameters() {
        const deviceId = this.deviceId;
        if (deviceId == null) {
            return null;
        }

        const currency = this.currency;
        if (currency == null) {
            return null;
        }

        return {
            deviceId,
            currency,
            month: this.selectedMonth,
        } as ByCategoryParameters;
    }

    get incomeByCategoryData() {
        const data = this.rawIncomeByCategoryData?.map(
            ([categoryId, value]) => {
                const category = this.categories.find(
                    (it) => it.id == categoryId
                );
                return [category?.name ?? categoryId, value];
            }
        );
        return data != null && data.length > 0 ? data : null;
    }

    get expenseByCategoryData() {
        const data = this.rawExpenseByCategoryData?.map(
            ([categoryId, value]) => {
                const category = this.categories.find(
                    (it) => it.id == categoryId
                );
                return [category?.name ?? categoryId, value];
            }
        );
        return data != null && data.length > 0 ? data : null;
    }

    @Watch("byDirectionParameters", { immediate: true })
    async onByDirectionParametersChanged(
        byDirectionParameters: ByDirectionParameters | null
    ) {
        if (byDirectionParameters == null) {
            return;
        }

        const data = await this.api.getOperationSummary(
            byDirectionParameters.deviceId,
            FinanceAnalyticGrouping.DIRECTION,
            {
                start: byDirectionParameters.month.toDate(),
                end: moment(byDirectionParameters.month)
                    .add(1, "month")
                    .toDate(),
                direction: null,
            },
            byDirectionParameters.currency
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
            const data = await this.api.getOperationSummary(
                byCategoryParameters.deviceId,
                FinanceAnalyticGrouping.CATEGORY,
                {
                    start: byCategoryParameters.month.toDate(),
                    end: moment(byCategoryParameters.month)
                        .add(1, "month")
                        .toDate(),
                    direction,
                },
                byCategoryParameters.currency
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
