/**
 * static/js/api.js
 * 依赖: ui.js (用于显示错误提示)
 */
const API = {
    /**
     * 统一 Fetch 请求
     * @param {string} url 请求地址
     * @param {object} options 配置项 {method, body, headers...}
     * @returns {Promise<any>} 返回 R<T>.data
     */
    fetch: async (url, options = {}) => {
        // 1. 默认配置
        const defaultHeaders = {
            'Content-Type': 'application/json',
            'X-Requested-With': 'XMLHttpRequest' // 标识为 AJAX，方便后端识别
        };

        const config = {
            method: 'GET',
            headers: { ...defaultHeaders, ...options.headers },
            credentials: 'same-origin', // 关键：携带 Session Cookie
            ...options
        };

        // 自动序列化 body
        if (config.body && typeof config.body === 'object' && !(config.body instanceof FormData)) {
            config.body = JSON.stringify(config.body);
        }

        try {
            const response = await fetch(url, config);

            // 2. HTTP 协议层错误处理
            if (response.status === 401) {
                // 未登录：跳转登录页，并携带当前路径以便跳回
                const currentPath = encodeURIComponent(window.location.pathname + window.location.search);
                window.location.href = `/login?next=${currentPath}`;
                return Promise.reject('Unauthorized'); // 中断后续逻辑
            }

            if (response.status === 403) {
                UI.showToast('无权访问此资源', 'warning');
                return Promise.reject('Forbidden');
            }

            if (!response.ok) {
                throw new Error(`HTTP Error: ${response.status}`);
            }

            // 3. 解析 JSON
            const res = await response.json();

            // 4. 业务逻辑层错误处理 (R<T> 结构)
            if (res.code === 200) {
                return res.data; // 成功：只返回 data 部分
            } else {
                // 业务错误：弹窗提示
                const errorMsg = res.msg || '未知业务错误';
                UI.showToast(errorMsg, 'danger');
                throw new Error(errorMsg);
            }

        } catch (error) {
            // 只有当不是手动 reject 的时候才打印/提示网络错误
            if (error !== 'Unauthorized' && error !== 'Forbidden') {
                console.error('API Fetch Error:', error);
                // 避免重复弹窗（如果上面已经弹过业务错误）
                if (!error.message || !error.message.includes('业务错误')) {
                   // UI.showToast('网络请求失败或服务不可用', 'danger');
                }
            }
            throw error; // 继续向上抛出，以便页面内 .catch 处理特定逻辑
        }
    }
};