import axios from "axios";

export interface FinanceCategory {
    id: string,
    name: string;
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

export class FinanceApi {
    async getCategories(deviceId: string): Promise<FinanceCategory[]> {
        return (await axios.get("/web/finance/categories", { params: { deviceId } })).data;
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
}
