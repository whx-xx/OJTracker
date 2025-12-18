document.addEventListener('DOMContentLoaded', function() {
    const loginBtn = document.getElementById('btnLogin');
    const btnText = document.getElementById('btnText');
    const btnSpinner = document.getElementById('btnSpinner');
    const alertBox = document.getElementById('loginAlert');
    const errorMsg = document.getElementById('errorMsg');

    loginBtn.addEventListener('click', async function() {
        const username = document.getElementById('username').value;
        const password = document.getElementById('password').value;

        // 1. 简单校验
        if (!username || !password) {
            showError("请输入完整的账号和密码");
            return;
        }

        // 2. 切换加载状态
        setLoading(true);
        alertBox.classList.add('d-none');

        try {
            const response = await fetch('/api/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password })
            });

            const result = await response.json();

            if (result.code === 200) {
                // 登录成功，加入一个平滑跳转效果
                btnText.textContent = "验证成功，跳转中...";
                setTimeout(() => {
                    window.location.href = '/dashboard';
                }, 800);
            } else {
                showError(result.msg || "账号或密码错误");
                setLoading(false);
            }
        } catch (error) {
            showError("网络异常，请稍后再试");
            setLoading(false);
        }
    });

    function setLoading(isLoading) {
        loginBtn.disabled = isLoading;
        if (isLoading) {
            btnText.classList.add('d-none');
            btnSpinner.classList.remove('d-none');
        } else {
            btnText.classList.remove('d-none');
            btnSpinner.classList.add('d-none');
        }
    }

    function showError(msg) {
        errorMsg.textContent = msg;
        alertBox.classList.remove('d-none');
    }
});