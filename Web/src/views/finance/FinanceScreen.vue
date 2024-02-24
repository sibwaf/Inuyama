<template>
    <div>
        <div class="section has-text-centered" v-if="deviceId == null || currency == null">
            <h1 class="title" v-if="deviceId == null">No device selected</h1>
            <h1 class="title" v-else-if="currency == null">No currency selected</h1>
        </div>
        <template v-else>
            <el-date-picker type="month" :clearable="false" v-model="rawSelectedMonth" />
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
            <finance-dashboard-savings-history-panel :deviceId="deviceId" :currency="currency" />
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

import Storage from "@/storage/Storage";

@Component({ components: { FinanceDashboardSavingsPanel, FinanceDashboardIncomePanel, FinanceDashboardExpensePanel, FinanceDashboardSavingsHistoryPanel } })
export default class FinanceScreen extends Vue {
    @Inject()
    private storage!: Storage;

    rawSelectedMonth = moment();

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
