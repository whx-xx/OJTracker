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

@Component
public class CodeforcesClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CodeforcesClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    // ---------- public APIs ----------

    public CfUserInfoResponse.User getUser(String handle) {
        String h = enc(handle);
        String url = "https://codeforces.com/api/user.info?handles=" + h;

        String json = httpGetWithRetry(url);

        CfUserInfoResponse body = readJson(json, CfUserInfoResponse.class, "CF_PARSE_ERROR");
        if (body == null || body.getStatus() == null) {
            throw new CfClientException("CF_EMPTY", "CF response empty");
        }
        if (!"OK".equalsIgnoreCase(body.getStatus())) {
            // CF 有时会返回 FAILED + comment（你的 VO 里不一定有 comment，直接把整段 json 留给 message 也行）
            throw new CfClientException("CF_STATUS_NOT_OK", "CF status=" + body.getStatus());
        }
        if (body.getResult() == null || body.getResult().isEmpty()) {
            throw new CfClientException("CF_EMPTY_RESULT", "CF result empty");
        }
        return body.getResult().get(0);
    }

    public List<CfUserStatusResponse.Submission> getUserStatus(String handle, int from, int count) {
        String h = enc(handle);
        String url = "https://codeforces.com/api/user.status?handle=" + h
                + "&from=" + from + "&count=" + count;

        String json = httpGetWithRetry(url);

        CfUserStatusResponse resp = readJson(json, CfUserStatusResponse.class, "CF_PARSE_ERROR");
        if (resp == null || resp.getStatus() == null) {
            throw new CfClientException("CF_EMPTY", "CF response empty");
        }
        if (!"OK".equalsIgnoreCase(resp.getStatus())) {
            throw new CfClientException("CF_STATUS_NOT_OK", "CF status=" + resp.getStatus());
        }
        return resp.getResult();
    }

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

    // ---------- core http + retry ----------

    private String httpGetWithRetry(String url) {
        int[] backoffs = {0, 500, 1500}; // 最多 3 次：立即、500ms、1500ms
        CfClientException last = null;

        for (int attempt = 0; attempt < backoffs.length; attempt++) {
            if (backoffs[attempt] > 0) sleep(backoffs[attempt]);

            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(15))
                        .header("User-Agent", "oj-tracker/1.0(contact: 3243916556@qq.com)")
                        .GET()
                        .build();

                HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int sc = resp.statusCode();

                if (sc == 200) return resp.body();

                // 502/503/504：上游波动 -> 允许重试
                if (sc == 502 || sc == 503 || sc == 504) {
                    last = new CfClientException("CF_HTTP_" + sc, "HTTP " + sc + ": " + safeBody(resp.body()));
                    continue;
                }

                // 其他 HTTP：不重试（一般是 4xx 或者 5xx 非网关）
                throw new CfClientException("CF_HTTP_" + sc, "HTTP " + sc + ": " + safeBody(resp.body()));

            } catch (java.net.http.HttpTimeoutException e) {
                last = new CfClientException("CF_TIMEOUT", "HTTP timeout: " + e.getMessage(), e);
            } catch (IOException e) {
                // 典型：Connection reset
                String msg = (e.getMessage() == null) ? "" : e.getMessage().toLowerCase();
                if (msg.contains("connection reset")) {
                    last = new CfClientException("CF_CONNECTION_RESET", "Connection reset", e);
                } else {
                    last = new CfClientException("CF_IO_ERROR", "IO error: " + e.getMessage(), e);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CfClientException("CF_INTERRUPTED", "HTTP interrupted", e);
            } catch (CfClientException e) {
                last = e;
            } catch (Exception e) {
                last = new CfClientException("CF_UNKNOWN", "HTTP error: " + e.getMessage(), e);
            }
        }

        throw (last != null) ? last : new CfClientException("CF_UNKNOWN", "HTTP GET failed");
    }

    // ---------- helpers ----------

    private String enc(String s) {
        if (s == null) return "";
        return URLEncoder.encode(s.trim(), StandardCharsets.UTF_8);
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    private String safeBody(String body) {
        if (body == null) return "";
        // 避免日志/DB 过大，你的 error_message 是 text 其实可以不截断；这里轻微保护一下
        return body.length() > 500 ? body.substring(0, 500) + "..." : body;
    }

    private <T> T readJson(String json, Class<T> clazz, String parseCode) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            // Jackson 解析失败：字段不匹配、格式错误
            throw new CfClientException(parseCode, "JSON parse error: " + e.getOriginalMessage(), e);
        } catch (Exception e) {
            throw new CfClientException(parseCode, "JSON parse error: " + e.getMessage(), e);
        }
    }
}
