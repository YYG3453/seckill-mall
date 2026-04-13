<template>
  <div class="dash">
    <div class="head">
      <h2>数据看板</h2>
      <p class="sub">经营指标、趋势与结构分析；下方保留秒杀 QPS 与库存采样监控。</p>
      <el-button size="small" :loading="loading" @click="reload">刷新</el-button>
    </div>

    <el-row :gutter="12" class="kpi-row">
      <el-col v-for="c in kpiDefs" :key="c.key" :xs="12" :sm="8" :md="6" :lg="3">
        <el-card shadow="hover" class="kpi-card">
          <div class="kpi-val">{{ formatKpi(c.key) }}</div>
          <div class="kpi-label">{{ c.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" class="chart-card">
          <template #header>近 14 日下单量与成交额</template>
          <div ref="elBar" class="chart" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="chart-card">
          <template #header>订单状态分布</template>
          <div ref="elPie" class="chart" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" class="chart-card">
          <template #header>成交额日 K 线（按日：首单开盘、末单收盘、当日单笔低/高）</template>
          <div ref="elK" class="chart" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="chart-card">
          <template #header>分类销售额矩形树图</template>
          <div ref="elTree" class="chart" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :span="24">
        <el-card shadow="never" class="chart-card">
          <template #header>TOP 商品成交额（柱状）</template>
          <div ref="elTop" class="chart-sm" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :span="24">
        <el-card shadow="never" class="chart-card">
          <template #header>最近一小时秒杀接口估算 QPS</template>
          <div ref="elQps" class="chart-sm" />
        </el-card>
      </el-col>
    </el-row>

    <el-collapse class="collapse">
      <el-collapse-item title="最近一小时监控明细表" name="1">
        <p class="tip">QPS 由秒杀分钟桶估算；库存差异为告警采样值。</p>
        <el-table :data="monitorRows" height="360" size="small">
          <el-table-column prop="minute" label="分钟" min-width="180" />
          <el-table-column prop="qps" label="估算 QPS" width="120" />
          <el-table-column prop="stockDiffSample" label="库存差异采样" width="140" />
        </el-table>
      </el-collapse-item>
    </el-collapse>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { adminMonitorStats, adminStatisticsDashboard } from '../api/admin'

/**
 * 调用关系（本页）：
 * - 监控曲线：api/admin.adminMonitorStats -> AdminController#monitorStats -> MonitorService
 * - 经营看板：api/admin.adminStatisticsDashboard -> AdminController#statisticsDashboard -> AdminStatisticsService
 * - 图表层：本页消费接口数据并渲染 ECharts，不直接访问后端
 */
const loading = ref(false)
const stats = ref({
  kpis: {},
  dailyTrend: [],
  orderStatusPie: [],
  dailyKline: [],
  categoryTreemap: [],
  topProducts: []
})
const monitorRows = ref([])

const elBar = ref(null)
const elPie = ref(null)
const elK = ref(null)
const elTree = ref(null)
const elTop = ref(null)
const elQps = ref(null)

const kpiDefs = [
  { key: 'paidRevenue', label: '成交销售额(元)' },
  { key: 'paidOrderCount', label: '成交订单数' },
  { key: 'totalOrders', label: '订单总数' },
  { key: 'pendingOrderCount', label: '待支付' },
  { key: 'todayPaidRevenue', label: '今日成交(元)' },
  { key: 'todayOrderCount', label: '今日下单' },
  { key: 'userCount', label: '用户数' },
  { key: 'productCount', label: '商品数' }
]

const charts = []

function formatKpi(key) {
  const v = stats.value.kpis?.[key]
  if (v === undefined || v === null) return '—'
  if (key.includes('Revenue') || key === 'paidRevenue' || key === 'todayPaidRevenue') {
    return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  }
  return String(v)
}

function disposeCharts() {
  while (charts.length) {
    const c = charts.pop()
    c.dispose()
  }
}

function setBar() {
  const el = elBar.value
  if (!el) return
  const trend = stats.value.dailyTrend || []
  const labels = trend.map((r) => r.label)
  const counts = trend.map((r) => r.orderCount)
  const rev = trend.map((r) => r.paidRevenue)
  const chart = echarts.init(el)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['下单数', '成交额(元)'] },
    grid: { left: 48, right: 48, top: 40, bottom: 28 },
    xAxis: { type: 'category', data: labels, axisLabel: { rotate: 30 } },
    yAxis: [
      { type: 'value', name: '单' },
      { type: 'value', name: '元' }
    ],
    series: [
      { name: '下单数', type: 'bar', data: counts, itemStyle: { color: '#5470c6' } },
      {
        name: '成交额(元)',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        data: rev,
        itemStyle: { color: '#91cc75' }
      }
    ]
  })
  charts.push(chart)
}

