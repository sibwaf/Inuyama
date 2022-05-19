import "./style.scss";

import Vue from "vue";

import ElementUI from "element-ui";
import locale from "element-ui/lib/locale/lang/en";

import {
    Chart,
    Tooltip,

    CategoryScale,
    LinearScale,

    LineController,
    BarController,
    DoughnutController,
    BubbleController,
    
    LineElement,
    PointElement,
    BarElement,
    ArcElement
} from "chart.js";

import App from "./App.vue";
import router from "./router";

Chart.register(Tooltip);
Chart.register(CategoryScale, LinearScale);
Chart.register(LineController, BarController, DoughnutController, BubbleController);
Chart.register(LineElement, PointElement, BarElement, ArcElement);

Vue.use(ElementUI, { locale });
Vue.config.productionTip = false;

new Vue({
    router,
    render: h => h(App)
}).$mount("#app");
