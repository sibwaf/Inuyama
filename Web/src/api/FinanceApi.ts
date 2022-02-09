import axios from "axios";

export interface FinanceCategory {
    id: string,
    name: string;
}

export interface FinanceAccount {
    id: string;
    name: string;
    balance: number;
}

export enum FinanceOperationDirection {
    INCOME = "income",
    EXPENSE = "expense"
}

export enum FinanceAnalyticGrouping {
    DIRECTION = "direction",
    CATEGORY = "category"
}

export interface FinanceAnalyticFilter {
    start: Date;
    end: Date;

    direction: FinanceOperationDirection | null;
}

export interface FinanceAnalyticSeriesDto {
    readonly timeline: Date[];
    readonly data: Map<string, number[]>;
}

export class FinanceApi {
    async getCategories(deviceId: string): Promise<FinanceCategory[]> {
        return (await axios.get("/web/finance/categories", { params: { deviceId } })).data;
    }

    async getAccounts(deviceId: string): Promise<FinanceAccount[]> {
        return (await axios.get("/web/finance/accounts", { params: { deviceId } })).data;
    }

    async getSummary(deviceId: string, grouping: FinanceAnalyticGrouping | null, filter: FinanceAnalyticFilter): Promise<Map<string, number>> {
        const params = {
            deviceId,
            grouping,
            filter: JSON.stringify(filter)
        };

        const response = await axios.get("/web/finance/analytics/summary", { params });
        return new Map(Object.entries(response.data));
    }

    async getSeries(deviceId: string, grouping: FinanceAnalyticGrouping | null, filter: FinanceAnalyticFilter): Promise<FinanceAnalyticSeriesDto> {
        const params = {
            deviceId,
            grouping,
            filter: JSON.stringify(filter),
            zoneOffset: new Date().getTimezoneOffset() * -60
        };

        const response = await axios.get("/web/finance/analytics/series", { params });

        return {
            timeline: response.data.timeline,
            data: new Map(Object.entries(response.data.data))
        };
    }
}
