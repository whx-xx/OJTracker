// 全局配置
const API_BASE = '/api';
const DEFAULT_AVATAR = '/static/images/default-avatar.png'; // 请放入一张默认图片

// Axios 拦截器
axios.defaults.withCredentials = true; // 允许携带 Cookie
axios.interceptors.response.use(
    response => response.data,
    error => {
        if (error.response && error.response.status === 401) {
            Swal.fire({
                icon: 'warning',
                title: '登录失效',
                text: '请重新登录',
                background: 'rgba(30, 30, 40, 0.9)', // 暗黑弹窗
                color: '#fff',
                confirmButtonColor: '#6a11cb'
            }).then(() => {
                window.location.href = '/login';
            });
        } else {
            showToast('error', error.response?.data?.msg || '网络错误');
        }
        return Promise.reject(error);
    }
);

// 统一 Toast 提示 (右上角轻提示)
function showToast(icon, title) {
    const Toast = Swal.mixin({
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 3000,
        timerProgressBar: true,
        background: 'rgba(30, 30, 40, 0.9)',
        color: '#fff',
        didOpen: (toast) => {
            toast.addEventListener('mouseenter', Swal.stopTimer)
            toast.addEventListener('mouseleave', Swal.resumeTimer)
        }
    });
    Toast.fire({ icon: icon, title: title });
}

// 统一弹窗 (Modal)
function showModal(title, text, icon = 'info') {
    return Swal.fire({
        title: title,
        text: text,
        icon: icon,
        background: 'rgba(30, 30, 40, 0.95)',
        color: '#fff',
        confirmButtonColor: '#6a11cb',
        confirmButtonText: '确定'
    });
}

// 图片加载失败回退
function imgError(image) {
    image.onerror = "";
    image.src = DEFAULT_AVATAR;
    return true;
}

// 退出登录
function logout() {
    axios.post(API_BASE + '/auth/logout').then(res => {
        if(res.code === 200) {
            window.location.href = '/login';
        }
    });
}