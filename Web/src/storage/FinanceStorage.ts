import { FinanceAccount, FinanceApi, FinanceCategory } from "@/api/FinanceApi";

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

    private _accountFetch = false;
    private _accounts: FinanceAccount[] | null = null;
    get accounts() {
        const result = this._accounts;
        if (result != null) {
            return [...result];
        }

        if (!this._accountFetch) {
            (async () => {
                this._accountFetch = true;
                this._accounts = await this.api.getAccounts(this.deviceId);
                this._accountFetch = false;
            })();
        }
        return [] as FinanceAccount[];
    }

    private _selectedCurrency: string | null = localStorage.getItem("storage.finance.selected-currency");
    get selectedCurrency() { return this._selectedCurrency; }
    set selectedCurrency(value) {
        if (value == null) {
            localStorage.removeItem("storage.finance.selected-currency");
        } else {
            localStorage.setItem("storage.finance.selected-currency", value);
        }
        this._selectedCurrency = value;
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
