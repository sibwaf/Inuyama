<template>
    <div class="select">
        <select v-model="deviceStorage.selectedDevice">
            <option v-for="id in deviceStorage.availableDevices" :key="id" :value="id">
                {{ id }}
            </option>
            <option :value="null">None</option>
        </select>
    </div>
</template>

<script lang="ts">
import { Component, Inject, Vue } from "vue-property-decorator";

import Storage from "@/storage/Storage";

@Component
export default class DeviceSelector extends Vue {
    @Inject()
    private readonly storage!: Storage;

    get deviceStorage() {
        return this.storage.devices;
    }
}
</script>
