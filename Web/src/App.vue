<template>
    <div class="app-container">
        <nav class="navbar is-fixed-top">
            <div class="container">
                <div class="navbar-menu">
                    <div class="navbar-start">
                        <router-link to="/" class="navbar-item">
                            Home
                        </router-link>
                    </div>
                    <div class="navbar-end">
                        <div class="navbar-item">
                            <device-selector />
                        </div>
                    </div>
                </div>
            </div>
        </nav>
        <div class="columns">
            <div class="column is-2 menu-wrapper">
                <aside class="menu">
                    <p class="menu-label">Menu</p>
                    <ul class="menu-list">
                        <li>
                            <span>Finance</span>
                            <ul>
                                <li><router-link to="/finance/compare">Compare</router-link></li>
                            </ul>
                        </li>
                    </ul>
                </aside>
            </div>
            <div class="column is-10">
                <router-view />
            </div>
        </div>
    </div>
</template>

<script lang="ts">
import { Component, Provide, Vue } from "vue-property-decorator";

import Storage from "@/storage/Storage";

import DeviceSelector from "@/components/DeviceSelector.vue";

@Component({
    components: { DeviceSelector },
})
export default class App extends Vue {
    @Provide()
    private storage = Vue.observable(new Storage());

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
}
</script>

<style lang="scss" scoped>
@import "./style.scss";

.app-container {
    padding: $size-large;
}
</style>