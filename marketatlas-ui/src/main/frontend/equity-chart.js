import * as echarts from 'echarts/core';
import { LineChart } from 'echarts/charts';
import { GridComponent, TooltipComponent, LegendComponent, DataZoomComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';

echarts.use([LineChart, GridComponent, TooltipComponent, LegendComponent, DataZoomComponent, CanvasRenderer]);

// Draws (or redraws) strategy vs buy-and-hold equity curves inside `el`.
window.marketatlasEquityChart = function (el, json) {
  const data = JSON.parse(json);
  let chart = el._marketatlasChart;
  if (!chart) {
    chart = echarts.init(el);
    el._marketatlasChart = chart;
    new ResizeObserver(() => chart.resize()).observe(el);
  }
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['Strategy', 'Buy & hold'] },
    grid: { left: 64, right: 24, top: 40, bottom: 70 },
    xAxis: { type: 'category', data: data.dates, boundaryGap: false },
    yAxis: { type: 'value', scale: true },
    dataZoom: [{ type: 'inside' }, { type: 'slider' }],
    series: [
      { name: 'Strategy', type: 'line', showSymbol: false, data: data.strategy },
      { name: 'Buy & hold', type: 'line', showSymbol: false, data: data.benchmark }
    ]
  }, true);
};