package hdc.rjxy.common;

import java.util.HashMap;
import java.util.Map;

public class SyncErrorCodeDict {

    public static class Info {
        public final String desc;
        public final String action;
        public final boolean retryable;

        public Info(String desc, String action, boolean retryable) {
            this.desc = desc;
            this.action = action;
            this.retryable = retryable;
        }
    }

    private static final Map<String, Info> MAP = new HashMap<>();

    static {
        // 绑定问题
        MAP.put("HANDLE_MISSING", new Info("未绑定平台账号", "BIND_HANDLE", false));
        MAP.put("CF_HTTP_400", new Info("handle 无效/格式错误", "FIX_HANDLE", false));

        // 网络波动
        MAP.put("CF_HTTP_502", new Info("CF 服务 502（临时波动）", "RETRY", true));
        MAP.put("CF_HTTP_503", new Info("CF 服务 503（临时波动）", "RETRY", true));
        MAP.put("CF_HTTP_504", new Info("CF 服务 504（超时）", "RETRY", true));
        MAP.put("CF_TIMEOUT",  new Info("请求超时", "RETRY", true));
        MAP.put("CF_RESET",    new Info("连接被重置", "RETRY", true));
        MAP.put("CF_PARSE_ERROR", new Info("返回解析失败（接口结构变化/脏数据）", "RETRY", true));

        MAP.put("RATING_UNCHANGED", new Info("无新增比赛", "NONE", false));
        MAP.put("UNKNOWN", new Info("未知错误", "RETRY", true));
    }

    public static Info get(String code) {
        if (code == null) return new Info("未知错误码", "RETRY", true);
        Info info = MAP.get(code);
        if (info != null) return info;
        // 未收录的：默认可重跑
        return new Info("未收录的错误码: " + code, "RETRY", true);
    }

    // 兼容你已有 explain() 用法
    public static String explain(String code) {
        return get(code).desc;
    }
}
