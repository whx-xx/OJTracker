document.addEventListener('DOMContentLoaded', function() {
    const platformSelect = document.getElementById('platformSelect');
    if (platformSelect) {
        fetchRankings(platformSelect.value);
        platformSelect.addEventListener('change', function() {
            fetchRankings(this.value);
        });
    }
});

async function fetchRankings(platformCode) {
    const tableBody = document.getElementById('rankingTableBody');
    try {
        const response = await fetch(`/api/team/rankings?platformCode=${platformCode}`);
        const result = await response.json();

        if (result.code === 200) {
            renderTable(result.data);
        } else {
            tableBody.innerHTML = `<tr><td colspan="5" class="text-center text-danger py-4">${result.msg}</td></tr>`;
        }
    } catch (error) {
        tableBody.innerHTML = `<tr><td colspan="5" class="text-center text-danger py-4">无法连接到服务器</td></tr>`;
    }
}

function renderTable(rankings) {
    const tableBody = document.getElementById('rankingTableBody');
    if (!rankings || rankings.length === 0) {
        tableBody.innerHTML = `<tr><td colspan="5" class="text-center py-4 text-muted">暂无数据</td></tr>`;
        return;
    }

    tableBody.innerHTML = rankings.map((item, index) => {
        let badgeClass = index === 0 ? 'bg-warning text-dark' : (index === 1 ? 'bg-light text-dark border' : (index === 2 ? 'bg-danger' : 'bg-secondary'));

        // 增强的日期处理：支持 ISO 字符串和数字数组
        let dateStr = '从未同步';
        if (item.snapshotTime) {
            const d = Array.isArray(item.snapshotTime)
                ? new Date(item.snapshotTime[0], item.snapshotTime[1]-1, item.snapshotTime[2], item.snapshotTime[3], item.snapshotTime[4])
                : new Date(item.snapshotTime);
            dateStr = d.toLocaleString();
        }

        return `
            <tr>
                <td class="ps-4">
                    <span class="badge rounded-circle p-2 ${badgeClass}" style="width: 30px; height: 30px; display: inline-flex; align-items: center; justify-content: center;">
                        ${index + 1}
                    </span>
                </td>
                <td class="fw-bold">${item.nickname}</td>
                <td><code class="text-muted small">${item.studentNo || 'N/A'}</code></td>
                <td class="fw-bold text-primary">${item.rating || 0}</td>
                <td class="text-end pe-4 small text-muted">${dateStr}</td>
            </tr>
        `;
    }).join('');
}