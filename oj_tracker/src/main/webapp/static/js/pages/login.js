/**
 * static/js/pages/login.js
 * 依赖: api.js, ui.js
 */
document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.getElementById('loginForm');
    const loginBtn = document.getElementById('loginBtn');
    const normalText = loginBtn.querySelector('.normal-text');
    const loadingText = loginBtn.querySelector('.loading-text');
    const inputs = loginForm.querySelectorAll('input');

    // 切换加载状态
    const toggleLoading = (isLoading) => {
        loginBtn.disabled = isLoading;
        inputs.forEach(input => input.disabled = isLoading);
        if (isLoading) {
            normalText.classList.add('d-none');
            loadingText.classList.remove('d-none');
        } else {
            normalText.classList.remove('d-none');
            loadingText.classList.add('d-none');
        }
    };

    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        // 1. 简单校验
        const username = loginForm.username.value.trim();
        const password = loginForm.password.value.trim();

        if (!username || !password) {
            UI.showToast('请输入用户名和密码', 'warning');
            return;
        }

        // 2. 进入加载态
        toggleLoading(true);

        try {
            // 3. 发起请求
            // API.fetch 内部已处理 JSON 解析、401/403 跳转
            // 这里只需要处理 200 OK 的业务逻辑
            // 假设接口返回 R<String> token 或 R<UserVO> user
            const data = await API.fetch('/api/auth/login', {
                method: 'POST',
                body: {
                    username: username,
                    password: password
                }
            });

            // 4. 登录成功处理
            UI.showToast('登录成功，正在跳转...', 'success');

            // 解析 URL 中的 next 参数，如果有则跳转回去，否则跳首页
            const urlParams = new URLSearchParams(window.location.search);
            const nextUrl = urlParams.get('next');

            setTimeout(() => {
                window.location.href = nextUrl ? decodeURIComponent(nextUrl) : '/';
            }, 800); // 稍微延迟一点，让用户看到成功提示

        } catch (error) {
            // 5. 错误处理
            // API.fetch 已经弹出了 Toast (针对 msg)，这里主要是重置按钮状态
            console.error('Login failed:', error);
        } finally {
            // 无论成功失败，恢复按钮状态 (成功跳转前瞬间恢复也无所谓)
            // 如果成功后跳转很快，用户可能看不到恢复，也没关系
            if (!loginBtn.disabled) return; // 已经被跳转打断
            toggleLoading(false);
        }
    });
});