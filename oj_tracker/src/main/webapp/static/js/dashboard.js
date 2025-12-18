document.addEventListener('DOMContentLoaded', function() {
    fetchStats();
    fetchPlatforms();
    renderRatingChart();
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

window.addEventListener('resize', () => {
    echarts.getInstanceByDom(document.getElementById('ratingChart'))?.resize();
});