// Reads the MarketAtlas palette from the CSS tokens in marketatlas.css so charts follow the theme.
export function maTheme() {
  const css = getComputedStyle(document.documentElement);
  const v = (name, fallback) => css.getPropertyValue(name).trim() || fallback;
  return {
    nominal: v('--ma-nominal', '#d98aa3'),
    real: v('--ma-real', '#ebc79e'),
    text: v('--ma-text', '#e0c8b0'),
    muted: v('--ma-text-muted', '#a8917f'),
    line: v('--ma-line', '#4f3e47'),
    card: v('--ma-card', '#3b2a33'),
    burgundy: v('--ma-burgundy', '#742c42')
  };
}

// Shared chart chrome: transparent background, themed text, axes, tooltip, legend and zoom slider.
export function baseOption(t) {
  return {
    backgroundColor: 'transparent',
    textStyle: { color: t.text },
    tooltip: { trigger: 'axis', backgroundColor: t.card, borderColor: t.line, textStyle: { color: t.text } },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      axisLine: { lineStyle: { color: t.line } },
      axisLabel: { color: t.muted },
      axisTick: { lineStyle: { color: t.line } },
      splitLine: { show: false }
    },
    yAxis: {
      type: 'value',
      scale: true,
      axisLine: { show: false },
      axisLabel: { color: t.muted },
      splitLine: { lineStyle: { color: t.line, opacity: 0.6 } }
    },
    dataZoom: [
      { type: 'inside' },
      {
        type: 'slider',
        borderColor: t.line,
        backgroundColor: 'transparent',
        fillerColor: 'rgba(235, 199, 158, 0.15)',
        textStyle: { color: t.muted },
        dataBackground: { lineStyle: { color: t.line }, areaStyle: { color: t.line, opacity: 0.4 } },
        selectedDataBackground: { lineStyle: { color: t.real }, areaStyle: { color: t.real, opacity: 0.2 } },
        handleStyle: { color: t.real, borderColor: t.real },
        moveHandleStyle: { color: t.burgundy }
      }
    ]
  };
}
