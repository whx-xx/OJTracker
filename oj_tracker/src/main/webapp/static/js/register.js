document.addEventListener('DOMContentLoaded', function() {
    const registerBtn = document.getElementById('btnRegister');
    const btnText = document.getElementById('btnText');
    const btnSpinner = document.getElementById('btnSpinner');
    const alertBox = document.getElementById('registerAlert');
    const errorMsg = document.getElementById('errorMsg');

    registerBtn.addEventListener('click', async function() {
        const studentNo = document.getElementById('studentNo').value.trim();
        const username = document.getElementById('username').value.trim();
        const password = document.getElementById('password').value;

        // 1. 简单校验
        if (!studentNo || !username || !password) {
            showError("请填写完整信息");
            return;
        }

        // 2. 切换加载状态
        setLoading(true);
        alertBox.classList.add('d-none');

        try {
            const response = await fetch('/api/auth/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ studentNo, username, password })
            });

            const result = await response.json();

            if (result.code === 200) {
                // 注册成功
                btnText.textContent = "注册成功，跳转登录...";
                alertBox.classList.remove('alert-danger');
                alertBox.classList.add('alert-success', 'd-flex'); // 也可以用 toast
                alertBox.innerHTML = '<i class="bi bi-check-circle-fill me-2"></i> 注册成功！即将跳转...';

                setTimeout(() => {
                    window.location.href = '/login';
                }, 1500);
            } else {
                showError(result.msg || "注册失败");
                setLoading(false);
            }
        } catch (error) {
            showError("网络异常，请稍后再试");
            setLoading(false);
        }
    });

    function setLoading(isLoading) {
        registerBtn.disabled = isLoading;
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
        // 确保是红色警告
        alertBox.classList.remove('alert-success');
        alertBox.classList.add('alert-danger');
    }
});