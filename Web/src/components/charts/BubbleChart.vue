<template>
    <div class="responsive-chart">
        <canvas ref="chart"></canvas>
    </div>
</template>

<script lang="ts">
import { Component, Prop, Vue, Watch } from "vue-property-decorator";
import { Chart, ChartItem } from "chart.js";

import { generatePalette, rgbToHex } from "@/utility";

interface ChartState {
    readonly horizontal: any[];
    readonly vertical: any[];
    readonly data: Bubble[];
    readonly color: [number, number, number];
}

interface Bubble {
    x: number;
    y: number;
    r: number;
    value: number;
}

export interface ChartData {
    readonly horizontal: any[];
    readonly vertical: any[];
    readonly values: (number | null)[][];
}

const MIN_RADIUS = 4;
const MAX_RADIUS = 16;

@Component
export default class BubbleChart extends Vue {
    @Prop()
    private readonly data!: ChartData;
    @Prop({ default: () => (value: number) => value.toString() })
    private readonly valueFormatter!: (value: number) => string;

    private chart!: Chart;

    mounted() {
        this.chart = new Chart(this.$refs["chart"] as ChartItem, {
            type: "bubble",
            data: {
                datasets: [
                    {
                        data: [],
                    },
                ],
            },
            options: {
                scales: {
                    x: {
                        min: -1,
                        ticks: {
                            stepSize: 1,
                        },
                    },
                    y: {
                        min: -1,
                        ticks: {
                            stepSize: 1,
                        },
                    },
                },
                responsive: true,
            },
        });

        this.onChartStateChanged(this.chartState);
    }

    private get chartState(): ChartState {
        let minValue: number = 0;
        let maxValue: number = 0;

        const data: Bubble[] = [];
        for (let x = 0; x < this.data.values.length; x++) {
            for (let y = 0; y < this.data.values[x].length; y++) {
                const value = this.data.values[x][y];
                if (value != null) {
                    minValue = Math.min(minValue, value);
                    maxValue = Math.max(maxValue, value);
                    data.push({ x, y, r: MAX_RADIUS, value });
                }
            }
        }

        if (minValue != maxValue) {
            const valueJitter = maxValue - minValue;
            const radiusJitter = MAX_RADIUS - MIN_RADIUS;
            for (const point of data) {
                const valuePercentage = (point.value - minValue) / valueJitter;
                point.r = valuePercentage * radiusJitter + MIN_RADIUS;
            }
        }

        return {
            horizontal: this.data.horizontal,
            vertical: this.data.vertical,
            data,
            color: generatePalette(1)[0],
        };
    }

    @Watch("chartState")
    private onChartStateChanged(chartState: ChartState) {
        this.chart.data.datasets[0].backgroundColor = rgbToHex(
            chartState.color,
            170
        );

        this.chart.data.labels = chartState.data.map((it) => it.value);
        this.chart.data.datasets[0].data = chartState.data;

        this.chart.options.plugins!.tooltip!.callbacks!.title = (item) =>
            item
                .map((it) => {
                    const vertical = chartState.vertical[it.parsed.y];
                    const horizontal = chartState.horizontal[it.parsed.x];
                    return `${vertical} ${horizontal}`;
                })
                .join("; ");
        this.chart.options.plugins!.tooltip!.callbacks!.label = (item) =>
            this.valueFormatter((item.raw as Bubble).value);

        this.chart.options.scales!.x!.ticks!.callback = (value) =>
            chartState.horizontal[value as number];
        this.chart.options.scales!.x!.max = chartState.horizontal.length;

        this.chart.options.scales!.y!.ticks!.callback = (value) =>
            chartState.vertical[value as number];
        this.chart.options.scales!.y!.max = chartState.vertical.length;

        this.chart.update();
    }
}
</script>
