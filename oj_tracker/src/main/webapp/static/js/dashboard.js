document.addEventListener('DOMContentLoaded', function() {
    // 1. 加载基础统计数据
    fetchStats();
    // 2. 加载平台列表
    fetchPlatforms();
    // 3. 渲染 Rating 折线图
    renderRatingChart();
    // 4. 渲染热力图
    renderHeatmap();
});

async function fetchStats() {
    const res = await fetch('/api/user-stats/summary');
    const data = await res.json();
    if (data.code === 200) {
        document.getElementById('totalSolved').textContent = data.data.totalSolved;
        document.getElementById('weeklySolved').textContent = data.data.weeklySolved;
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
            <li class="list-group-item d-flex justify-content-between align-items-center">
                <div>
                    <span class="fw-bold">${p.platformName}</span><br>
                    <small class="text-muted">${p.platformHandle}</small>
                </div>
                <span class="badge bg-primary rounded-pill">${p.currentRating || 'N/A'}</span>
            </li>
        `).join('');
    }
}

async function renderRatingChart() {
    const chart = echarts.init(document.getElementById('ratingChart'));
    const res = await fetch('/api/user-rating/history');
    const result = await res.json();

    if (result.code === 200) {
        const option = {
            tooltip: { trigger: 'axis' },
            xAxis: { type: 'category', data: result.data.map(i => i.date) },
            yAxis: { type: 'value', scale: true },
            series: [{
                data: result.data.map(i => i.rating),
                type: 'line',
                smooth: true,
                areaStyle: { opacity: 0.1 },
                itemStyle: { color: '#4e73df' }
            }]
        };
        chart.setOption(option);
    }
}

async function logout() {
    await fetch('/api/auth/logout', { method: 'POST' });
    window.location.href = '/login';
}

// 响应式调整图表
window.addEventListener('resize', () => {
    echarts.getInstanceByDom(document.getElementById('ratingChart'))?.resize();
});