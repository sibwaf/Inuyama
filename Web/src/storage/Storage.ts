import Vue from "vue";

import { DeviceStorage } from "@/storage/DeviceStorage";
import { FinanceStorage } from "@/storage/FinanceStorage";

class ScopedStorage {

    readonly finance: FinanceStorage

    constructor(deviceId: string) {
        this.finance = new FinanceStorage(deviceId);
    }
}

export default class Storage {

    readonly devices = new DeviceStorage();

    private scopes: Map<string, ScopedStorage> = new Map();

    ofCurrentDevice() {
        const deviceId = this.devices.selectedDevice;
        if (deviceId == null) {
            return null;
        }

        return this.ofDevice(deviceId);
    }

    ofDevice(deviceId: string) {
        const existing = this.scopes.get(deviceId);
        if (existing != null) {
            return existing;
        }

        const result = Vue.observable(new ScopedStorage(deviceId));
        this.scopes.set(deviceId, result);
        return result;
    }
};
