<template>
    <div class="responsive-chart">
        <canvas ref="chart"></canvas>
    </div>
</template>

<script lang="ts">
import { Component, Prop, Vue, Watch } from "vue-property-decorator";
import { Chart, ChartItem } from "chart.js";

import { generatePalette, rgbToHex } from "@/utility";

interface ChartData {
    readonly labels: string[];
    readonly values: number[];
    readonly colors: [number, number, number][];
}

@Component
export default class DoughnutChart extends Vue {
    @Prop()
    private readonly data!: [string, number][];

    private chart!: Chart;

    mounted() {
        this.chart = new Chart(this.$refs["chart"] as ChartItem, {
            type: "doughnut",
            data: {
                labels: [],
                datasets: [
                    {
                        data: [],
                        borderWidth: 1,
                        borderColor: "#0005"
                    },
                ],
            },
            options: {
                responsive: true,
            },
        });

        this.onChartDataChanged(this.chartData);
    }

    private get chartData(): ChartData {
        return {
            labels: this.data.map((it) => it[0]),
            values: this.data.map((it) => it[1]),
            colors: generatePalette(this.data.length),
        };
    }

    @Watch("chartData")
    private onChartDataChanged(chartData: ChartData) {
        this.chart.data.labels = chartData.labels;
        this.chart.data.datasets[0].data = chartData.values;
        this.chart.data.datasets[0].backgroundColor = chartData.colors.map((it) => rgbToHex(it, 170));

        this.chart.update();
    }
}
</script>

<style>
</style>
