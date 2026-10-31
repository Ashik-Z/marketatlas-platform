import * as echarts from 'echarts/core';
import { LineChart } from 'echarts/charts';
import { GridComponent, TooltipComponent, LegendComponent, DataZoomComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';

echarts.use([LineChart, GridComponent, TooltipComponent, LegendComponent, DataZoomComponent, CanvasRenderer]);

// Draws (or redraws) the nominal vs real price chart inside `el`.
window.marketatlasPriceChart = function (el, json, realLabel) {
  const data = JSON.parse(json);
  let chart = el._marketatlasChart;
  if (!chart) {
    chart = echarts.init(el);
    el._marketatlasChart = chart;
    new ResizeObserver(() => chart.resize()).observe(el);
  }
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['Nominal', realLabel] },
    grid: { left: 56, right: 24, top: 40, bottom: 70 },
    xAxis: { type: 'category', data: data.dates, boundaryGap: false },
    yAxis: { type: 'value', scale: true },
    dataZoom: [{ type: 'inside' }, { type: 'slider' }],
    series: [
      { name: 'Nominal', type: 'line', showSymbol: false, data: data.nominal },
      { name: realLabel, type: 'line', showSymbol: false, data: data.real }
    ]
  }, true);
};