export default {
    first<T>(items: T[]): T | undefined {
        return items[0];
    },

    last<T>(items: T[]): T | undefined {
        return items.slice(-1)[0];
    },

    head<T>(items: T[]): T[] {
        return this.dropLast(items, 1);
    },

    tail<T>(items: T[]): T[] {
        return this.dropFirst(items, 1);
    },

    takeFirst<T>(items: T[], count: number): T[] {
        return items.slice(0, count);
    },

    takeLast<T>(items: T[], count: number): T[] {
        return items.slice(-count);
    },

    dropFirst<T>(items: T[], count: number): T[] {
        return items.slice(count);
    },

    dropLast<T>(items: T[], count: number): T[] {
        return items.slice(0, -count);
    },

    slidingWindow<T>(items: T[], size: number): T[][] {
        if (items.length == 0) {
            return [];
        }
        if (items.length < size) {
            return [items];
        }

        const result: T[][] = [];
        for (let i = 0; i < items.length - size + 1; i++) {
            result.push(items.slice(i, i + size));
        }
        return result;
    },
}
