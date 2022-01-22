export function getWeightedAverage(values: number[], weightProvider: (value: number, index: number) => number): number {
    const weights = values.map(weightProvider);
    const totalWeight = weights.reduce((acc, it) => acc + it, 0);

    return values
        .map((it, index) => it * weights[index] / totalWeight)
        .reduce((acc, it) => acc + it, 0);
}
