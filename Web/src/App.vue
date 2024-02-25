<template>
    <div class="app-container">
        <nav class="navbar is-fixed-top">
            <div class="container">
                <div class="navbar-brand">
                    <a role="button" class="navbar-burger" :class="{ 'is-active': menuExpanded }"
                        @click="menuExpanded = !menuExpanded">
                        <span></span>
                        <span></span>
                        <span></span>
                    </a>
                </div>
                <div class="navbar-menu" :class="{ 'is-active': menuExpanded }">
                    <div class="navbar-start">
                        <router-link to="/" class="navbar-item"
                            :class="{ 'is-tab': !menuExpanded, 'is-active': selectedScreen == 'FinanceDashboard' }">
                            Dashboard
                        </router-link>
                        <router-link to="/compare" class="navbar-item"
                            :class="{ 'is-tab': !menuExpanded, 'is-active': selectedScreen == 'FinanceComparison' }">
                            Compare
                        </router-link>
                    </div>
                    <div class="navbar-end">
                        <div class="navbar-item">
                            <div class="field is-grouped">
                                <p class="control">
                                    <device-selector />
                                </p>
                                <p class="control">
                                    <currency-selector />
                                </p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </nav>
        <router-view />
    </div>
</template>

<script lang="ts">
import { Component, Provide, Vue, Watch } from "vue-property-decorator";

import Storage from "@/storage/Storage";

import DeviceSelector from "@/components/DeviceSelector.vue";
import CurrencySelector from "@/components/CurrencySelector.vue";

@Component({
    components: { DeviceSelector, CurrencySelector },
})
export default class App extends Vue {
    @Provide()
    private storage = Vue.observable(new Storage());

    public menuExpanded = false;

    async created() {
        try {
            await this.storage.devices.refreshAvailableDevices();

            const available = this.storage.devices.availableDevices;
            const selected = this.storage.devices.selectedDevice;

            if (available.length == 1) {
                this.storage.devices.selectedDevice = available[0];
            } else if (selected != null && !available.includes(selected)) {
                this.storage.devices.selectedDevice = null;
            }
        } catch {
            console.error("Failed to retrieve device list");
        }
    }

    public get selectedScreen() {
        return this.$route.name;
    }

    @Watch("selectedScreen")
    public onSelectedScreenChanged() {
        this.menuExpanded = false;
    }
}
</script>

<style lang="scss" scoped>
@import "./style.scss";

.app-container {
    padding: $size-large;
}
</style>