document.addEventListener('DOMContentLoaded', function() {
    console.log("Dashboard initializing...");
    initDashboard();
});

function initDashboard() {
    fetchStats();
    fetchPlatforms();
    renderRatingChart();
    renderHeatmap();
    fetchWeeklyProblems();
    fetchRecentSubmissions();

    window.addEventListener('resize', () => {
        const rChart = echarts.getInstanceByDom(document.getElementById('ratingChart'));
        const hChart = echarts.getInstanceByDom(document.getElementById('heatmapChart'));
        if (rChart) rChart.resize();
        if (hChart) hChart.resize();
    });
}

// 1. 基础统计
async function fetchStats() {
    try {
        const res = await fetch('/api/users/summary?platformCode=CF');
        const data = await res.json();
        if (data.code === 200 && data.data) {
            document.getElementById('totalSolved').innerText = data.data.solvedTotal || 0;
            document.getElementById('weeklySolved').innerText = data.data.weeklySolved || 0;
            document.getElementById('activeDays').innerText = data.data.activeDays || 0;
        }
    } catch (e) { console.error("Stats error", e); }
}

// 2. 平台列表
async function fetchPlatforms() {
    try {
        const res = await fetch('/api/me/platforms');
        const data = await res.json();
        const list = document.getElementById('platformList');

        if (data.code === 200) {
            const platforms = data.data || [];
            document.getElementById('platformCount').innerText = platforms.length;

            if (platforms.length === 0) {
                list.innerHTML = `<li class="list-group-item text-center text-muted py-4 border-0">暂无绑定账号</li>`;
                return;
            }

            list.innerHTML = platforms.map(p => `
                <li class="list-group-item d-flex justify-content-between align-items-center px-4 py-3 border-bottom-0">
                    <div class="d-flex align-items-center">
                        <div class="rounded-circle bg-light d-flex justify-content-center align-items-center me-3 text-secondary fw-bold"
                             style="width: 40px; height: 40px;">
                             ${(p.platformName || 'P').substring(0,1)}
                        </div>
                        <div>
                            <div class="fw-bold text-dark">${p.platformName}</div>
                            <div class="small text-muted font-monospace">${p.identifierValue}</div>
                        </div>
                    </div>
                    ${p.verified
                        ? '<span class="badge bg-success-subtle text-success rounded-pill">Verified</span>'
                        : '<span class="badge bg-warning-subtle text-warning rounded-pill">Syncing</span>'}
                </li>
            `).join('');
        }
    } catch (e) { console.error("Platform error", e); }
}

// 3. 本周题目
async function fetchWeeklyProblems() {
    const container = document.getElementById('weeklyProblemList');
    try {
        const res = await fetch('/api/user/problems/week?platformCode=CF');
        const result = await res.json();

        if (result.code === 200) {
            const list = result.data || [];
            document.getElementById('weeklyCountBadge').innerText = list.length;

            if (list.length === 0) {
                container.innerHTML = `<div class="text-center py-5 text-muted small">本周暂无 AC 记录</div>`;
                return;
            }

            container.innerHTML = list.map(p => `
                <a href="${p.problemUrl}" target="_blank" class="list-group-item list-group-item-action px-4 py-3 border-0 border-bottom">
                    <div class="d-flex w-100 justify-content-between align-items-center">
                        <div class="text-truncate me-3">
                            <span class="fw-bold text-primary me-2">${p.problemIndex}</span>
                            <span class="text-dark">${p.problemName}</span>
                        </div>
                        <small class="text-muted text-nowrap">${formatTime(p.submitTime)}</small>
                    </div>
                </a>
            `).join('');
        }
    } catch (e) {
        container.innerHTML = `<div class="text-center py-3 text-danger small">加载失败</div>`;
    }
}

