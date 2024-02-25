<template>
    <div>
        <h2 class="title has-text-centered">Dynamics history</h2>
        <div class="has-text-centered" v-if="state == STATE_LOADING">
            Loading...
        </div>
        <div class="has-text-centered" v-else-if="state == STATE_ERROR">
            Failed to load data
        </div>
        <line-chart v-else class="finance-dashboard-global-dynamics-chart" :data="chartData" :xFormatter="formatTimestamp"
            :valueFormatter="formatValue" />
    </div>
</template>

<script lang="ts">
import { Component, Inject, Prop, Vue, Watch } from "vue-property-decorator";
import moment, { Moment } from "moment";

import Storage from "@/storage/Storage";
import { FinanceAnalyticFilter, FinanceAnalyticGrouping, FinanceAnalyticSeriesDto, FinanceApi } from "@/api/FinanceApi";

import LineChart, { ChartData } from "@/components/charts/LineChart.vue";

interface Parameters {
    readonly deviceId: string;
    readonly currency: string;
    readonly periodStart: Moment;
    readonly periodEnd: Moment;
}

@Component({ components: { LineChart } })
export default class FinanceDashboardDynamicsHistoryPanel extends Vue {

    @Inject()
    private storage!: Storage;

    @Prop() private readonly deviceId!: string;
    @Prop() private readonly currency!: string;

    public readonly STATE_OK = "OK";
    public readonly STATE_LOADING = "LOADING";
    public readonly STATE_ERROR = "ERROR";

    private readonly api = new FinanceApi();

    public state = this.STATE_LOADING;

    private rawPeriodStart = moment().subtract(1, "years").add(1, "month");
    private rawPeriodEnd = moment();

    private operationsByDirectionTimeline: FinanceAnalyticSeriesDto = {
        timeline: [],
        data: new Map(),
    };

    private loadedCurrency = this.currency;

    private get categories() {
        return this.storage.ofDevice(this.deviceId).finance.categories;
    }

    private get expenseValues() {
        return this.operationsByDirectionTimeline.data.get("EXPENSE") ?? [];
    }

    private get incomeValues() {
        return this.operationsByDirectionTimeline.data.get("INCOME") ?? [];
    }

    private get changeValues() {
        const result: number[] = [];
        for (const [_, values] of this.operationsByDirectionTimeline.data) {
            for (let i = 0; i < values.length; i++) {
                result[i] = (result[i] ?? 0) + values[i];
            }
        }
        return result;
    }

    public get chartData() {
        return {
            xs: this.operationsByDirectionTimeline.timeline,
            lines: [
                {
                    name: "Expenses",
                    values: this.expenseValues,
                },
                {
                    name: "Income",
                    values: this.incomeValues,
                },
                {
                    name: "Change",
                    values: this.changeValues,
                },
            ],
        } as ChartData<Date>;
    }

    public formatLabel(label: string): string {
        const category = this.categories.find((it) => it.id == label);
        return category?.name ?? label;
    }

    public formatValue(value: number): string {
        return `${value.toFixed(0)} ${this.loadedCurrency}`;
    }

    public formatTimestamp(timestamp: any): string {
        return moment(timestamp).format("MMMM YYYY");
    }

    private get parameters() {
        return {
            deviceId: this.deviceId,
            currency: this.currency,
            periodStart: moment(this.rawPeriodStart).startOf("month"),
            periodEnd: moment(this.rawPeriodEnd).endOf("month"),
        } as Parameters;
    }

    @Watch("parameters", { immediate: true })
    private async onParametersChanged(parameters: Parameters) {
        try {
            this.state = this.STATE_LOADING;

            this.operationsByDirectionTimeline = await this.api.getOperationSeries(
                parameters.deviceId,
                FinanceAnalyticGrouping.DIRECTION,
                {
                    start: parameters.periodStart.toDate(),
                    end: parameters.periodEnd.toDate(),
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

<style lang="scss">
.finance-dashboard-global-dynamics-chart {
    height: 24em;
}
</style>
