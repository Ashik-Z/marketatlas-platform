import * as echarts from 'echarts/core';
import { LineChart } from 'echarts/charts';
import { GridComponent, TooltipComponent, LegendComponent, DataZoomComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import { maTheme, baseOption } from './chart-theme.js';

echarts.use([LineChart, GridComponent, TooltipComponent, LegendComponent, DataZoomComponent, CanvasRenderer]);

// Draws (or redraws) the nominal vs real price chart inside `el`.
window.marketatlasPriceChart = function (el, json, realLabel) {
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
    legend: { data: ['Nominal', realLabel], textStyle: { color: t.text } },
    grid: { left: 56, right: 24, top: 40, bottom: 70 },
    xAxis: { ...base.xAxis, data: data.dates },
    series: [
      { name: 'Nominal', type: 'line', showSymbol: false, data: data.nominal,
        lineStyle: { color: t.nominal, width: 3 }, itemStyle: { color: t.nominal } },
      { name: realLabel, type: 'line', showSymbol: false, data: data.real,
        lineStyle: { color: t.real, width: 2 }, itemStyle: { color: t.real } }
    ]
  }, true);
};