// 4. 最近提交 Timeline
async function fetchRecentSubmissions() {
    const container = document.getElementById('submissionTimeline');
    try {
        const res = await fetch('/api/user/submissions/timeline?platformCode=CF&range=WEEK&limit=20');
        const result = await res.json();

        if (result.code === 200) {
            const list = result.data || [];
            if (list.length === 0) {
                container.innerHTML = `<div class="text-center py-5 text-muted small">暂无最近提交记录</div>`;
                return;
            }

            container.innerHTML = list.map(sub => {
                const isOk = sub.verdict === 'OK';
                let dotClass = 'dot-warning';
                let badgeText = sub.verdict;
                let textColor = 'text-warning';

                if (isOk) {
                    dotClass = 'dot-success';
                    badgeText = 'AC';
                    textColor = 'text-success';
                } else if (badgeText === 'WRONG_ANSWER') {
                    dotClass = 'dot-danger';
                    badgeText = 'WA';
                    textColor = 'text-danger';
                } else if (badgeText === 'TIME_LIMIT_EXCEEDED') {
                    badgeText = 'TLE';
                }

                return `
                <div class="timeline-item">
                    <div class="timeline-dot ${dotClass}"></div>
                    <div class="d-flex justify-content-between align-items-start">
                        <div class="me-2 overflow-hidden">
                            <div class="fw-bold text-dark text-truncate" style="max-width: 200px;">
                                ${sub.problemIndex}. ${sub.problemName}
                            </div>
                            <div class="small text-muted">
                                <span class="fw-bold ${textColor} me-2">${badgeText}</span>
                                <span class="font-monospace">#${sub.submissionId}</span>
                            </div>
                        </div>
                        <small class="text-muted text-nowrap" style="font-size: 0.75rem;">
                            ${formatTime(sub.submitTime)}
                        </small>
                    </div>
                </div>
                `;
            }).join('');
        }
    } catch (e) {
        container.innerHTML = `<div class="text-center py-3 text-danger small">Timeline 加载失败</div>`;
    }
}

/**
 * 仿 Codeforces 风格的 Rating History
 */
/**
 * 精准匹配 Codeforces 官方颜色的 Rating History
 */
async function renderRatingChart() {
    const chartDom = document.getElementById('ratingChart');
    if (!chartDom) return;
    const chart = echarts.init(chartDom);
    chart.showLoading();

    try {
        const res = await fetch('/api/user/rating/history?platformCode=CF');
        const result = await res.json();
        chart.hideLoading();

        if (result.code === 200) {
            const list = result.data || [];
            const dates = list.map(i => i.time.split('T')[0]);
            const ratings = list.map(i => i.rating);

            // 动态计算 Y 轴范围
            const minRating = Math.min(...ratings, 1000) - 150;
            const maxRating = Math.max(...ratings, 2000) + 150;

            const option = {
                backgroundColor: '#fff',
                tooltip: {
                    trigger: 'axis',
                    backgroundColor: 'rgba(255, 255, 255, 0.96)',
                    borderColor: '#ccc',
                    borderWidth: 1,
                    padding: 10,
                    textStyle: { color: '#000' },
                    formatter: function (params) {
                        const item = list[params[0].dataIndex];
                        const delta = item.delta || 0;
                        const deltaStr = delta > 0 ? `<span style="color:green;">+${delta}</span>` : `<span style="color:red;">${delta}</span>`;
                        return `<div style="font-weight:bold;margin-bottom:5px;">${item.contestName}</div>
                                <div style="font-size:12px;">Rank: <b>${item.rank || 'N/A'}</b></div>
                                <div style="font-size:12px;">Rating: <b>${item.rating}</b> (${deltaStr})</div>
                                <div style="font-size:11px;color:#666;margin-top:5px;">${item.time.replace('T', ' ')}</div>`;
                    }
                },
                grid: {
                    left: '45',
                    right: '20',
                    top: '20',
                    bottom: '40'
                },
                xAxis: {
                    type: 'category',
                    data: dates,
                    boundaryGap: true,
                    axisLine: { lineStyle: { color: '#000', width: 1 } },
                    axisLabel: { color: '#000', fontSize: 11, margin: 15 }
                },
                yAxis: {
                    type: 'value',
                    min: Math.floor(minRating / 100) * 100,
                    max: Math.ceil(maxRating / 100) * 100,
                    interval: 200,
                    axisLine: { show: true, lineStyle: { color: '#000' } },
                    axisLabel: { color: '#000', fontSize: 11 },
                    splitLine: { show: false } // 禁用网格线，使用背景块区分
                },
                series: [{
                    data: ratings,
                    type: 'line',
                    symbol: 'circle',
                    symbolSize: 7,
                    connectNulls: true,
                    lineStyle: { color: '#333', width: 2 },
                    itemStyle: { color: '#fff', borderColor: '#333', borderWidth: 2 },
                    // 使用 markArea 绘制背景段位
                    markArea: {
                        silent: true,
                        data: [
                            [{ yAxis: 0, itemStyle: { color: '#ccc', opacity: 0.4 } }, { yAxis: 1200 }],     // Newbie
                            [{ yAxis: 1200, itemStyle: { color: '#8f8', opacity: 0.4 } }, { yAxis: 1400 }],  // Pupil
                            [{ yAxis: 1400, itemStyle: { color: '#aaf', opacity: 0.4 } }, { yAxis: 1600 }],  // Specialist (CF 原版偏蓝)
                            [{ yAxis: 1600, itemStyle: { color: '#f8f', opacity: 0.4 } }, { yAxis: 1900 }],  // Expert (CF 原版偏粉/紫)
                            [{ yAxis: 1900, itemStyle: { color: '#fbb', opacity: 0.4 } }, { yAxis: 2100 }],  // Candidate Master
                            [{ yAxis: 2100, itemStyle: { color: '#ffcc88', opacity: 0.4 } }, { yAxis: 2300 }], // Master
                            [{ yAxis: 2300, itemStyle: { color: '#ffbb55', opacity: 0.4 } }, { yAxis: 2400 }], // International Master
                            [{ yAxis: 2400, itemStyle: { color: '#f77', opacity: 0.4 } }, { yAxis: 2600 }],  // Grandmaster
                            [{ yAxis: 2600, itemStyle: { color: '#f33', opacity: 0.4 } }, { yAxis: 3000 }],  // Int. Grandmaster
                            [{ yAxis: 3000, itemStyle: { color: '#a00', opacity: 0.4 } }, { yAxis: 5000 }]   // Legendary GM
                        ]
                    }
                }]
            };
            chart.setOption(option);
        }
    } catch (e) {
        chart.hideLoading();
        console.error("Rating Chart error", e);
    }
}

