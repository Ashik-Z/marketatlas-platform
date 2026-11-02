import * as echarts from 'echarts/core';
import { LineChart } from 'echarts/charts';
import { GridComponent, TooltipComponent, LegendComponent, DataZoomComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import { maTheme, baseOption } from './chart-theme.js';

echarts.use([LineChart, GridComponent, TooltipComponent, LegendComponent, DataZoomComponent, CanvasRenderer]);

// Draws (or redraws) strategy vs buy-and-hold equity curves inside `el`.
window.marketatlasEquityChart = function (el, json) {
  const data = JSON.parse(json);
  const t = maTheme();
  let chart = el._marketatlasChart;
  if (!chart) {
    chart = echarts.init(el);
    el._marketatlasChart = chart;
    new ResizeObserver(() => chart.resize()).observe(el);
  }
  const base = baseOption(t);
  chart.setOption({
    ...base,
    legend: { data: ['Strategy', 'Buy & hold'], textStyle: { color: t.text } },
    grid: { left: 64, right: 24, top: 40, bottom: 70 },
    xAxis: { ...base.xAxis, data: data.dates },
    series: [
      { name: 'Strategy', type: 'line', showSymbol: false, data: data.strategy,
        lineStyle: { color: t.real, width: 2 }, itemStyle: { color: t.real } },
      { name: 'Buy & hold', type: 'line', showSymbol: false, data: data.benchmark,
        lineStyle: { color: t.nominal, width: 2 }, itemStyle: { color: t.nominal } }
    ]
  }, true);
};
