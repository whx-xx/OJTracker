/**
 * static/js/ui.js
 * 依赖: Bootstrap 5, SweetAlert2
 */
const UI = {
    // --- 1. 交互组件 ---

    /**
     * 显示 Toast 提示
     * @param {string} message 内容
     * @param {string} type 'success' | 'danger' | 'warning' | 'info'
     */
    showToast: (message, type = 'success') => {
        let toastContainer = document.getElementById('toast-container');
        if (!toastContainer) {
            toastContainer = document.createElement('div');
            toastContainer.id = 'toast-container';
            toastContainer.className = 'toast-container position-fixed top-0 end-0 p-3';
            toastContainer.style.zIndex = 1055;
            document.body.appendChild(toastContainer);
        }

        const toastHtml = `
            <div class="toast align-items-center text-white bg-${type} border-0" role="alert" aria-live="assertive" aria-atomic="true">
                <div class="d-flex">
                    <div class="toast-body">
                        ${type === 'success' ? '<i class="bi bi-check-circle me-2"></i>' : ''}
                        ${type === 'danger' ? '<i class="bi bi-exclamation-circle me-2"></i>' : ''}
                        ${message}
                    </div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
                </div>
            </div>
        `;

        // 临时创建一个容器转换 HTML
        const tempDiv = document.createElement('div');
        tempDiv.innerHTML = toastHtml;
        const toastEl = tempDiv.firstElementChild;
        toastContainer.appendChild(toastEl);

        const toast = new bootstrap.Toast(toastEl, { delay: 3000 });
        toast.show();

        // 销毁
        toastEl.addEventListener('hidden.bs.toast', () => {
            toastEl.remove();
        });
    },

    /**
     * 确认弹窗 (Promise)
     * @param {string} title 标题
     * @param {string} text 内容
     * @returns {Promise<boolean>} confirmed
     */
    showConfirm: async (title, text) => {
        const result = await Swal.fire({
            title: title,
            text: text,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#3085d6',
            cancelButtonColor: '#d33',
            confirmButtonText: '确定',
            cancelButtonText: '取消'
        });
        return result.isConfirmed;
    },

    // --- 2. 格式化工具 ---

    formatDateTime: (isoString) => {
        if (!isoString) return '-';
        return new Date(isoString).toLocaleString('zh-CN', {
            year: 'numeric', month: '2-digit', day: '2-digit',
            hour: '2-digit', minute: '2-digit', second: '2-digit'
        });
    },

    formatDuration: (ms) => {
        if (!ms && ms !== 0) return '-';
        if (ms < 1000) return ms + 'ms';
        const sec = Math.floor(ms / 1000);
        if (sec < 60) return sec + 's';
        const min = Math.floor(sec / 60);
        return `${min}m ${sec % 60}s`;
    },

    safeText: (str) => {
        if (!str) return '';
        const div = document.createElement('div');
        div.innerText = str;
        return div.innerHTML;
    },

    // --- 3. 业务状态映射 ---

    mapUserStatus: (status) => {
        // status: 1=启用, 0=禁用 (或 true/false)
        const isActive = status === 1 || status === true;
        return isActive
            ? `<span class="badge bg-success bg-opacity-10 text-success">启用</span>`
            : `<span class="badge bg-secondary bg-opacity-10 text-secondary">禁用</span>`;
    },

    mapJobStatus: (status) => {
        switch (status) {
            case 'SUCCESS': return '<span class="badge bg-success">成功</span>';
            case 'FAIL': return '<span class="badge bg-danger">失败</span>';
            case 'RUNNING': return '<span class="badge bg-primary spinner-grow-sm">运行中</span>';
            default: return `<span class="badge bg-secondary">${status}</span>`;
        }
    },

    mapRole: (role) => {
        if (role === 'ADMIN') return '<span class="badge bg-danger">管理员</span>';
        return '<span class="badge bg-info text-dark">用户</span>';
    },

    // --- 4. 分页工具 ---

    /**
     * 渲染分页
     * @param {HTMLElement|string} container 容器或ID
     * @param {number} current 当前页
     * @param {number} size 页大小
     * @param {number} total 总条数
     * @param {function} onChange 回调函数(newPage)
     */
    renderPagination: (container, current, size, total, onChange) => {
        const target = typeof container === 'string' ? document.getElementById(container) : container;
        if (!target) return;

        const totalPages = Math.ceil(total / size);
        if (totalPages <= 1) {
            target.innerHTML = '';
            return;
        }

        let html = `<ul class="pagination justify-content-end mb-0">`;

        // 上一页
        html += `<li class="page-item ${current <= 1 ? 'disabled' : ''}">
                    <a class="page-link" href="#" data-page="${current - 1}">上一页</a>
                 </li>`;

        // 简单的页码逻辑：只显示 1..totalPages，如果太多可以后续优化为省略号模式
        // 这里实现一个简单的：总是显示第一页、最后一页、和当前页附近的页
        const showRange = 2; // 当前页前后各显示几页

        for (let i = 1; i <= totalPages; i++) {
            // 逻辑：是第一页 || 是最后一页 || 在当前页范围内
            if (i === 1 || i === totalPages || (i >= current - showRange && i <= current + showRange)) {
                const active = i === current ? 'active' : '';
                html += `<li class="page-item ${active}"><a class="page-link" href="#" data-page="${i}">${i}</a></li>`;
            } else if (i === current - showRange - 1 || i === current + showRange + 1) {
                html += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
            }
        }

        // 下一页
        html += `<li class="page-item ${current >= totalPages ? 'disabled' : ''}">
                    <a class="page-link" href="#" data-page="${current + 1}">下一页</a>
                 </li>`;

        html += `</ul>`;
        target.innerHTML = html;

        // 绑定事件
        target.querySelectorAll('a.page-link').forEach(a => {
            a.addEventListener('click', (e) => {
                e.preventDefault();
                const page = parseInt(a.getAttribute('data-page'));
                if (!isNaN(page) && page !== current && page > 0 && page <= totalPages) {
                    onChange(page);
                }
            });
        });
    }
};