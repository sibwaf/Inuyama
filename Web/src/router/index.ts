import Vue from "vue";
import VueRouter, { RouteConfig } from "vue-router";

import Home from "@/views/Home.vue";
import FinanceComparisonScreen from "@/views/finance/FinanceComparisonScreen.vue";

Vue.use(VueRouter);

const routes: Array<RouteConfig> = [
    {
        path: "/",
        name: "Home",
        component: Home
    },
    {
        path: "/finance/compare",
        name: "Finance",
        component: FinanceComparisonScreen
    }
];

const router = new VueRouter({
    routes
});

export default router;
