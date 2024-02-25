<template>
    <div>
        <div class="field is-grouped">
            <p class="control">
                <button class="button" @click="toPreviousMonth">Previous</button>
            </p>
            <p class="control">
                <el-date-picker type="month" :clearable="false" v-model="rawSelectedMonth" />
            </p>
            <p class="control">
                <button class="button" @click="toCurrentMonth" :disabled="!canSelectCurrentMonth">Today</button>
            </p>
            <p class="control">
                <button class="button" @click="toNextMonth" :disabled="!canSelectNextMonth">Next</button>
            </p>
        </div>
        <br>
        <div class="columns">
            <finance-dashboard-savings-panel class="column is-4" :month="rawSelectedMonth" :deviceId="deviceId"
                :currency="currency" />
            <finance-dashboard-income-panel class="column is-4" :month="rawSelectedMonth" :deviceId="deviceId"
                :currency="currency" />
            <finance-dashboard-expense-panel class="column is-4" :month="rawSelectedMonth" :deviceId="deviceId"
                :currency="currency" />
        </div>
    </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from "vue-property-decorator";
import moment from "moment";

import FinanceDashboardSavingsPanel from "@/components/finance/FinanceDashboardSavingsPanel.vue";
import FinanceDashboardIncomePanel from "@/components/finance/FinanceDashboardIncomePanel.vue";
import FinanceDashboardExpensePanel from "@/components/finance/FinanceDashboardExpensePanel.vue";

@Component({
    components: {
        FinanceDashboardSavingsPanel,
        FinanceDashboardIncomePanel,
        FinanceDashboardExpensePanel,
    }
})
export default class FinanceDashboardMonthGroup extends Vue {

    @Prop() public deviceId!: string;
    @Prop() public currency!: string;

    public rawSelectedMonth = moment();

    public toPreviousMonth() {
        this.rawSelectedMonth = moment(this.rawSelectedMonth).subtract(1, "month");
    }

    public toNextMonth() {
        this.rawSelectedMonth = moment(this.rawSelectedMonth).add(1, "month");
    }

    public toCurrentMonth() {
        this.rawSelectedMonth = moment();
    }

    public get canSelectCurrentMonth() {
        const selectedMonth = moment(this.rawSelectedMonth).startOf("month");
        const currentMonth = moment().startOf("month");
        return !selectedMonth.isSame(currentMonth);
    }

    public get canSelectNextMonth() {
        const selectedMonth = moment(this.rawSelectedMonth).startOf("month");
        const currentMonth = moment().startOf("month");
        return selectedMonth.isBefore(currentMonth);
    }
}
</script>
