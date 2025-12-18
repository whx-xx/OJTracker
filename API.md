# OJTracker 接口文档

本项目的接口均返回 JSON，接口路径统一以 `/api` 开头。未标注特殊说明的接口均需要登录态（`JSESSIONID` Cookie），管理员接口（`/api/admin/**`）需要 `role=ADMIN`。登录态由会话维持，拦截器会校验是否登录、账号状态以及是否需要先修改密码。 【F:oj_tracker/src/main/java/hdc/rjxy/common/AuthInterceptor.java†L9-L64】

## 公共约定

- 响应包装：所有接口（`/api/ping` 除外）统一使用 `R<T>` 包装，包含 `code`（0=成功）、`msg`（消息）和 `data`（业务数据）。 【F:oj_tracker/src/main/java/hdc/rjxy/common/R.java†L6-L23】
- 分页结构：分页类接口返回 `PageResult<T>`，字段为 `total`（总数）和 `list`（数据列表）。 【F:oj_tracker/src/main/java/hdc/rjxy/common/PageResult.java†L8-L19】
- 时间格式：使用 Jackson JavaTimeModule，日期时间序列化为 ISO-8601 字符串，而不是时间戳数组。 【F:oj_tracker/src/main/java/hdc/rjxy/config/WebMvcConfig.java†L29-L37】

## 健康检查

| 方法 | 路径 | 说明 | 请求体 | 响应 |
| --- | --- | --- | --- | --- |
| GET | `/api/ping` | 存活检查 | 无 | `{ "ok": true }` 【F:oj_tracker/src/main/java/hdc/rjxy/controller/PingController.java†L8-L17】 |

## 认证与账户

| 方法 | 路径 | 说明 | 请求体 | 响应数据 |
| --- | --- | --- | --- | --- |
| POST | `/api/auth/login` | 登录并写入会话 | `{"username","password"}` | `UserSession`（包含 `id, username, studentNo, nickname, role, status, mustChangePassword`）【F:oj_tracker/src/main/java/hdc/rjxy/controller/AuthController.java†L21-L41】【F:oj_tracker/src/main/java/hdc/rjxy/pojo/UserSession.java†L8-L16】 |
| POST | `/api/auth/register` | 注册账号 | `{"studentNo","username","password"}` | 新用户 ID（Long）【F:oj_tracker/src/main/java/hdc/rjxy/controller/AuthController.java†L43-L48】 |
| POST | `/api/auth/change-password` | 修改密码（需登录） | `{"oldPassword","newPassword"}` | `data: null`【F:oj_tracker/src/main/java/hdc/rjxy/controller/AuthController.java†L50-L65】 |
| POST | `/api/auth/logout` | 注销会话 | 无 | `data: null`【F:oj_tracker/src/main/java/hdc/rjxy/controller/AuthController.java†L67-L72】 |
| GET | `/api/me` | 获取当前登录用户 | 无 | `UserSession`【F:oj_tracker/src/main/java/hdc/rjxy/controller/MeController.java†L11-L20】【F:oj_tracker/src/main/java/hdc/rjxy/pojo/UserSession.java†L8-L16】 |

### 平台绑定（个人）

| 方法 | 路径 | 说明 | 请求体 / 查询参数 | 响应数据 |
| --- | --- | --- | --- | --- |
| GET | `/api/me/platforms` | 查询本人已绑定的各评测平台账号 | 无 | `MyPlatformAccountVO` 列表：`platformId, platformCode, platformName, identifierType, identifierValue, verified`【F:oj_tracker/src/main/java/hdc/rjxy/controller/MePlatformController.java†L23-L35】【F:oj_tracker/src/main/java/hdc/rjxy/pojo/vo/MyPlatformAccountVO.java†L6-L13】 |
| PUT | `/api/me/platforms` | 批量更新绑定信息 | `{"items":[{"platformId","identifierType","identifierValue"}]}` | `data: null`【F:oj_tracker/src/main/java/hdc/rjxy/controller/MePlatformController.java†L37-L49】【F:oj_tracker/src/main/java/hdc/rjxy/pojo/dto/UpdateMyPlatformsReq.java†L6-L19】 |

## 用户数据与统计

