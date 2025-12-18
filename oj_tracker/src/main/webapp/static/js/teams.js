/**
 * static/js/teams.js
 */
document.addEventListener('DOMContentLoaded', () => {
    const platformSelect = document.getElementById('platformSelect');

    // 1. 绑定筛选事件
    if (platformSelect) {
        platformSelect.addEventListener('change', (e) => {
            fetchRankings(e.target.value);
        });
    }

    // 2. 核心修复：无论有没有下拉框，初始化时必须加载一次数据！
    const defaultPlatform = platformSelect ? platformSelect.value : 'CF';
    fetchRankings(defaultPlatform);
});

async function fetchRankings(platformCode) {
    try {
        // 你的接口：/api/team/rankings?platformCode=CF
        const res = await fetch(`/api/team/rankings?platformCode=${platformCode}`);

        // 打印日志，确认请求已发送 (调试用)
        console.log(`Fetching rankings for ${platformCode}... Status: ${res.status}`);

        const result = await res.json();

        if (result.code === 200) {
            renderRankTable(result.data);
        } else {
            document.getElementById('rankTableBody').innerHTML =
                `<tr><td colspan="5" class="text-center py-5 text-danger">${result.msg || '数据加载失败'}</td></tr>`;
        }
    } catch (e) {
        console.error("Fetch error:", e);
        document.getElementById('rankTableBody').innerHTML =
            '<tr><td colspan="5" class="text-center py-5 text-muted">网络连接异常，请检查后台服务</td></tr>';
    }
}

function renderRankTable(list) {
    const tbody = document.getElementById('rankTableBody');
    if (!list || list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center py-5 text-muted">暂无排名数据，请先绑定账号</td></tr>';
        return;
    }

    // 找到最新的更新时间
    if (list.length > 0) {
        const lastUpdate = list[0].snapshotTime || '';
        document.getElementById('updateTime').innerText = 'Data: ' + formatTime(lastUpdate);
    }

    tbody.innerHTML = list.map((user, index) => {
        const rank = index + 1;
        const colorClass = getRatingColorClass(user.rating); // 获取颜色类名

        return `
            <tr>
                <td class="ps-4 fw-bold text-secondary">${renderRankIcon(rank)}</td>
                <td>
                    <div class="fw-bold ${colorClass}" style="font-size: 1.05rem;">
                        ${user.nickname || 'Unknown'}
                    </div>
                    <small class="text-muted font-monospace">@${user.handle || '-'}</small>
                </td>
                <td class="text-muted small">${user.studentNo || '-'}</td>
                <td>
                    <span class="fw-bold ${colorClass}">${user.rating || 0}</span>
                </td>
                <td class="text-end pe-4 small text-muted">
                    ${formatTime(user.snapshotTime).split(' ')[0]}
                </td>
            </tr>
        `;
    }).join('');
}

/**
 * 渲染前三名奖杯图标
 */
function renderRankIcon(rank) {
    if (rank === 1) return '<i class="bi bi-trophy-fill text-warning" style="font-size: 1.2rem;"></i>'; // 金
    if (rank === 2) return '<i class="bi bi-trophy-fill text-secondary" style="font-size: 1.1rem;"></i>'; // 银
    if (rank === 3) return '<i class="bi bi-trophy-fill text-brown" style="color: #cd7f32; font-size: 1rem;"></i>'; // 铜
    return rank;
}

/**
 * Codeforces Rating 颜色映射逻辑
 * 配合 CSS 使用
 */
function getRatingColorClass(rating) {
    if (!rating) return 'text-secondary';
    if (rating < 1200) return 'text-secondary'; // Newbie (Gray)
    if (rating < 1400) return 'text-success';   // Pupil (Green)
    if (rating < 1600) return 'text-info';      // Specialist (Cyan)
    if (rating < 1900) return 'text-primary';   // Expert (Blue)
    if (rating < 2100) return 'text-purple';    // Candidate Master (Violet)
    if (rating < 2400) return 'text-warning';   // Master (Orange)
    return 'text-danger';                       // Grandmaster (Red)
}

function formatTime(isoStr) {
    if (!isoStr) return '-';
    return isoStr.replace('T', ' ');
}