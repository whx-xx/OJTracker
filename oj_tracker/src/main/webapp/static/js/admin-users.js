/**
 * static/js/admin-users.js
 */
let currentPage = 1;
const pageSize = 8;
let currentKeyword = "";

document.addEventListener('DOMContentLoaded', () => {
    fetchUsers(1);
    updateStats();
    // 绑定回车搜索
    document.getElementById('userSearch').addEventListener('keypress', (e) => {
        if (e.key === 'Enter') handleSearch();
    });
});

/**
 * 搜索处理
 */
function handleSearch() {
    currentKeyword = document.getElementById('userSearch').value;
    fetchUsers(1); // 搜索时重置回第一页
}

/**
 * 核心：获取用户数据
 */
async function fetchUsers(page) {
    currentPage = page;
    const url = `/api/admin/users?page=${page}&pageSize=${pageSize}&keyword=${encodeURIComponent(currentKeyword)}`;

    try {
        const res = await fetch(url);
        const result = await res.json();

        if (result.code === 200) {
            const pageData = result.data;
            renderTable(pageData.list);
            renderPagination(pageData.total, page, pageSize);

            // 这里只更新分页上方的“共 X 条记录”文字，不调用 updateStats
            document.getElementById('totalCount').innerText = pageData.total;
        }
    } catch (e) {
        showToast('error', '加载失败');
    }
}

/**
 * 渲染用户表格
 */
function renderTable(users) {
    const tbody = document.getElementById('userTableBody');
    if (!users || users.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center py-5 text-muted">暂无数据</td></tr>';
        return;
    }

    tbody.innerHTML = users.map(user => {
        const isNormal = user.status === 1;
        return `
            <tr>
                <td class="ps-4"><code class="text-muted small">#${user.id}</code></td>
                <td>
                    <div class="fw-bold text-dark">${user.nickname || '未设置昵称'}</div>
                    <div class="text-muted extra-small">@${user.username} | ID: ${user.studentNo || 'N/A'}</div>
                </td>
                <td><span class="badge ${user.role === 'ADMIN' ? 'bg-dark' : 'bg-light text-dark border'}">${user.role}</span></td>
                <td>
                    <span class="badge ${isNormal ? 'bg-success-subtle text-success' : 'bg-danger-subtle text-danger'} px-3">
                        ${isNormal ? '正常' : '已禁用'}
                    </span>
                </td>
                <td class="text-end pe-4">
                    <div class="btn-group">
                        <button class="btn btn-sm ${isNormal ? 'btn-outline-danger' : 'btn-outline-success'}"
                                onclick="updateStatus(${user.id}, ${user.status})">
                            <i class="bi ${isNormal ? 'bi-slash-circle' : 'bi-check-circle'}"></i>
                            <sapn>${isNormal ? '禁用' : '启用'}</sapn>
                        </button>
                        <button class="btn btn-sm btn-outline-secondary" onclick="resetPwd(${user.id})">
                            <i class="bi bi-key"></i>
                            <span class="small">重置密码</span>
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

/**
 * 渲染分页按钮核心逻辑
 */
function renderPagination(total, page, size) {
    const totalPages = Math.ceil(total / size);
    const container = document.getElementById('pagination');
    document.getElementById('totalCount').innerText = total;
    document.getElementById('currentPageNum').innerText = page;

    if (totalPages <= 1) {
        container.innerHTML = ''; // 只有一页时不显示分页栏
        return;
    }

    let html = '';

    // 上一页
    html += `
        <li class="page-item ${page === 1 ? 'disabled' : ''}">
            <a class="page-link" href="javascript:void(0)" onclick="fetchUsers(${page - 1})">
                <i class="bi bi-chevron-left"></i>
            </a>
        </li>`;

    // 页码数字
    for (let i = 1; i <= totalPages; i++) {
        // 如果页数很多，这里可以做省略号逻辑，目前先显示全部
        html += `
            <li class="page-item ${i === page ? 'active' : ''}">
                <a class="page-link" href="javascript:void(0)" onclick="fetchUsers(${i})">${i}</a>
            </li>`;
    }

    // 下一页
    html += `
        <li class="page-item ${page === totalPages ? 'disabled' : ''}">
            <a class="page-link" href="javascript:void(0)" onclick="fetchUsers(${page + 1})">
                <i class="bi bi-chevron-right"></i>
            </a>
        </li>`;

    container.innerHTML = html;
}

/**
 * 更新统计卡片数字
 */
async function updateStats() {
    try {
        const res = await fetch('/api/admin/users/stats');
        const result = await res.json();
        if (result.code === 200) {
            const stats = result.data;
            // 对应你 HTML 中的 4 个 ID
            document.getElementById('statTotalUsers').textContent = stats.totalUsers;
            document.getElementById('statAdmins').textContent = stats.adminCount;
            document.getElementById('statActive').textContent = stats.activeCount;
            document.getElementById('statBanned').textContent = stats.bannedCount;
        }
    } catch (e) {
        console.error("加载统计数据失败:", e);
    }
}

/**
 * 对应 PUT /api/admin/users/{id}/status
 */
async function updateStatus(userId, currentStatus) {
    const newStatus = currentStatus === 1 ? 0 : 1;
    const { isConfirmed } = await Swal.fire({
        title: '确认变更状态?',
        icon: 'warning',
        showCancelButton: true,
        confirmButtonText: '确定',
        cancelButtonText: '取消'
    });

    if (isConfirmed) {
            const res = await fetch(`/api/admin/users/${userId}/status`, {
                method: 'PUT',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({ status: newStatus })
            });
            if ((await res.json()).code === 200) {
                showToast('success', '状态已更新');
                fetchUsers(currentPage); // 刷新列表
                updateStats();           // 关键：同步刷新顶部的统计卡片数字
            }
    }
}

/**
 * 对应 POST /api/admin/users/{id}/reset-password
 */
async function resetPwd(userId) {
    const { isConfirmed } = await Swal.fire({
        title: '重置密码?',
        text: '密码将恢复为默认设置',
        icon: 'warning',
        showCancelButton: true,
        confirmButtonText: '确定',
        cancelButtonText: '取消'
    });

    if (isConfirmed) {
        const res = await fetch(`/api/admin/users/${userId}/reset-password`, { method: 'POST' });
        if ((await res.json()).code === 200) {
            Swal.fire('已重置', '该用户密码已初始化', 'success');
        }
    }
}