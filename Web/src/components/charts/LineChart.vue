<template>
    <div class="responsive-chart">
        <canvas ref="chart"></canvas>
    </div>
</template>

<script lang="ts">
import { Component, Prop, Vue, Watch } from "vue-property-decorator";
import { Chart, ChartDataset, ChartItem } from "chart.js";

import { generatePalette, rgbToHex } from "@/utility";

interface ChartState {
    readonly xs: any[];
    readonly labels: string[];
    readonly values: number[][];
    readonly colors: [number, number, number][];
}

export interface ChartData<T> {
    readonly xs: T[];
    readonly lines: ChartLine[];
}

export interface ChartLine {
    readonly name: string;
    readonly values: number[];
}

@Component
export default class LineChart extends Vue {
    @Prop()
    private readonly data!: ChartData<any>;
    @Prop({ default: () => (value: number) => value.toString() })
    private readonly valueFormatter!: (value: number) => string;

    private chart!: Chart;

    mounted() {
        this.chart = new Chart(this.$refs["chart"] as ChartItem, {
            type: "line",
            data: {
                labels: [],
                datasets: [],
            },
            options: {
                scales: {
                    y: {
                        ticks: {
                            callback: (value) =>
                                this.valueFormatter(value as number),
                        },
                    },
                },
                plugins: {
                    tooltip: {
                        callbacks: {
                            label: (item) =>
                                this.valueFormatter(item.raw as number),
                        },
                    },
                },
                maintainAspectRatio: false,
                responsive: true,
            },
        });

        this.onChartStateChanged(this.chartState);
    }

    private get chartState(): ChartState {
        return {
            xs: [...this.data.xs],
            labels: this.data.lines.map((it) => it.name),
            values: this.data.lines.map((it) => it.values),
            colors: generatePalette(this.data.lines.length),
        };
    }

    @Watch("chartState")
    private onChartStateChanged(chartState: ChartState) {
        const datasets: ChartDataset[] = [];
        for (let i = 0; i < chartState.labels.length; i++) {
            datasets.push({
                label: chartState.labels[i],
                data: chartState.values[i],
                backgroundColor: rgbToHex(chartState.colors[i], 170),
                borderColor: rgbToHex(chartState.colors[i], 170),
            });
        }

        this.chart.data.labels = chartState.xs;
        this.chart.data.datasets = datasets;

        this.chart.update();
    }
}
</script>

<style>
</style>
