export function getWeightedAverage(values: number[], weightProvider: (value: number, index: number) => number): number {
    const weights = values.map(weightProvider);
    const totalWeight = weights.reduce((acc, it) => acc + it, 0);

    return values
        .map((it, index) => it * weights[index] / totalWeight)
        .reduce((acc, it) => acc + it, 0);
}

export function makeAutoRegression(values: number[]): (depth: number) => number {
    const ax: number[][] = [];
    const b: number[] = [];

    // values.length = 4
    // v[1] = c[0] * v[0]
    // v[2] = c[0] * v[1] + c[1] * v[0]
    // v[3] = c[0] * v[2] + c[1] * v[1] + c[2] * v[0]
    // => row = 1 .. 3, column = 0 .. 2

    for (let rowIndex = 1; rowIndex < values.length; rowIndex++) {
        const row: number[] = [];

        for (let columnIndex = 0; columnIndex < values.length - 1; columnIndex++) {
            if (columnIndex < rowIndex) {
                // row = 1, column = 0 => v[0]
                // row = 3, column = 0 => v[2]
                // row = 3, column = 1 => v[1]
                row.push(values[rowIndex - columnIndex - 1]);
            } else {
                row.push(0);
            }
        }

        ax.push(row);
        b.push(values[rowIndex]);
    }

    const coeff = solveSystemGauss(ax, b);
    const cache = new Map<number, number>();

    function compute(depth: number): number {
        if (depth < 0) {
            return values[values.length + depth];
        }

        const cachedResult = cache.get(depth);
        if (cachedResult != null) {
            return cachedResult;
        }

        // coeff.length = 4, depth = 0
        // result = coeff[0]f(-1) + coeff[1]f(-2) + coeff[2]f(-3) + coeff[3]f(-4)

        let result = 0;
        for (let i = 0; i < coeff.length; i++) {
            result += coeff[i] * compute(depth - i - 1);
        }

        cache.set(depth, result);
        return result;
    }

    return (depth) => compute(depth);
}

export function makePolynomialRegression(values: Map<number, number>): (value: number) => number {
    const points = [...values];
    if (points.length == 0) {
        // @ts-ignore
        return (_) => undefined;
    }

    const ax: number[][] = [];
    const b: number[] = [];
    for (const [x, y] of points) {
        const row: number[] = [];
        for (let power = 0; power < points.length; power++) {
            row.push(Math.pow(x, power));
        }

        ax.push(row);
        b.push(y);
    }

    const coeff = solveSystemGauss(ax, b);

    return (x) => {
        let result = 0;
        for (let power = 0; power < coeff.length; power++) {
            result += coeff[power] * Math.pow(x, power);
        }
        return result;
    };
}

export function solveSystemGauss(ax: number[][], b: number[]): number[] {
    const solveAx: number[][] = [];
    for (const row of ax) {
        solveAx.push([...row]);
    }
    const solveB = [...b];

    for (let mainRowIndex = 0; mainRowIndex < solveAx.length; mainRowIndex++) {
        const mainRow = solveAx[mainRowIndex];
        const mainCoeff = mainRow[mainRowIndex];

        for (let columnIndex = mainRowIndex; columnIndex < mainRow.length; columnIndex++) {
            mainRow[columnIndex] /= mainCoeff;
        }
        solveB[mainRowIndex] /= mainCoeff;

        if (mainRowIndex >= solveAx.length - 1) {
            continue;
        }

        for (let nextRowIndex = mainRowIndex + 1; nextRowIndex < solveAx.length; nextRowIndex++) {
            const nextRow = solveAx[nextRowIndex];;
            const nextCoeff = nextRow[mainRowIndex];

            for (let columnIndex = mainRowIndex; columnIndex < nextRow.length; columnIndex++) {
                nextRow[columnIndex] -= mainRow[columnIndex] * nextCoeff;
            }
            solveB[nextRowIndex] -= solveB[mainRowIndex] * nextCoeff;
        }
    }

    const result: number[] = [];
    for (let rowIndex = solveAx.length - 1; rowIndex >= 0; rowIndex--) {
        const coeff = solveAx[rowIndex][rowIndex];
        if (Math.abs(coeff) < 0.00001) {
            result.splice(0, 0, 0);
            continue;
        }

        let value = solveB[rowIndex];
        for (let columnIndex = rowIndex + 1; columnIndex < solveAx.length; columnIndex++) {
            value -= solveAx[rowIndex][columnIndex] * result[columnIndex - rowIndex - 1];
        }

        result.splice(0, 0, value / coeff);
    }
    return result;
}

export function derivative(nums: number[]): number[] {
    const result: number[] = [];
    for (let i = 1; i < nums.length; i++) {
        result.push(nums[i] - nums[i - 1]);
    }
    return result;
}

export function median(nums: number[]): number | null {
    const sorted = [...nums].sort((first, second) => first - second);
    if (nums.length % 2 == 1) {
        return sorted[Math.floor(nums.length / 2)];
    } else {
        return (sorted[nums.length / 2 - 1] + sorted[nums.length / 2]) / 2;
    }
}

export function sum(nums: number[]): number {
    return nums.reduce((acc, it) => acc + it, 0);
}
