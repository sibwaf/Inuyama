<template>
    <div>
        <h2 class="title has-text-centered">Savings</h2>
        <div class="columns">
            <div class="column is-6">
                <doughnut-chart :data="latestSavingsByCurrency" :valueFormatter="formatValue" />
            </div>

            <div class="column is-6" v-if="state == STATE_LOADING">
                Loading...
            </div>
            <div class="column is-6" v-else-if="state == STATE_ERROR">
                Failed to load data
            </div>
            <div class="column is-6" v-else>
                <h3 class="subtitle">{{ formatValue(latestSavings) }}</h3>

                <hr v-if="latestChange != null || previousChange != null">
                <div v-if="latestChange != null">
                    This month:
                    <span :class="latestChange >= 0 ? 'has-text-success' : 'has-text-danger'">
                        {{ formatDiff(latestChange) }}
                    </span>
                </div>
                <div v-if="previousChange != null">
                    Typical before:
                    <span :class="previousChange >= 0 ? 'has-text-success' : 'has-text-danger'">
                        {{ formatDiff(previousChange) }}
                    </span>
                </div>
            </div>
        </div>
    </div>
</template>

<script lang="ts">
import { Component, Prop, Vue, Watch } from "vue-property-decorator";
import moment, { Moment } from "moment";

import DoughnutChart from "@/components/charts/DoughnutChart.vue";

import {
    FinanceAnalyticSeriesDto,
    FinanceApi,
} from "@/api/FinanceApi";

import Collections from "@/utility/Collections";
import { derivative, median, sum } from "@/utility/Magic";

interface Parameters {
    readonly month: Moment;
    readonly deviceId: string;
    readonly currency: string;
}

const HISTORY_LOOKBEHIND_MONTHS = 4;

@Component({ components: { DoughnutChart } })
export default class FinanceDashboardSavingsPanel extends Vue {

    @Prop() private readonly month!: Moment;
    @Prop() private readonly deviceId!: string;
    @Prop() private readonly currency!: string;

    public readonly STATE_OK = "OK";
    public readonly STATE_LOADING = "LOADING";
    public readonly STATE_ERROR = "ERROR";

    private readonly api = new FinanceApi();

    public state = this.STATE_LOADING;
    private loadedCurrency = this.currency;

    private savingsByCurrencyTimeline: FinanceAnalyticSeriesDto = {
        timeline: [],
        data: new Map(),
    };

    public get latestSavingsByCurrency() {
        const data: [string, number][] = [...this.savingsByCurrencyTimeline.data]
            .map(([currency, values]) => [currency, Collections.last(values) ?? 0]);

        return data.sort((first, second) => second[1] - first[1]);
    }

    private get savingsTimeline() {
        const result: number[] = [];
        for (const [_, values] of this.savingsByCurrencyTimeline.data) {
            for (let i = 0; i < values.length; i++) {
                result[i] = (result[i] ?? 0) + values[i];
            }
        }
        return result;
    }

    private get savingsTimelineDiff() {
        return derivative(this.savingsTimeline);
    }

    public get previousChange() {
        return median(Collections.head(this.savingsTimelineDiff));
    }

    public get latestChange() {
        return Collections.last(this.savingsTimelineDiff);
    }

    public get latestSavings() {
        return sum(this.latestSavingsByCurrency.map(([_, amount]) => amount));
    }

    public formatValue(value: number): string {
        return `${value.toFixed(0)} ${this.loadedCurrency}`;
    }

    public formatDiff(value: number): string {
        return `${value >= 0 ? "+" : ""}${value.toFixed(0)} ${this.loadedCurrency}`;
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

            this.savingsByCurrencyTimeline = await this.api.getSavingsSeries(
                parameters.deviceId,
                parameters.currency,
                moment(this.month).subtract(HISTORY_LOOKBEHIND_MONTHS, "months").startOf("month").toDate(),
                moment(this.month).endOf("month").toDate(),
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
