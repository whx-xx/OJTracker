// 全局工具与初始化
const App = {
    // 侧边栏切换
    toggleSidebar: function() {
        document.body.classList.toggle('sb-sidenav-toggled');
        document.getElementById('wrapper').classList.toggle('toggled');
    },

    // 简单的 Toast 封装
    toast: function(title, icon = 'success') {
        const Toast = Swal.mixin({
            toast: true,
            position: 'top-end',
            showConfirmButton: false,
            timer: 3000,
            timerProgressBar: true,
            didOpen: (toast) => {
                toast.addEventListener('mouseenter', Swal.stopTimer)
                toast.addEventListener('mouseleave', Swal.resumeTimer)
            }
        });
        Toast.fire({
            icon: icon,
            title: title
        });
    }
};

document.addEventListener('DOMContentLoaded', event => {
    // 绑定侧边栏切换按钮
    const sidebarToggle = document.body.querySelector('#sidebarToggle');
    if (sidebarToggle) {
        sidebarToggle.addEventListener('click', event => {
            event.preventDefault();
            App.toggleSidebar();
        });
    }

    // 初始化 Bootstrap Tooltips
    var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'))
    var tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl)
    });
});