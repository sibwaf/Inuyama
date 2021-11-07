import { DeviceApi } from "@/api/DeviceApi";

export class DeviceStorage {
    private api = new DeviceApi();

    private _availableDevices: string[] = [];

    get availableDevices(): string[] {
        return [...this._availableDevices];
    }

    async refreshAvailableDevices() {
        this._availableDevices = await this.api.listDevices();
    }

    private _selectedDevice: string | null = localStorage.getItem("storage.devices.selected-device");;
    get selectedDevice() { return this._selectedDevice; }
    set selectedDevice(value) {
        if (value == null) {
            localStorage.removeItem("storage.devices.selected-device");
        } else {
            localStorage.setItem("storage.devices.selected-device", value);
        }
        this._selectedDevice = value;
    }
}
