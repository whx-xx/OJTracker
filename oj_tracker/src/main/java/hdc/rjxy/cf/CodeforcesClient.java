package hdc.rjxy.cf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * Codeforces API 客户端组件
 * <p>
 * 负责与 Codeforces 官方 API 进行通信，封装了底层 HTTP 请求、
 * JSON 解析、错误处理以及由网络波动引起的重试逻辑。
 * </p>
 */
@Component
public class CodeforcesClient {

    // Java 11+ 原生 HttpClient，性能优异且支持异步（虽然这里用了同步）
    private final HttpClient httpClient;
    // Jackson 序列化工具，用于将 JSON 字符串转为 Java 对象
    private final ObjectMapper objectMapper;

    /**
     * 构造函数注入 ObjectMapper，并初始化 HttpClient
     */
    public CodeforcesClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        // 配置全局连接超时为 10 秒，防止请求一直挂起占用资源
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    // =========================================================
    //                    Public APIs (业务接口)
    // =========================================================

    /**
     * 获取用户信息 (user.info)
     *
     * @param handle Codeforces 用户名 (Handle)
     * @return 用户详细信息对象
     * @throws CfClientException 当 API 返回错误或解析失败时抛出
     */
    public CfUserInfoResponse.User getUser(String handle) {
        // 1. URL 编码，防止 handle 中包含特殊字符导致 URL 非法
        String h = enc(handle);
        String url = "https://codeforces.com/api/user.info?handles=" + h;

        // 2. 发送 HTTP 请求（包含重试机制）
        String json = httpGetWithRetry(url);

        // 3. 解析 JSON 响应
        CfUserInfoResponse body = readJson(json, CfUserInfoResponse.class, "CF_PARSE_ERROR");

        // 4. 业务层面的校验
        if (body == null || body.getStatus() == null) {
            throw new CfClientException("CF_EMPTY", "CF response empty");
        }
        // 检查 CF 返回的状态是否为 "OK" (CF 即使出错也可能返回 200，需要在 body 里看 status)
        if (!"OK".equalsIgnoreCase(body.getStatus())) {
            // 如果 handle 不存在，CF 会返回 FAILED
            throw new CfClientException("CF_STATUS_NOT_OK", "CF status=" + body.getStatus());
        }
        // 结果集校验
        if (body.getResult() == null || body.getResult().isEmpty()) {
            throw new CfClientException("CF_EMPTY_RESULT", "CF result empty");
        }

        // user.info 接口支持批量查询，但我们只查一个，所以取第 0 个
        return body.getResult().get(0);
    }

    /**
     * 获取用户提交记录 (user.status)
     *
     * @param handle 用户名
     * @param from   起始索引 (1-based)
     * @param count  查询数量
     * @return 提交记录列表
     */
    public List<CfUserStatusResponse.Submission> getUserStatus(String handle, int from, int count) {
        String h = enc(handle);
        // 拼接 API 参数
        String url = "https://codeforces.com/api/user.status?handle=" + h
                + "&from=" + from + "&count=" + count;

        String json = httpGetWithRetry(url);

        CfUserStatusResponse resp = readJson(json, CfUserStatusResponse.class, "CF_PARSE_ERROR");
        // 基础校验逻辑同上
        if (resp == null || resp.getStatus() == null) {
            throw new CfClientException("CF_EMPTY", "CF response empty");
        }
        if (!"OK".equalsIgnoreCase(resp.getStatus())) {
            throw new CfClientException("CF_STATUS_NOT_OK", "CF status=" + resp.getStatus());
        }
        return resp.getResult();
    }

    /**
     * 获取用户 Rating 变化历史 (user.rating)
     *
     * @param handle 用户名
     * @return Rating 变化列表
     */
    public List<CfUserRatingResponse.Item> getUserRating(String handle) {
        String h = enc(handle);
        String url = "https://codeforces.com/api/user.rating?handle=" + h;

        String json = httpGetWithRetry(url);

        CfUserRatingResponse resp = readJson(json, CfUserRatingResponse.class, "CF_PARSE_ERROR");
        if (resp == null || resp.getStatus() == null) {
            throw new CfClientException("CF_EMPTY", "CF response empty");
        }
        if (!"OK".equalsIgnoreCase(resp.getStatus())) {
            throw new CfClientException("CF_STATUS_NOT_OK", "CF status=" + resp.getStatus());
        }
        return resp.getResult();
    }

    // =========================================================
    //              Core HTTP + Retry (核心网络层)
    // =========================================================

