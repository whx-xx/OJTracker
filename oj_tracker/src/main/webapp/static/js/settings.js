document.addEventListener('DOMContentLoaded', function() {
    loadMyPlatforms();
});

// 1. 保存昵称
async function saveNickname() {
    const nick = document.getElementById('nicknameInput').value;
    if (!nick) return Swal.fire("提示", "请输入新昵称", "info");

    const res = await fetch('/api/me/nickname', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ nickname: nick })
    });
    const data = await res.json();
    if (data.code === 200) {
        Swal.fire("成功", "昵称已修改", "success").then(() => location.reload());
    } else {
        Swal.fire("错误", data.msg, "error");
    }
}

// 2. 加载平台账号
async function loadMyPlatforms() {
    const listDiv = document.getElementById('platformList');
    const res = await fetch('/api/me/platforms');
    const data = await res.json();
    if (data.code === 200) {
        if (data.data.length === 0) {
            listDiv.innerHTML = '<p class="text-muted small">暂未绑定账号。</p>';
            return;
        }
        listDiv.innerHTML = data.data.map(acc => `
            <div class="d-flex justify-content-between p-2 mb-2 bg-light border rounded small">
                <span><strong>${acc.platformName}:</strong> ${acc.identifierValue || '未配置'}</span>
                <span class="badge ${acc.verified ? 'bg-success' : 'bg-secondary'}">${acc.verified ? '已验证' : '同步中'}</span>
            </div>
        `).join('');
    }
}

// 3. 绑定平台
async function addPlatform() {
    const handle = document.getElementById('newPlatformHandle').value;
    if (!handle) return showToast('info', '请输入 Handle');

    const res = await fetch('/api/me/platforms', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            items: [{ platformId: 1, identifierValue: handle, identifierType: 'handle' }]
        })
    });
    const data = await res.json();
    if (data.code === 200) {
        showToast('success', '绑定成功');
        loadMyPlatforms();
        document.getElementById('newPlatformHandle').value = "";
    } else {
        Swal.fire("失败", data.msg, "error");
    }
}

// 4. 修改密码
async function updatePassword() {
    const oldP = document.getElementById('oldPwd').value;
    const newP = document.getElementById('newPwd').value;
    const confirmP = document.getElementById('confirmPwd').value;

    if (newP !== confirmP) return Swal.fire("提醒", "两次新密码输入不一致", "warning");

    const res = await fetch('/api/auth/change-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ oldPassword: oldP, newPassword: newP })
    });
    const data = await res.json();
    if (data.code === 200) {
        Swal.fire("成功", "密码已修改，请重新登录", "success").then(() => {
            window.location.href = '/login';
        });
    } else {
        Swal.fire("错误", data.msg, "error");
    }
}