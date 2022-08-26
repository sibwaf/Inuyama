<template>
    <div class="select" v-if="financeStorage">
        <select v-model="financeStorage.selectedCurrency">
            <option v-for="name of availableCurrencies" :key="name" :value="name">
                {{ name }}
            </option>
            <option :value="null" disabled>No currency</option>
        </select>
    </div>
</template>

<script lang="ts">
import { Component, Inject, Vue } from "vue-property-decorator";

import Storage from "@/storage/Storage";

@Component
export default class CurrencySelector extends Vue {
    @Inject()
    private readonly storage!: Storage;

    get financeStorage() {
        return this.storage.ofCurrentDevice()?.finance;
    }

    get availableCurrencies() {
        const accounts = this.financeStorage?.accounts ?? [];
        return new Set(accounts.map(it => it.currency));
    }
}
</script>
