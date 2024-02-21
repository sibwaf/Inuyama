<template>
    <div>
        <h2 class="title has-text-centered">Expense</h2>
        <div class="columns">
            <div class="column is-6">
                <doughnut-chart :data="latestExpenseByCategory" :labelFormatter="formatLabel"
                    :valueFormatter="formatValue" />
            </div>

            <div class="column is-6" v-if="state == STATE_LOADING">
                Loading...
            </div>
            <div class="column is-6" v-else-if="state == STATE_ERROR">
                Failed to load data
            </div>
            <div class="column is-6" v-else>
                <h3 class="subtitle">{{ formatValue(latestExpense) }}</h3>

                <hr v-if="expenseDiff.length > 0">
                <div v-for="[categoryId, diff] in expenseDiff" :key="categoryId">
                    <div>
                        <span>{{ formatLabel(categoryId) }}:</span>
                        <span :class="diff >= 0 ? 'has-text-success' : 'has-text-danger'">
                            {{ formatDiff(diff) }} {{ diff >= 0 ? 'less' : 'more' }}
                        </span>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script lang="ts">
import { Component, Inject, Prop, Vue, Watch } from "vue-property-decorator";
import moment, { Moment } from "moment";

import DoughnutChart from "@/components/charts/DoughnutChart.vue";

import Storage from "@/storage/Storage";
import {
    FinanceAnalyticGrouping,
    FinanceAnalyticFilter,
    FinanceAnalyticSeriesDto,
    FinanceApi,
    FinanceOperationDirection,
} from "@/api/FinanceApi";

import Collections from "@/utility/Collections";
import { median, sum } from "@/utility/Magic";

interface Parameters {
    readonly month: Moment;
    readonly deviceId: string;
    readonly currency: string;
}

const HISTORY_LOOKBEHIND_MONTHS = 4;
const TOP_OPERATION_COUNT = 3;

@Component({ components: { DoughnutChart } })
export default class FinanceDashboardExpensePanel extends Vue {

    @Inject()
    private storage!: Storage;

    @Prop() private readonly month!: Moment;
    @Prop() private readonly deviceId!: string;
    @Prop() private readonly currency!: string;

    public readonly STATE_OK = "OK";
    public readonly STATE_LOADING = "LOADING";
    public readonly STATE_ERROR = "ERROR";

    private readonly api = new FinanceApi();

    public state = this.STATE_LOADING;
    private loadedCurrency = this.currency;

    private expenseByCategoryTimeline: FinanceAnalyticSeriesDto = {
        timeline: [],
        data: new Map(),
    };

    private get categories() {
        return this.storage.ofDevice(this.deviceId).finance.categories;
    }

    private get previousExpenseByCategory() {
        const data: [string, number][] = [...this.expenseByCategoryTimeline.data]
            .map(([categoryId, values]) => [categoryId, median(Collections.head(values)) ?? 0]);

        return data;
    }

    public get latestExpenseByCategory() {
        const data: [string, number][] = [...this.expenseByCategoryTimeline.data]
            .map(([categoryId, values]) => [categoryId, Collections.last(values) ?? 0]);

        return data.sort((first, second) => first[1] - second[1]);
    }

    public get expenseDiff() {
        const diff = new Map<string, number>();
        for (const [categoryId, amount] of this.latestExpenseByCategory) {
            diff.set(categoryId, (diff.get(categoryId) ?? 0) + amount);
        }
        for (const [categoryId, amount] of this.previousExpenseByCategory) {
            diff.set(categoryId, (diff.get(categoryId) ?? 0) - amount);
        }

        const sorted = [...diff].sort((first, second) => first[1] - second[1]);
        const more = Collections.takeFirst(sorted, TOP_OPERATION_COUNT).filter(it => it[1] < 0);
        const less = Collections.takeLast(sorted, TOP_OPERATION_COUNT).filter(it => it[1] > 0);
        return [...more, ...less];
    }

    public get latestExpense() {
        return sum(this.latestExpenseByCategory.map(([_, amount]) => amount));
    }

    public formatLabel(label: string): string {
        const category = this.categories.find((it) => it.id == label);
        return category?.name ?? label;
    }

    public formatValue(value: number): string {
        return `${Math.abs(value).toFixed(0)} ${this.loadedCurrency}`;
    }

    public formatDiff(value: number): string {
        return `${Math.abs(value).toFixed(0)} ${this.loadedCurrency}`;
    }

    private get parameters() {
        return {
            month: this.month,
            deviceId: this.deviceId,
            currency: this.currency
        } as Parameters;
    }

    @Watch("parameters", { immediate: true })
    private async onParametersChanged(parameters: Parameters) {
        try {
            this.state = this.STATE_LOADING;

            this.expenseByCategoryTimeline = await this.api.getOperationSeries(
                parameters.deviceId,
                FinanceAnalyticGrouping.CATEGORY,
                {
                    direction: FinanceOperationDirection.EXPENSE,
                    start: moment(this.month).subtract(HISTORY_LOOKBEHIND_MONTHS, "months").startOf("month").toDate(),
                    end: moment(this.month).endOf("month").toDate(),
                } as FinanceAnalyticFilter,
                parameters.currency,
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
