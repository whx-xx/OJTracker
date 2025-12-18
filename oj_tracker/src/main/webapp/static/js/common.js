/**
 * 全局公共逻辑 - 处理 Navbar 退出等通用功能
 */

// 统一的退出登录处理 (集成 SweetAlert2)
async function logout() {
    Swal.fire({
        title: '确定要退出登录吗？',
        text: "退出后需要重新登录才能访问系统",
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#3085d6',
        cancelButtonColor: '#d33',
        confirmButtonText: '确定退出',
        cancelButtonText: '取消'
    }).then(async (result) => {
        if (result.isConfirmed) {
            try {
                await fetch('/api/auth/logout', { method: 'POST' });
                window.location.href = '/login';
            } catch (e) {
                console.error("登出失败:", e);
                window.location.href = '/login';
            }
        }
    });
}

document.addEventListener('DOMContentLoaded', function() {
    // 1. 自动高亮当前的导航栏菜单
    const currentPath = window.location.pathname;
    const navLinks = document.querySelectorAll('.navbar-nav .nav-link');

    navLinks.forEach(link => {
        // 如果链接的 href 属性等于当前路径，则设为 active
        if (link.getAttribute('href') === currentPath) {
            link.classList.add('active');
        } else {
            link.classList.remove('active');
        }
    });
});

// 通用美化 Toast 提示
const showToast = (icon, title) => {
    Swal.fire({
        icon: icon,
        title: title,
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 2000,
        timerProgressBar: true
    });
};