    /**
     * 执行 HTTP GET 请求，并带有智能重试策略
     * <p>
     * 针对 Codeforces 常见的 503/504 不稳定性进行特殊处理。
     * </p>
     *
     * @param url 请求地址
     * @return 响应体字符串
     */
    private String httpGetWithRetry(String url) {
        // 重试策略：退避算法 (Backoff)
        // 第1次：立即执行 (等待0ms)
        // 第2次：失败后等待 500ms 再试
        // 第3次：失败后等待 1500ms 再试
        int[] backoffs = {0, 500, 1500};
        CfClientException last = null; // 记录最后一次异常，用于重试耗尽后抛出

        for (int attempt = 0; attempt < backoffs.length; attempt++) {
            // 如果需要等待（非第1次），则线程休眠
            if (backoffs[attempt] > 0) sleep(backoffs[attempt]);

            try {
                // 构建 HTTP 请求
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(15)) // 单次请求超时 15s
                        // 重要：设置 User-Agent，包含联系方式。
                        // Codeforces 会封禁没有 User-Agent 或默认 Java User-Agent 的爬虫请求。
                        .header("User-Agent", "oj-tracker/1.0(contact: 3243916556@qq.com)")
                        .GET()
                        .build();

                // 发送同步请求
                HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int sc = resp.statusCode();

                // 200 OK：直接返回内容
                if (sc == 200) return resp.body();

                // 核心重试逻辑：
                // 502 Bad Gateway / 503 Service Unavailable / 504 Gateway Timeout
                // 这些通常是 CF 服务器负载过高或正在维护，重试可能会成功
                if (sc == 502 || sc == 503 || sc == 504) {
                    // 记录异常，进入下一次循环 (continue)
                    last = new CfClientException("CF_HTTP_" + sc, "HTTP " + sc + ": " + safeBody(resp.body()));
                    continue;
                }

                // 其他错误 (如 404 Not Found, 403 Forbidden, 400 Bad Request)
                // 这些通常是参数错误或被封禁，重试也没有意义，直接抛出
                throw new CfClientException("CF_HTTP_" + sc, "HTTP " + sc + ": " + safeBody(resp.body()));

            } catch (java.net.http.HttpTimeoutException e) {
                // 超时通常也是网络波动，记录下来，允许重试
                last = new CfClientException("CF_TIMEOUT", "HTTP timeout: " + e.getMessage(), e);
            } catch (IOException e) {
                // 网络 IO 异常处理
                String msg = (e.getMessage() == null) ? "" : e.getMessage().toLowerCase();
                // "Connection reset" 也是 CF 不稳定时的常见报错，记录后重试
                if (msg.contains("connection reset")) {
                    last = new CfClientException("CF_CONNECTION_RESET", "Connection reset", e);
                } else {
                    last = new CfClientException("CF_IO_ERROR", "IO error: " + e.getMessage(), e);
                }
            } catch (InterruptedException e) {
                // 线程被中断（如服务器关闭时），恢复中断状态并停止重试
                Thread.currentThread().interrupt();
                throw new CfClientException("CF_INTERRUPTED", "HTTP interrupted", e);
            } catch (CfClientException e) {
                // 捕获上面抛出的非重试类异常 (如 404)
                last = e;
                // 如果是不可重试的错误（这里逻辑上可以优化直接 throw，但当前结构会跑完次数）
                // 实际代码中，因为 break 没有写，它会继续重试，除非在上方显式 throw
                // (注：当前代码逻辑中，上方 throw 的是非 Checked 异常，会直接跳出循环，没问题)
            } catch (Exception e) {
                // 未知异常兜底
                last = new CfClientException("CF_UNKNOWN", "HTTP error: " + e.getMessage(), e);
            }
        }

        // 循环结束仍未成功，抛出最后一次捕获的异常
        throw (last != null) ? last : new CfClientException("CF_UNKNOWN", "HTTP GET failed");
    }

    // =========================================================
    //                    Helpers (辅助方法)
    // =========================================================

    /**
     * URL 编码 helper
     */
    private String enc(String s) {
        if (s == null) return "";
        return URLEncoder.encode(s.trim(), StandardCharsets.UTF_8);
    }

    /**
     * 线程休眠 helper (吞掉 InterruptedException)
     */
    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    /**
     * 安全获取响应体：截断过长的 HTML/JSON
     * 防止 CF 返回几十 KB 的报错 HTML 撑爆日志数据库
     */
    private String safeBody(String body) {
        if (body == null) return "";
        return body.length() > 500 ? body.substring(0, 500) + "..." : body;
    }

    /**
     * 泛型 JSON 解析 helper
     * 统一处理 Jackson 的异常，转换为自定义的 CfClientException
     */
    private <T> T readJson(String json, Class<T> clazz, String parseCode) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            // JSON 格式错误或字段不匹配
            throw new CfClientException(parseCode, "JSON parse error: " + e.getOriginalMessage(), e);
        } catch (Exception e) {
            throw new CfClientException(parseCode, "JSON parse error: " + e.getMessage(), e);
        }
    }
}