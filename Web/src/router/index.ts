import Vue from "vue";
import VueRouter, { RouteConfig } from "vue-router";

import FinanceScreen from "@/views/FinanceScreen.vue";
import FinanceComparisonScreen from "@/views/FinanceComparisonScreen.vue";

Vue.use(VueRouter);

const routes: Array<RouteConfig> = [
    {
        path: "/",
        name: "FinanceDashboard",
        component: FinanceScreen
    },
    {
        path: "/compare",
        name: "FinanceComparison",
        component: FinanceComparisonScreen
    }
];

const router = new VueRouter({
    routes
});

export default router;
