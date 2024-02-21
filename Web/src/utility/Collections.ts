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
}
