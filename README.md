# OJTracker

OJTracker 是一套基于 Spring MVC + MyBatis 的在线判题账号数据聚合服务，当前聚焦 Codeforces 平台（platformCode=CF），支持账号绑定、提交/题目统计、评分趋势、团队榜与后台同步管控。

## 运行前置

- Java 11（已在 `pom.xml` 中设定源码/目标版本）
- Maven 3.8+（打包 WAR）
- MySQL 8.x（默认连接 `jdbc:mysql://localhost:3306/oj_tracker`）
- 兼容 Jakarta Servlet 6 的容器（如 Tomcat 10）

启动前请调整 `src/main/resources/db.properties` 中的数据库地址与账号密码。

## 启动步骤

1. 创建数据库 `oj_tracker`，并导入初始化表结构（如有建表脚本）。
2. 确认 `src/main/resources/db.properties` 的连接信息正确。
3. 打包：`mvn clean package`，生成 `target/oj_tracker.war`。
4. 将 WAR 部署到支持 Jakarta Servlet 6 的容器，或在容器内以 ROOT 方式运行。
5. 首次启动会自动创建管理员账号：用户名 `admin`，初始密码 `xxxx`（登录后需修改）。【F:src/main/java/hdc/rjxy/init/AdminBootstrap.java†L19-L41】

## 功能模块

- **认证与会话**：注册、登录、修改密码、退出登录，登录态通过 `HttpSession` 维护。【F:src/main/java/hdc/rjxy/controller/AuthController.java†L16-L68】【F:src/main/java/hdc/rjxy/controller/MeController.java†L14-L23】
- **个人平台绑定**：查看/更新已绑定的 OJ 账号（默认 Codeforces），用于同步数据。【F:src/main/java/hdc/rjxy/controller/MePlatformController.java†L20-L48】
- **提交与题目**：查看近一周/当日的提交时间线，可主动刷新增量提交；获取本周推荐或已做题目列表。【F:src/main/java/hdc/rjxy/controller/UserSubmissionController.java†L18-L52】【F:src/main/java/hdc/rjxy/controller/UserProblemController.java†L17-L38】
- **活跃度与战力**：提供热力图式活跃度统计，以及按天/比赛的评分历史或近期评分走势。【F:src/main/java/hdc/rjxy/controller/UserActivityController.java†L18-L44】【F:src/main/java/hdc/rjxy/controller/UserRatingController.java†L20-L43】【F:src/main/java/hdc/rjxy/controller/UserStatsController.java†L15-L49】
- **团队榜**：按平台展示团队成员排行，便于对比练习成效。【F:src/main/java/hdc/rjxy/controller/TeamController.java†L17-L35】
- **数据同步任务**：支持管理员手动触发 Codeforces 评分同步、近 N 天提交同步，查看作业详情与失败原因，及重跑失败用户；可开关定时任务调度。【F:src/main/java/hdc/rjxy/controller/AdminSyncController.java†L18-L114】【F:src/main/java/hdc/rjxy/controller/AdminScheduleController.java†L18-L39】
- **用户与操作审计**：管理员可分页管理用户昵称/状态、重置密码，并查看后台操作日志。【F:src/main/java/hdc/rjxy/controller/AdminUserController.java†L17-L73】【F:src/main/java/hdc/rjxy/controller/AdminOpLogController.java†L13-L33】

## 典型使用流程

1. **注册/登录**：通过 `/api/auth/register` 创建账号，再用 `/api/auth/login` 获得会话。
2. **绑定平台账号**：登录后调用 `/api/me/platforms`（GET）查看已绑定列表，或通过 PUT 传入平台标识与用户名/handle 完成绑定，确保后续同步可用。
3. **浏览个人数据**：
   - `/api/user/submissions/timeline` 查看近提交记录；如需立即更新，调用 `/api/user/submissions/refresh` 拉取最新提交。
   - `/api/user/activity/heatmap` 获取活跃度；`/api/user/rating/history` 与 `/api/users/{userId}/rating-history` 查看评分曲线；`/api/users/summary` 查看近期统计摘要。
   - `/api/user/problems/week` 获取周练题目数据。
4. **团队与榜单**：使用 `/api/team/rankings` 查看团队成员排名情况。
5. **后台运维**（管理员角色）：
   - `/api/admin/sync/run` 触发评分/日常同步，`/api/admin/sync/rerun` 重跑失败用户，`/api/admin/sync/jobs` & `/api/admin/sync/jobs/{jobId}` 查看历史与详情。
   - `/api/admin/sync/overview` 快速了解近期同步结果，`/api/admin/schedule/enable` 控制定时任务开关。
   - `/api/admin/users` 相关接口管理用户；`/api/admin/op-logs` 查询后台操作记录。

## 接口调用提示

- 所有需要会话的接口依赖 `HttpSession`，请在登录后保持同一会话或携带返回的 Cookie。
- 平台代码默认 `CF`，如果后续扩展平台，可在查询参数中指定 `platformCode`。
- 手工同步任务会根据用户是否绑定对应平台账号决定是否跳过，失败原因与建议操作可在作业详情中查看。【F:src/main/java/hdc/rjxy/service/SyncService.java†L74-L176】
