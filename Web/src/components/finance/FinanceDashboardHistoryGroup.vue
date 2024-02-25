<template>
    <div>
        <div class="field is-grouped">
            <p class="control" v-for="option in periodOptions">
                <button class="button" :class="{ 'is-success': historyDepth == option.months }"
                    :disabled="historyDepth == option.months" @click="historyDepth = option.months">
                    {{ option.label }}
                </button>
            </p>
        </div>
        <br>
        <div class="columns">
            <finance-dashboard-savings-history-panel class="column is-6" :deviceId="deviceId" :currency="currency"
                :periodStart="periodStart" :periodEnd="periodEnd" />
            <finance-dashboard-dynamics-history-panel class="column is-6" :deviceId="deviceId" :currency="currency"
                :periodStart="periodStart" :periodEnd="periodEnd" />
        </div>
    </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from "vue-property-decorator";
import moment from "moment";

import FinanceDashboardSavingsHistoryPanel from "@/components/finance/FinanceDashboardSavingsHistoryPanel.vue";
import FinanceDashboardDynamicsHistoryPanel from "@/components/finance/FinanceDashboardDynamicsHistoryPanel.vue";

interface PeriodOption {
    readonly label: string;
    readonly months: number;
}

@Component({
    components: {
        FinanceDashboardSavingsHistoryPanel,
        FinanceDashboardDynamicsHistoryPanel,
    }
})
export default class FinanceDashboardHistoryGroup extends Vue {

    @Prop() public deviceId!: string;
    @Prop() public currency!: string;

    public historyDepth = 12;

    public get periodOptions() {
        return [
            { label: "3 years", months: 36 },
            { label: "1 year", months: 12 },
        ] as PeriodOption[];
    }

    public get periodStart() {
        return moment().subtract(this.historyDepth - 1, "months").startOf("month");
    }

    public get periodEnd() {
        return moment().endOf("month");
    }
}
</script>

<style lang="scss">
.history-panel-content {
    height: 20em;

    &>* {
        height: 100%;
    }
}
</style>
