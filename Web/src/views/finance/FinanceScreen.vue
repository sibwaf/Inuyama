<template>
    <div>
        <div class="section has-text-centered" v-if="deviceId == null || currency == null">
            <h1 class="title" v-if="deviceId == null">No device selected</h1>
            <h1 class="title" v-else-if="currency == null">No currency selected</h1>
        </div>
        <template v-else>
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
            <hr>
            <div class="columns">
                <finance-dashboard-savings-panel class="column is-4" :month="rawSelectedMonth" :deviceId="deviceId"
                    :currency="currency" />
                <finance-dashboard-income-panel class="column is-4" :month="rawSelectedMonth" :deviceId="deviceId"
                    :currency="currency" />
                <finance-dashboard-expense-panel class="column is-4" :month="rawSelectedMonth" :deviceId="deviceId"
                    :currency="currency" />
            </div>
            <hr>
            <div class="columns">
                <finance-dashboard-savings-history-panel class="column is-6" :deviceId="deviceId" :currency="currency" />
                <finance-dashboard-dynamics-history-panel class="column is-6" :deviceId="deviceId" :currency="currency" />
            </div>
        </template>
    </div>
</template>

<script lang="ts">
import { Component, Inject, Vue } from "vue-property-decorator";
import moment from "moment";

import FinanceDashboardSavingsPanel from "@/components/finance/FinanceDashboardSavingsPanel.vue";
import FinanceDashboardIncomePanel from "@/components/finance/FinanceDashboardIncomePanel.vue";
import FinanceDashboardExpensePanel from "@/components/finance/FinanceDashboardExpensePanel.vue";
import FinanceDashboardSavingsHistoryPanel from "@/components/finance/FinanceDashboardSavingsHistoryPanel.vue";
import FinanceDashboardDynamicsHistoryPanel from "@/components/finance/FinanceDashboardDynamicsHistoryPanel.vue";

import Storage from "@/storage/Storage";

@Component({
    components: {
        FinanceDashboardSavingsPanel,
        FinanceDashboardIncomePanel,
        FinanceDashboardExpensePanel,
        FinanceDashboardSavingsHistoryPanel,
        FinanceDashboardDynamicsHistoryPanel,
    }
})
export default class FinanceScreen extends Vue {
    @Inject()
    private storage!: Storage;

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

    public get deviceId() {
        return this.storage.devices.selectedDevice;
    }

    public get currency() {
        const deviceId = this.deviceId;
        if (deviceId == null) {
            return null;
        }

        const financeStorage = this.storage.ofDevice(deviceId).finance;
        return financeStorage.selectedCurrency;
    }
}
</script>
