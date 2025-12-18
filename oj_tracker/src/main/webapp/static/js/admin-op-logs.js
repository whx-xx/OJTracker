/**
 * static/js/admin-op-logs.js
 */
let currentPage = 1;
const pageSize = 8;

document.addEventListener('DOMContentLoaded', () => {
    fetchLogs(1);
});

/**
 * 获取日志数据
 * API: GET /api/admin/op-logs?page=1&pageSize=15&opType=...
 */
async function fetchLogs(page) {
    currentPage = page;
    const opType = document.getElementById('opTypeFilter').value;
    const params = new URLSearchParams({
        page: page,
        pageSize: pageSize,
        opType: opType || ''
    });

    try {
        const res = await fetch(`/api/admin/op-logs?${params.toString()}`);
        const result = await res.json(); // Result<PageResult<AdminOpLogVO>>

        if (result.code === 200) {
            const pageData = result.data;
            renderTable(pageData.list);
            renderPagination(pageData.total, page, pageSize);
            document.getElementById('totalCount').innerText = pageData.total;
        } else {
            showToast('error', result.msg || '加载失败');
        }
    } catch (e) {
        console.error(e);
        showToast('error', '网络请求异常');
    }
}

function renderTable(list) {
    const tbody = document.getElementById('logTableBody');
    if (!list || list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center py-5 text-muted">暂无操作记录</td></tr>';
        return;
    }

    tbody.innerHTML = list.map(log => `
        <tr>
            <td class="ps-4 text-muted small">#${log.id}</td>
            <td>
                <div class="fw-bold text-dark small">${log.adminName || 'Unknown'}</div>
            </td>
            <td>${renderOpTypeBadge(log.opType)}</td>
            <td>
                ${log.targetUserId ?
                    `<span class="badge bg-light text-dark border pointer" title="ID: ${log.targetUserId}">
                        ${log.targetName || log.targetUserId}
                     </span>` :
                    '<span class="text-muted small">-</span>'}
            </td>
            <td class="small text-muted font-monospace">
                            ${formatIp(log.ip)}
            </td>
            <td class="small text-muted">${formatTime(log.opTime)}</td>
            <td class="pe-4 small text-secondary text-truncate" style="max-width: 200px;" title="${log.remark || ''}">
                ${log.remark || '-'}
            </td>
        </tr>
    `).join('');
}

function formatIp(ip) {
    if (!ip) return '-';
    if (ip === '0:0:0:0:0:0:0:1') return 'localhost'; // 或者显示 '127.0.0.1'
    return ip;
}

function renderOpTypeBadge(type) {
    let color = 'secondary';
    // 根据类型显示不同颜色，提升辨识度
    if (type.includes('UPDATE')) color = 'info';
    else if (type.includes('RESET')) color = 'warning';
    else if (type.includes('SYNC')) color = 'primary';
    else if (type.includes('DELETE') || type.includes('BAN')) color = 'danger';

    return `<span class="badge bg-${color}-subtle text-${color} border border-${color}-subtle">${type}</span>`;
}

function formatTime(isoStr) {
    if (!isoStr) return '-';
    return isoStr.replace('T', ' ');
}

// 分页渲染 (复用逻辑)
function renderPagination(total, page, size) {
    const totalPages = Math.ceil(total / size);
    const container = document.getElementById('pagination');
    if (totalPages <= 1) { container.innerHTML = ''; return; }

    let html = '';
    // 简单的分页逻辑：上一页 + 数字 + 下一页
    const prevDisabled = page === 1 ? 'disabled' : '';
    html += `<li class="page-item ${prevDisabled}"><a class="page-link" href="javascript:void(0)" onclick="fetchLogs(${page - 1})">&laquo;</a></li>`;

    // 仅显示当前页附近的页码（简化版）
    for (let i = 1; i <= totalPages; i++) {
        if (i === 1 || i === totalPages || (i >= page - 2 && i <= page + 2)) {
            html += `<li class="page-item ${i === page ? 'active' : ''}"><a class="page-link" href="javascript:void(0)" onclick="fetchLogs(${i})">${i}</a></li>`;
        } else if (i === page - 3 || i === page + 3) {
            html += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
        }
    }

    const nextDisabled = page === totalPages ? 'disabled' : '';
    html += `<li class="page-item ${nextDisabled}"><a class="page-link" href="javascript:void(0)" onclick="fetchLogs(${page + 1})">&raquo;</a></li>`;

    container.innerHTML = html;
}