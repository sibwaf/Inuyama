import "./style.scss";

import Vue from "vue";

import ElementUI from "element-ui";
import locale from "element-ui/lib/locale/lang/en";

import { Chart, CategoryScale, LinearScale, BarController, BarElement } from "chart.js";

import App from "./App.vue";
import router from "./router";

Chart.register(CategoryScale, LinearScale, BarController, BarElement);

Vue.use(ElementUI, { locale });
Vue.config.productionTip = false;

new Vue({
    router,
    render: h => h(App)
}).$mount("#app");
