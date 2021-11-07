import { FinanceApi, FinanceCategory } from "@/api/FinanceApi";

export class FinanceStorage {

    private readonly api = new FinanceApi();
    private readonly deviceId: string;

    private _categoryFetch = false;
    private _categories: FinanceCategory[] | null = null;
    get categories() {
        const result = this._categories;
        if (result != null) {
            return [...result];
        }

        (async () => await this.refreshCategories())();
        return [] as FinanceCategory[];
    }

    constructor(deviceId: string) {
        this.deviceId = deviceId;
    }

    async refreshCategories() {
        if (this._categoryFetch) {
            return;
        }

        this._categoryFetch = true;
        this._categories = await this.api.getCategories(this.deviceId);
        this._categoryFetch = false;
    }
};
