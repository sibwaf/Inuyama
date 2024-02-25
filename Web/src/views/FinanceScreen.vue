<template>
    <div>
        <div class="section has-text-centered" v-if="deviceId == null || currency == null">
            <h1 class="title" v-if="deviceId == null">No device selected</h1>
            <h1 class="title" v-else-if="currency == null">No currency selected</h1>
        </div>
        <template v-else>
            <finance-dashboard-month-group :deviceId="deviceId" :currency="currency" />
            <hr>
            <finance-dashboard-history-group :deviceId="deviceId" :currency="currency" />
        </template>
    </div>
</template>

<script lang="ts">
import { Component, Inject, Vue } from "vue-property-decorator";

import FinanceDashboardMonthGroup from "@/components/finance/FinanceDashboardMonthGroup.vue";
import FinanceDashboardHistoryGroup from "@/components/finance/FinanceDashboardHistoryGroup.vue";

import Storage from "@/storage/Storage";

@Component({
    components: {
        FinanceDashboardMonthGroup,
        FinanceDashboardHistoryGroup,
    }
})
export default class FinanceScreen extends Vue {
    @Inject()
    private storage!: Storage;

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
