document.addEventListener('DOMContentLoaded', function() {
    const platformSelect = document.getElementById('platformSelect');

    // 页面加载时执行一次
    fetchRankings(platformSelect.value);

    // 切换平台时重新加载
    platformSelect.addEventListener('change', function() {
        fetchRankings(this.value);
    });
});

async function fetchRankings(platformCode) {
    const tableBody = document.getElementById('rankingTableBody');

    try {
        // 调用后端接口
        const response = await fetch(`/api/team/rankings?platformCode=${platformCode}`);
        const result = await response.json();

        // 记得我们已经统一了 code 200 为成功
        if (result.code === 200) {
            renderTable(result.data);
        } else {
            tableBody.innerHTML = `<tr><td colspan="5" class="text-center text-danger py-4">${result.msg || '获取排名失败'}</td></tr>`;
        }
    } catch (error) {
        console.error("Fetch error:", error);
        tableBody.innerHTML = `<tr><td colspan="5" class="text-center text-danger py-4">服务器连接异常</td></tr>`;
    }
}

function renderTable(rankings) {
    const tableBody = document.getElementById('rankingTableBody');

    if (!rankings || rankings.length === 0) {
        tableBody.innerHTML = `<tr><td colspan="5" class="text-center py-4 text-muted">该平台暂无排名数据</td></tr>`;
        return;
    }

    tableBody.innerHTML = rankings.map((item, index) => {
        // 简单的奖牌颜色逻辑
        let badgeClass = 'bg-secondary';
        if (index === 0) badgeClass = 'bg-warning text-dark'; // 金牌
        if (index === 1) badgeClass = 'bg-light text-dark border'; // 银牌
        if (index === 2) badgeClass = 'bg-danger'; // 铜牌

        // 格式化时间 (item.snapshotTime)
        const date = item.snapshotTime ? new Date(item.snapshotTime).toLocaleString() : '从未同步';

        return `
            <tr>
                <td class="ps-4">
                    <span class="badge rounded-circle p-2 ${badgeClass}" style="width: 30px; height: 30px; display: inline-flex; align-items: center; justify-content: center;">
                        ${index + 1}
                    </span>
                </td>
                <td>
                    <div class="fw-bold text-dark">${item.nickname}</div>
                </td>
                <td><code class="text-muted small">${item.studentNo || 'N/A'}</code></td>
                <td>
                    <span class="fw-bold ${item.rating >= 1900 ? 'text-danger' : 'text-primary'}">
                        ${item.rating || 0}
                    </span>
                </td>
                <td class="text-end pe-4 small text-muted">${date}</td>
            </tr>
        `;
    }).join('');
}