import Vue from "vue";
import VueRouter, { RouteConfig } from "vue-router";

import Home from "@/views/Home.vue";
import FinanceScreen from "@/views/finance/FinanceScreen.vue";
import FinanceComparisonScreen from "@/views/finance/FinanceComparisonScreen.vue";
import FinanceOverviewScreen from "@/views/finance/FinanceOverviewScreen.vue";

Vue.use(VueRouter);

const routes: Array<RouteConfig> = [
    {
        path: "/",
        name: "Home",
        component: Home
    },
    {
        path: "/finance",
        name: "Finance",
        component: FinanceScreen
    },
    {
        path: "/finance/overview",
        name: "FinanceOverview",
        component: FinanceOverviewScreen
    },
    {
        path: "/finance/compare",
        name: "FinanceComparison",
        component: FinanceComparisonScreen
    }
];

const router = new VueRouter({
    routes
});

export default router;
