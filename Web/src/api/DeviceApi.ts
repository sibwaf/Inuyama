import axios from "axios";

export class DeviceApi {
    async listDevices(): Promise<string[]> {
        return (await axios.get("/web/devices")).data;
    }
}
