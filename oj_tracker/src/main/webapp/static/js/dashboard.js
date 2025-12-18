document.addEventListener('DOMContentLoaded', function() {
    fetchStats();
    fetchPlatforms();
    renderRatingChart();
    renderHeatmap();
});

async function fetchStats() {
    const res = await fetch('/api/users/summary?platformCode=CF');
    const data = await res.json();
    if (data.code === 200) {
        document.getElementById('totalSolved').textContent = data.data.solvedTotal; // 后端是 solvedTotal
        // 如果后端暂时没提供 weeklySolved，可以先显示 solvedTotal 或 0，防止报错
        document.getElementById('weeklySolved').textContent = data.data.solvedTotal || 0;
        document.getElementById('activeDays').textContent = data.data.activeDays;
    }
}

async function fetchPlatforms() {
    const res = await fetch('/api/me/platforms');
    const data = await res.json();
    const list = document.getElementById('platformList');
    if (data.code === 200) {
        document.getElementById('platformCount').textContent = data.data.length;
        list.innerHTML = data.data.map(p => `
            <li class="list-group-item d-flex justify-content-between align-items-center border-0 px-0">
                <div>
                    <span class="fw-bold">${p.platformName}</span><br>
                    <small class="text-muted">${p.identifierValue || '未设置'}</small>
                </div>
                <span class="badge bg-primary rounded-pill">Rank: ${p.verified ? '已校验' : '同步中'}</span>
            </li>
        `).join('');
    }
}

async function renderRatingChart() {
    const chartDom = document.getElementById('ratingChart');
    if (!chartDom) return;
    const chart = echarts.init(chartDom);

    // 调用 API
    const res = await fetch('/api/user/rating/history?platformCode=CF');
    const result = await res.json();

    if (result.code === 200) {
        const option = {
            tooltip: {
                trigger: 'axis',
                // 增强提示框，显示比赛名称和积分变化
                formatter: function (params) {
                    const item = result.data[params[0].dataIndex];
                    return `<strong>${item.contestName}</strong><br/>` +
                           `时间: ${item.time.replace('T', ' ')}<br/>` +
                           `rating: ${item.rating} (Δ${item.delta > 0 ? '+' + item.delta : item.delta})`;
                }
            },
            xAxis: {
                type: 'category',
                // 修复点：将 i.date 改为 i.time，并只取日期部分
                data: result.data.map(i => i.time.split('T')[0])
            },
            yAxis: {
                type: 'value',
                scale: true,
                axisLabel: { formatter: '{value}' }
            },
            series: [{
                // 确保字段名 rating 匹配
                data: result.data.map(i => i.rating),
                type: 'line',
                smooth: true,
                symbolSize: 8,
                areaStyle: {
                    color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                        { offset: 0, color: 'rgba(78, 115, 223, 0.3)' },
                        { offset: 1, color: 'rgba(78, 115, 223, 0)' }
                    ])
                },
                itemStyle: { color: '#4e73df' }
            }]
        };
        chart.setOption(option);
    }
}

/**
 * 热力图渲染逻辑
 * 对应 API: http://localhost:8080/api/user/activity/heatmap?platformCode=CF&days=365
 */
async function renderHeatmap() {
    const chartDom = document.getElementById('heatmapChart');
    if (!chartDom) return;
    const chart = echarts.init(chartDom);

    // 获取最近一年的数据
    const res = await fetch('/api/user/activity/heatmap?platformCode=CF&days=365');
    const result = await res.json();

    if (result.code === 200) {
        // ECharts 日历图需要 [[日期, 提交数], ...] 格式
        const heatmapData = result.data.map(item => [
            item.day, // 格式如 "2025-12-18"
            item.submitCnt
        ]);

        const option = {
            tooltip: {
                position: 'top',
                formatter: function (p) {
                    const format = echarts.format.formatTime('yyyy-MM-dd', p.data[0]);
                    return `${format}: ${p.data[1]} 提交`;
                }
            },
            visualMap: {
                min: 0,
                max: 10, // 根据用户平均解题量调整
                type: 'piecewise',
                orient: 'horizontal',
                left: 'center',
                top: 0,
                pieces: [
                    {min: 0, max: 0, label: '无提交', color: '#ebedf0'},
                    {min: 1, max: 2, label: '1-2', color: '#9be9a8'},
                    {min: 3, max: 5, label: '3-5', color: '#40c463'},
                    {min: 6, max: 8, label: '6-8', color: '#30a14e'},
                    {min: 9, label: '9+', color: '#216e39'}
                ]
            },
            calendar: {
                top: 60,
                left: 30,
                right: 30,
                cellSize: ['auto', 18],
                range: getHeatmapRange(), // 获取时间轴范围
                itemStyle: {
                    borderWidth: 0.5,
                    borderColor: '#fff'
                },
                yearLabel: { show: false },
                dayLabel: { nameMap: ['日', '一', '二', '三', '四', '五', '六'] },
                monthLabel: { nameMap: 'cn' }
            },
            series: {
                type: 'heatmap',
                coordinateSystem: 'calendar',
                data: heatmapData
            }
        };
        chart.setOption(option);
    }
}

/**
 * 获取最近一年的日期范围
 */
function getHeatmapRange() {
    const today = new Date();
    const oneYearAgo = new Date();
    oneYearAgo.setDate(today.getDate() - 364);
    return [
        echarts.format.formatTime('yyyy-MM-dd', oneYearAgo),
        echarts.format.formatTime('yyyy-MM-dd', today)
    ];
}

// 同时响应两个图表
window.addEventListener('resize', () => {
    echarts.getInstanceByDom(document.getElementById('ratingChart'))?.resize();
    echarts.getInstanceByDom(document.getElementById('heatmapChart'))?.resize();
});