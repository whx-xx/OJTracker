/**
 * static/js/admin-sync.js
 */
let currentPage = 1;
const pageSize = 8;

document.addEventListener('DOMContentLoaded', () => {
    loadOverview();
    fetchJobs(1);
    initScheduleStatus();
});

/**
 * 初始化调度开关状态 [接口: GET /api/admin/schedule/status]
 */
async function initScheduleStatus() {
    try {
        const res = await fetch('/api/admin/schedule/status');
        const r = await res.json();
        if (r.code === 200) {
            // 将后端传回的 boolean 值同步到 HTML 开关上
            document.getElementById('scheduleSwitch').checked = r.data;
        }
    } catch (e) {
        console.error("无法获取调度状态", e);
    }
}

/**
 * 切换调度开关 [接口: POST /api/admin/schedule/enable]
 */
async function toggleSchedule(enabled) {
    try {
        const res = await fetch(`/api/admin/schedule/enable?enabled=${enabled}`, {
            method: 'POST'
        });
        const r = await res.json();
        if (r.code === 200) {
            showToast('success', `自动调度已${enabled ? '启用' : '禁用'}`);
        } else {
            // 如果后端处理失败，恢复开关的原始视觉状态
            document.getElementById('scheduleSwitch').checked = !enabled;
            Swal.fire('失败', r.msg, 'error');
        }
    } catch (e) {
        document.getElementById('scheduleSwitch').checked = !enabled;
        showToast('error', '网络请求失败');
    }
}

/**
 * 加载概览数据 [接口: GET /api/admin/sync/overview]
 */
async function loadOverview() {
    try {
        const res = await fetch('/api/admin/sync/overview');
        const r = await res.json();
        if (r.code === 200) {
            const d = r.data;
            // 渲染积分同步状态
            if (d.latestRating) {
                document.getElementById('latestRatingTime').innerText = d.latestRating.startTime.replace('T', ' ');
                const badge = document.getElementById('latestRatingStatus');
                badge.innerText = d.latestRating.status;
                badge.className = `badge rounded-pill mx-auto mt-1 ${d.latestRating.status === 'SUCCESS' ? 'bg-success' : 'bg-danger'}`;
            }
            // 渲染每日解题同步状态
            if (d.latestDaily) {
                document.getElementById('latestDailyTime').innerText = d.latestDaily.startTime.replace('T', ' ');
                const badge = document.getElementById('latestDailyStatus');
                badge.innerText = d.latestDaily.status;
                badge.className = `badge rounded-pill mx-auto mt-1 ${d.latestDaily.status === 'SUCCESS' ? 'bg-success' : 'bg-danger'}`;
            }
        }
    } catch (e) { console.error("加载概览失败", e); }
}

/**
 * 分页获取作业列表 [接口: GET /api/admin/sync/jobs]
 */
async function fetchJobs(page) {
    currentPage = page;
    try {
        const res = await fetch(`/api/admin/sync/jobs?page=${page}&pageSize=${pageSize}`);
        const r = await res.json();
        if (r.code === 200) {
            renderTable(r.data.list);
            renderPagination(r.data.total, page, pageSize);
            document.getElementById('totalJobs').innerText = r.data.total;
        }
    } catch (e) { showToast('error', '加载作业列表失败'); }
}

function renderTable(list) {
    const tbody = document.getElementById('jobTableBody');
    if (!list || list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center py-4">暂无作业记录</td></tr>';
        return;
    }
    tbody.innerHTML = list.map(job => `
        <tr>
            <td class="ps-4 text-muted small">#${job.id}</td>
            <td><span class="fw-bold">${job.jobType}</span></td>
            <td><span class="badge ${job.status === 'SUCCESS' ? 'bg-success-subtle text-success' : 'bg-warning-subtle text-warning'}">${job.status}</span></td>
            <td class="small text-muted">${job.startTime.replace('T', ' ')}</td>
            <td>
                <span class="text-success">${job.successCount}</span> / <span class="text-dark">${job.totalCount}</span>
                ${job.failCount > 0 ? `<span class="text-danger ms-1">(失败 ${job.failCount})</span>` : ''}
            </td>
            <td class="text-muted small">${job.durationMs || '-'}</td>
            <td class="text-end pe-4">
                <button class="btn btn-sm btn-outline-secondary" onclick="viewDetail(${job.id})">
                    <i class="bi bi-eye"></i> 详情
                </button>
            </td>
        </tr>
    `).join('');
}

/**
 * 触发新任务 [接口: POST /api/admin/sync/run]
 */
async function triggerSync(type) {
    const { isConfirmed } = await Swal.fire({
        title: '确认手动触发?',
        text: `系统将立即开始执行 ${type} 同步任务`,
        icon: 'question',
        showCancelButton: true,
        confirmButtonText: '确定',
        cancelButtonText: '取消'
    });
    if (!isConfirmed) return;

    const res = await fetch(`/api/admin/sync/run?jobType=${type}&days=3`, { method: 'POST' });
    const r = await res.json();
    if (r.code === 200) {
        showToast('success', `同步作业已提交，ID: ${r.data}`);
        fetchJobs(1);
    } else {
        Swal.fire('提交失败', r.msg, 'error');
    }
}

/**
 * 作业详情 [接口: GET /api/admin/sync/jobs/{jobId}]
 */
async function viewDetail(jobId) {
    const res = await fetch(`/api/admin/sync/jobs/${jobId}`);
    const r = await res.json();
    if (r.code !== 200) return;

    const data = r.data;
    document.getElementById('currentJobId').innerText = `#${jobId}`;
    const tbody = document.getElementById('failListBody');

    if (!data.failList || data.failList.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center py-4 text-success">本任务无失败用户记录</td></tr>';
    } else {
        tbody.innerHTML = data.failList.map(f => `
            <tr>
                <td class="ps-3">
                    <div class="fw-bold">${f.nickname}</div>
                    <div class="extra-small text-muted">${f.studentNo}</div>
                </td>
                <td><span class="badge bg-light text-dark border">${f.platformCode}</span></td>
                <td><code class="text-danger">${f.errorCode}</code></td>
                <td class="small">${f.errorMessage}</td>
                <td class="text-end pe-3 small text-primary">${f.suggestAction}</td>
            </tr>
        `).join('');
    }

    // 重跑按钮逻辑 [接口: POST /api/admin/sync/rerun]
    const rerunBtn = document.getElementById('rerunBtn');
    rerunBtn.onclick = async () => {
        const rerunRes = await fetch(`/api/admin/sync/rerun?jobId=${jobId}`, { method: 'POST' });
        const rerunR = await rerunRes.json();
        if (rerunR.code === 200) {
            bootstrap.Modal.getInstance(document.getElementById('detailModal')).hide();
            showToast('success', '重跑任务已提交');
            fetchJobs(1);
        }
    };

    new bootstrap.Modal(document.getElementById('detailModal')).show();
}

// 辅助：分页渲染 (复用之前的逻辑)
function renderPagination(total, page, size) {
    const totalPages = Math.ceil(total / size);
    const container = document.getElementById('pagination');
    if (totalPages <= 1) { container.innerHTML = ''; return; }
    let html = '';
    for (let i = 1; i <= totalPages; i++) {
        html += `<li class="page-item ${i === page ? 'active' : ''}"><a class="page-link" href="javascript:void(0)" onclick="fetchJobs(${i})">${i}</a></li>`;
    }
    container.innerHTML = html;
}