function setPie() {
  const el = elPie.value
  if (!el) return
  const data = stats.value.orderStatusPie || []
  const chart = echarts.init(el)
  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, type: 'scroll' },
    series: [
      {
        type: 'pie',
        radius: ['36%', '68%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { formatter: '{b}\n{d}%' },
        data
      }
    ]
  })
  charts.push(chart)
}

function setKline() {
  const el = elK.value
  if (!el) return
  const raw = stats.value.dailyKline || []
  const dates = raw.map((row) => row[0])
  const values = raw.map((row) => [row[1], row[2], row[3], row[4]])
  const chart = echarts.init(el)
  if (!raw.length) {
    chart.setOption({
      title: {
        text: '暂无已支付订单，无法绘制 K 线',
        left: 'center',
        top: 'center',
        textStyle: { color: '#999', fontSize: 14 }
      }
    })
  } else {
    chart.setOption({
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'cross' }
      },
      grid: { left: 56, right: 24, top: 24, bottom: 48 },
      xAxis: { type: 'category', data: dates, boundaryGap: true },
      yAxis: { scale: true, name: '元' },
      series: [
        {
          type: 'candlestick',
          name: '日 K',
          data: values,
          itemStyle: {
            color: '#ef5350',
            color0: '#26a69a',
            borderColor: '#ef5350',
            borderColor0: '#26a69a'
          }
        }
      ]
    })
  }
  charts.push(chart)
}

function setTreemap() {
  const el = elTree.value
  if (!el) return
  const data = stats.value.categoryTreemap || []
  const chart = echarts.init(el)
  if (!data.length) {
    chart.setOption({
      title: {
        text: '暂无成交明细按分类汇总',
        left: 'center',
        top: 'center',
        textStyle: { color: '#999', fontSize: 14 }
      }
    })
  } else {
    chart.setOption({
      tooltip: { formatter: (p) => `${p.name}<br/>¥${p.value}` },
      series: [
        {
          type: 'treemap',
          roam: false,
          breadcrumb: { show: false },
          label: { show: true, formatter: '{b}\n¥{c}' },
          upperLabel: { show: false },
          itemStyle: { borderColor: '#fff', gapWidth: 2 },
          data
        }
      ]
    })
  }
  charts.push(chart)
}

function setTop() {
  const el = elTop.value
  if (!el) return
  const list = stats.value.topProducts || []
  const names = list.map((x) => x.name)
  const vals = list.map((x) => x.value)
  const chart = echarts.init(el)
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 120, right: 40, top: 16, bottom: 24 },
    xAxis: { type: 'value', name: '元' },
    yAxis: { type: 'category', data: names, inverse: true },
    series: [{ type: 'bar', data: vals, itemStyle: { color: '#73c0de' } }]
  })
  charts.push(chart)
}

function setQps() {
  const el = elQps.value
  if (!el) return
  const series = monitorRows.value || []
  const labels = series.map((r) => {
    const s = String(r.minute || '')
    return s.length > 16 ? s.slice(5, 16) : s
  })
  const qps = series.map((r) => r.qps)
  const chart = echarts.init(el)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 44, right: 20, top: 24, bottom: 64 },
    xAxis: { type: 'category', data: labels, axisLabel: { rotate: 45, fontSize: 10 } },
    yAxis: { type: 'value', name: 'QPS' },
    series: [{ type: 'line', smooth: true, areaStyle: { opacity: 0.12 }, data: qps, color: '#e91e63' }]
  })
  charts.push(chart)
}

function renderCharts() {
  disposeCharts()
  nextTick(() => {
    setBar()
    setPie()
    setKline()
    setTreemap()
    setTop()
    setQps()
    charts.forEach((c) => c.resize())
  })
}

function onResize() {
  charts.forEach((c) => c.resize())
}

async function reload() {
  loading.value = true
  try {
    const [dashRes, monRes] = await Promise.all([adminStatisticsDashboard(), adminMonitorStats()])
    stats.value = dashRes.data || stats.value
    monitorRows.value = (monRes.data?.series || []).slice(-60)
    await nextTick()
    renderCharts()
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  window.addEventListener('resize', onResize)
  await reload()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  disposeCharts()
})
</script>

<style scoped>
.dash {
  max-width: 1400px;
}
.head {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.head h2 {
  margin: 0;
  font-size: 20px;
}
.sub {
  flex: 1;
  margin: 4px 0 0;
  color: #666;
  font-size: 13px;
  min-width: 200px;
}
.kpi-row {
  margin-bottom: 16px;
}
.kpi-card {
  margin-bottom: 12px;
  text-align: center;
}
.kpi-val {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
}
.kpi-label {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}
.chart-row {
  margin-bottom: 16px;
}
.chart-card {
  margin-bottom: 0;
}
.chart {
  height: 320px;
}
.chart-sm {
  height: 280px;
}
.collapse {
  margin-top: 8px;
}
.tip {
  color: #666;
  font-size: 13px;
  margin-bottom: 8px;
}
</style>