| 方法 | 路径 | 说明 | 查询参数 / 请求体 | 响应数据 |
| --- | --- | --- | --- | --- |
| GET | `/api/user/activity/heatmap` | 近 N 天活跃度热力图 | `platformCode`（必填）、`days`（默认 90） | `HeatmapDayVO` 列表：`day, submitCnt, acceptCnt, solvedCnt`【F:oj_tracker/src/main/java/hdc/rjxy/controller/UserActivityController.java†L18-L35】【F:oj_tracker/src/main/java/hdc/rjxy/pojo/vo/HeatmapDayVO.java†L6-L12】 |
| GET | `/api/user/problems/week` | 本周（或最近一次周赛）题目记录 | `platformCode`（默认 CF） | `WeeklyProblemVO` 列表：`contestId, problemIndex, problemName, problemUrl, verdict, submitTime, submissionId`【F:oj_tracker/src/main/java/hdc/rjxy/controller/UserProblemController.java†L23-L36】【F:oj_tracker/src/main/java/hdc/rjxy/pojo/vo/WeeklyProblemVO.java†L6-L16】 |
| GET | `/api/user/submissions/timeline` | 提交时间线 | `platformCode`（默认 CF）、`range`（TODAY/WEEK，默认 WEEK）、`limit`（默认 50） | `SubmissionTimelineVO` 列表：`submissionId, contestId, problemIndex, problemName, problemUrl, verdict, submitTime`【F:oj_tracker/src/main/java/hdc/rjxy/controller/UserSubmissionController.java†L18-L40】【F:oj_tracker/src/main/java/hdc/rjxy/pojo/vo/SubmissionTimelineVO.java†L6-L17】 |
| POST | `/api/user/submissions/refresh` | 轻量补拉最新提交 | `platformCode`（默认 CF）、`count`（默认 200） | `RefreshResultVO`：`fetched`（拉取条数）、`inserted`（增量写入条数）【F:oj_tracker/src/main/java/hdc/rjxy/controller/UserSubmissionController.java†L42-L58】【F:oj_tracker/src/main/java/hdc/rjxy/pojo/vo/RefreshResultVO.java†L6-L11】 |
| GET | `/api/user/rating/history` | 最近 N 天 Rating 变化 | `platformCode`（必填）、`days`（默认 365） | `RatingHistoryPointVO` 列表：`time, rating, delta, contestName`【F:oj_tracker/src/main/java/hdc/rjxy/controller/UserRatingController.java†L18-L35】【F:oj_tracker/src/main/java/hdc/rjxy/pojo/vo/RatingHistoryPointVO.java†L6-L15】 |
| GET | `/api/users/{userId}/rating-history` | （公开）指定用户 Rating 轨迹 | `platformCode`（默认 CF）、`limit`（默认 100） | `RatingPointVO` 列表：`time, rating, contestName, delta`【F:oj_tracker/src/main/java/hdc/rjxy/controller/UserStatsController.java†L18-L31】【F:oj_tracker/src/main/java/hdc/rjxy/pojo/vo/RatingPointVO.java†L6-L15】 |
| GET | `/api/users/summary` | 本人近 N 天刷题与比赛概览 | `platformCode`（必填）、`days`（默认 30） | `UserStatsSummaryVO`：活跃区间、提交/通过/题量、活跃天数、平均提交、Rating 起止与增量、最近比赛信息等【F:oj_tracker/src/main/java/hdc/rjxy/controller/UserStatsController.java†L33-L45】【F:oj_tracker/src/main/java/hdc/rjxy/pojo/vo/UserStatsSummaryVO.java†L6-L25】 |
| GET | `/api/team/rankings` | 团队榜单 | `platformCode`（默认 CF） | `TeamRankingVO` 列表：`userId, studentNo, nickname, rating, snapshotTime`【F:oj_tracker/src/main/java/hdc/rjxy/controller/TeamController.java†L18-L27】【F:oj_tracker/src/main/java/hdc/rjxy/pojo/vo/TeamRankingVO.java†L6-L15】 |

## 管理端接口（需 ADMIN）

### 调度与同步

