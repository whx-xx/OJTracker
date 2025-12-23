- # OJTracker

  **OJTracker** 是一个轻量级的 Online Judge 账号数据聚合服务。目前专注于 **Codeforces** 平台，提供数据同步、可视化分析及团队管理功能，帮助通过数据驱动算法能力的提升。

  ------

  ## ✨ 核心功能

  - **📊 数据看板**：自动同步 Codeforces 的提交记录与 Rating 分数，生成活跃度热力图、评分趋势图。
  - **🏆 团队榜单**：支持创建团队内部排行榜，直观展示成员的 Rating 和刷题进度对比。
  - **📅 智能同步**：内置定时任务调度系统，支持每日自动增量更新，也支持管理员手动触发全量补录。
  - **🛡️ 权限管理**：完善的注册/登录系统，区分普通用户与管理员权限（用户管理、日志审计）。

  ## 🛠️ 技术栈

  - **后端框架**: Java 11 + Spring MVC 6 + MyBatis 3.5
  - **数据库**: MySQL 8.0+
  - **构建工具**: Maven 3.8+
  - **运行容器**: Tomcat 10 (Jakarta Servlet 6)
  - **前端模板**: Thymeleaf + jQuery

  ## 🚀 快速开始

  ### 1. 环境准备

  确保本地已安装 **JDK 11**、**Maven**、**MySQL** 以及 **Tomcat 10**。

  ### 2. 数据库配置

  1. 创建数据库：`oj_tracker`

  2. 导入项目提供的初始化 SQL 脚本（建表结构）。

  3. 修改配置文件 `src/main/resources/db.properties`：

     Properties



     ```
     jdbc.url=jdbc:mysql://localhost:3306/oj_tracker?serverTimezone=UTC
     jdbc.username=你的数据库账号
     jdbc.password=你的数据库密码
     ```

  ### 3. 打包与部署

  在项目根目录下运行 Maven 命令打包：

  Bash



  ```
  mvn clean package
  ```

  构建成功后，将生成的 `target/oj_tracker.war` 部署到 Tomcat 的 `webapps` 目录下启动即可。

  ### 4. 登录系统

  - **访问地址**: `http://localhost:8080/oj_tracker` (具体端口视 Tomcat 配置而定)
  - **初始管理员账号**:
    - 用户名: `admin`
    - 密码: `xxxx` (首次启动后自动生成，请查看服务器日志或源码 `AdminBootstrap.java`)

  ## 📝 典型使用流程

  1. **用户注册/绑定**: 注册账号后，在个人中心绑定你的 Codeforces Handle。
  2. **数据同步**: 系统会自动拉取你的做题记录（也可以手动点击刷新）。
  3. **查看分析**: 访问仪表盘查看你的刷题热力图和 Rating 曲线。
  4. **加入团队**: 查看团队榜单，与队友共同进步。

  ------

  > 由 **Spring MVC + MyBatis** 驱动 | 专注于算法竞赛数据追踪