async function renderHeatmap() {
    const chartDom = document.getElementById('heatmapChart');
    if (!chartDom) return;
    const chart = echarts.init(chartDom);

    try {
        const res = await fetch('/api/user/activity/heatmap?platformCode=CF&days=365');
        const result = await res.json();

        if (result.code === 200) {
            const data = (result.data || []).map(item => [item.day, item.submitCnt]);
            const today = new Date();
            const oneYearAgo = new Date();
            oneYearAgo.setDate(today.getDate() - 364);

            const option = {
                tooltip: {
                    position: 'top',
                    formatter: (p) => `${p.data[0]} : ${p.data[1]} 次提交`
                },
                visualMap: {
                    min: 0,
                    max: 10,
                    type: 'piecewise',
                    orient: 'horizontal',
                    left: 'center',
                    bottom: 10,        // 稍微向上提一点
                    itemGap: 10,
                    itemWidth: 14,
                    itemHeight: 14,
                    textStyle: { color: '#666' },
                    pieces: [
                        { min: 0, max: 0, color: '#ebedf0', label: '0' },
                        { min: 1, max: 2, color: '#9be9a8', label: '' },
                        { min: 3, max: 5, color: '#40c463', label: '' },
                        { min: 6, max: 9, color: '#30a14e', label: '' },
                        { min: 10, color: '#216e39', label: '10+' }
                    ]
                },
                calendar: {
                    top: 35,           // 增加顶部间距，防止月份标签太挤
                    left: 50,
                    right: 20,
                    // [关键修改] cellSize 从 13 调大到 16，消除“扁”的感觉
                    cellSize: ['auto', 16],
                    range: [
                        oneYearAgo.toISOString().split('T')[0],
                        today.toISOString().split('T')[0]
                    ],
                    itemStyle: {
                        borderWidth: 3,
                        borderColor: '#fff'
                    },
                    splitLine: { show: false },
                    yearLabel: { show: false },
                    dayLabel: {
                        nameMap: ['Sun', '', 'Tue', '', 'Thu', '', 'Sat'],
                        color: '#999',
                        fontSize: 11,
                        firstDay: 0    // 确保从周日开始
                    },
                    monthLabel: {
                        nameMap: 'en',
                        color: '#666',
                        fontSize: 12,
                        margin: 10     // 月份文字离方块远一点
                    }
                },
                series: {
                    type: 'heatmap',
                    coordinateSystem: 'calendar',
                    data: data,
                    itemStyle: {
                        borderRadius: 3 // 圆角稍微明显一点更美观
                    }
                }
            };
            chart.setOption(option);
        }
    } catch (e) {
        console.error("Heatmap rendering failed:", e);
    }
}

function formatTime(isoStr) {
    if (!isoStr) return '';
    const date = new Date(isoStr);
    const now = new Date();
    if (date.toDateString() === now.toDateString()) {
        return date.toLocaleTimeString('zh-CN', {hour: '2-digit', minute:'2-digit'});
    }
    return (date.getMonth() + 1) + '-' + date.getDate();
}