| 方法 | 路径 | 说明 | 参数 / 请求体 | 响应数据 |
| --- | --- | --- | --- | --- |
| POST | `/api/admin/schedule/enable` | 启停定时调度 | `enabled`（boolean，查询参数） | `data: null`【F:oj_tracker/src/main/java/hdc/rjxy/controller/AdminScheduleController.java†L18-L34】 |
| GET | `/api/admin/sync/jobs` | 分页查询同步任务 | `page`（默认1）、`pageSize`（默认20）、`jobType`（可选） | `PageResult<SyncJobLogVO>`，`SyncJobLogVO` 含 `id, jobType, status, startTime, endTime, durationMs, totalCount, successCount, failCount, message, triggerSource`【F:oj_tracker/src/main/java/hdc/rjxy/controller/AdminSyncController.java†L19-L44】【F:oj_tracker/src/main/java/hdc/rjxy/pojo/vo/SyncJobLogVO.java†L6-L17】 |
| GET | `/api/admin/sync/jobs/{jobId}` | 查看单次任务详情 | 无 | `SyncJobDetailVO`：`job`（同上）与 `failList`（`SyncUserFailVO` 列表，含用户/平台、错误码、建议操作、是否可重试等）【F:oj_tracker/src/main/java/hdc/rjxy/controller/AdminSyncController.java†L47-L57】【F:oj_tracker/src/main/java/hdc/rjxy/pojo/vo/SyncJobDetailVO.java†L6-L11】【F:oj_tracker/src/main/java/hdc/rjxy/pojo/vo/SyncUserFailVO.java†L6-L20】 |
| POST | `/api/admin/sync/run` | 手动触发同步 | `jobType`（RATING_SYNC/DAILY_SYNC），`days`（每日同步可选，默认 3） | 返回新建任务 ID（Long）。不认识的 `jobType` 将返回 400。 【F:oj_tracker/src/main/java/hdc/rjxy/controller/AdminSyncController.java†L59-L89】 |
| GET | `/api/admin/sync/overview` | 同步总览 | `limit`（默认 20） | `SyncOverviewVO`：最近 Rating/Daily 任务和近期任务列表【F:oj_tracker/src/main/java/hdc/rjxy/controller/AdminSyncController.java†L91-L104】【F:oj_tracker/src/main/java/hdc/rjxy/pojo/vo/SyncOverviewVO.java†L6-L12】 |
| POST | `/api/admin/sync/rerun` | 仅重跑失败用户 | `jobId`（Long） | 返回新建任务 ID（Long）【F:oj_tracker/src/main/java/hdc/rjxy/controller/AdminSyncController.java†L106-L115】 |

### 用户与操作日志

| 方法 | 路径 | 说明 | 参数 / 请求体 | 响应数据 |
| --- | --- | --- | --- | --- |
| GET | `/api/admin/users` | 分页查询用户 | `page`（默认1）、`pageSize`（默认20）、`keyword`（可选） | `PageResult<UserAdminVO>`，字段 `id, studentNo, username, nickname, role, status, mustChangePassword, createdAt, updatedAt`【F:oj_tracker/src/main/java/hdc/rjxy/controller/AdminUserController.java†L21-L33】【F:oj_tracker/src/main/java/hdc/rjxy/pojo/vo/UserAdminVO.java†L6-L17】 |
| PUT | `/api/admin/users/{id}/nickname` | 修改昵称 | `{"nickname"}` | `data: null`【F:oj_tracker/src/main/java/hdc/rjxy/controller/AdminUserController.java†L35-L41】【F:oj_tracker/src/main/java/hdc/rjxy/pojo/dto/UpdateNicknameReq.java†L6-L11】 |
| PUT | `/api/admin/users/{id}/status` | 启禁用账户 | `{"status"}`（0/1） | `data: null`【F:oj_tracker/src/main/java/hdc/rjxy/controller/AdminUserController.java†L43-L49】【F:oj_tracker/src/main/java/hdc/rjxy/pojo/dto/UpdateStatusReq.java†L6-L11】 |
| POST | `/api/admin/users/{id}/reset-password` | 管理员重置密码 | 无 | `data: null`（操作会记录日志并校验登录管理员）【F:oj_tracker/src/main/java/hdc/rjxy/controller/AdminUserController.java†L51-L65】 |
| GET | `/api/admin/op-logs` | 分页查询管理员操作日志 | `page`（默认1）、`pageSize`（默认20）、`opType`（可选） | `PageResult<AdminOpLogVO>`，字段 `id, adminId, adminName, targetUserId, targetName, opType, opTime, ip, remark`【F:oj_tracker/src/main/java/hdc/rjxy/controller/AdminOpLogController.java†L14-L22】【F:oj_tracker/src/main/java/hdc/rjxy/pojo/vo/AdminOpLogVO.java†L6-L18】 |
