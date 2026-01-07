# 📈 OJ Tracker (Algorithm Training Tracker)

> **"Roots change, but the tree stands strong — so should your logic."**

**OJ Tracker** 是一个专为算法竞赛（ACM-ICPC/CCPC/Codeforces）选手打造的**现代化训练追踪与知识管理系统**。

它不仅是一个数据看板，更是一个**个人训练知识库**。系统通过对接 Codeforces 接口自动同步做题记录，并提供类似 GitHub 的提交热力图、可视化图表分析，同时支持对每一道题目进行**Markdown 笔记记录**和**自定义标签分类**，帮助选手构建自己的算法知识体系。

本项目采用 **纯净的 Spring MVC** 架构（非 Spring Boot）与 **现代原生 JavaScript** 开发，追求轻量、高效与代码的掌控力。

---

## ✨ 核心业务功能 (Core Features)

### 1. 📊 全维度训练看板 (Dashboard)

通过可视化的方式量化训练成果，拒绝盲目刷题：
- **提交热力图 (Heatmap)**：完美复刻 GitHub 风格的年度提交热力图，直观展示训练强度与连贯性。
- **Rating 趋势分析**：集成 ECharts 绘制积分变化曲线，支持缩放查看，包含 CF 官方段位背景色（Newbie 到 LGM）。
- **难度分布统计**：柱状图展示已解决题目的 Rating 分布，帮助选手评估当前训练的舒适区与突破区。
- **实时核心指标**：总解题数、本周解题数、当前 Rating、活跃天数统计。

### 2. 📝 智能题单与知识库 (Submission & Notes)

这是本系统的核心差异化功能——**将“做题记录”转化为“沉淀知识”**：
- **笔记系统 (Notebook)**：
  - 支持对任意题目编写解题笔记。
  - 内置 **Markdown** 编辑器与 **KaTeX** 数学公式渲染支持（如 `$E=mc^2$`）。
  - 支持 **Highlight.js** 代码高亮，方便记录核心代码片段。
- **自定义标签 (Custom Tags)**：用户可为题目添加自定义标签（如 `dp`, `贪心`, `补题`），方便后续检索与复习。
- **多维筛选与搜索**：支持按今日、本周、本月、全部时间段筛选，支持按题目名称、ID或标签进行模糊搜索。

### 3. 🏆 团队竞技场 (Team Rankings)

面向集训队或训练小组的内部竞争机制：
- **实时榜单**：基于 Codeforces Rating 的动态排名。
- **活跃度监控**：展示成员最近一次提交的时间（"刚刚", "2小时前"），方便教练或队长监控队员状态。
- **UI 细节**：前三名拥有专属奖牌图标与高亮样式，段位颜色与 CF 官网保持一致。

### 4. 🔄 高可靠数据同步 (Reliable Sync)

针对外网 API 不稳定性设计的健壮同步机制：
- **混合同步模式**：支持后台定时任务自动同步 + 前端用户手动触发“立即同步”。
- **智能重试策略**：内置指数退避 (Exponential Backoff) 机制，自动处理 CF API 的 503/504 错误及网络波动。
- **增量更新**：高效比对本地数据库与远程数据，仅同步新增记录，减少服务器压力。

### 5. 🛡️ 系统管理与审计 (Admin & Security)

- **AOP 操作日志**：基于 Spring AOP 全局记录关键操作（如修改用户、强制同步），确保系统安全可追溯。
- **用户管理**：管理员可管理用户状态、重置密码及查看同步任务日志。

---

## 🛠️ 技术栈 (Tech Stack)

### Frontend (前端)
- **Core**: **Vanilla JavaScript (ES6+)** - 无依赖，原生 DOM 操作，性能更优。
- **Network**: **Axios** - 现代化 Promise HTTP 客户端，统一拦截器处理。
- **Styling**: **Tailwind CSS** - 原子化 CSS 框架，构建极简且响应式的 UI。
- **Templating**: **Thymeleaf** - 服务器端 Java 模板引擎。
- **Visualization**: Apache ECharts 5.4。
- **Utils**: SweetAlert2 (弹窗), Marked (Markdown), KaTeX (Math), Highlight.js (Code).

### Backend (后端)
- **Framework**: **Spring Framework 6.1.14** (Spring MVC, Context, AOP, TX) - 纯粹的 SSM 架构。
- **Language**: **Java 11**。
- **ORM**: **MyBatis-Plus 3.5.7** - 简化 SQL 操作，极大提高开发效率。
- **Database**: MySQL 8.0。
- **Cache**: **Redis** (Spring Data Redis) - 用于 Session 共享与热点数据缓存。
- **JSON**: Jackson 2.17。
- **Tools**: Lombok, Maven 3.

---

## 📂 项目结构 (Project Structure)

```text
src/main/java/hdc/rjxy/
├── aop/            # AOP 切面 (如 AdminLogAspect 操作审计)
├── cf/             # Codeforces API 客户端 (HTTP连接、重试逻辑)
├── common/         # 通用组件 (全局异常处理、R统一返回类)
├── config/         # Spring JavaConfig (替代 web.xml/spring.xml)
│   ├── WebMvcConfig.java   # 视图解析、静态资源
│   ├── MyBatisConfig.java  # 数据库连接池、MyBatisPlus配置
│   └── ScheduleConfig.java # 定时任务配置
├── controller/     # 业务控制器
├── mapper/         # 数据持久层接口
├── pojo/           # 实体类 (DO/DTO/VO)
├── service/        # 核心业务逻辑层 (SyncService, UserSubmissionService...)
└── task/           # 定时任务调度 (SyncScheduler)
```

## 🚀 部署与运行 (Getting Started)

### 环境要求

- JDK 11+
- MySQL 8.0+
- Redis 5.0+
- Tomcat 10.1+ (必须支持 Jakarta Servlet 6.0)

### 1. 数据库配置

创建数据库 `oj_tracker`，并修改配置文件 `src/main/resources/db.properties`：

```Properties
jdbc.url=jdbc:mysql://localhost:3306/oj_tracker?useSSL=false&serverTimezone=Asia/Shanghai
jdbc.username=root
jdbc.password=YourPassword
redis.host=localhost
redis.port=6379
```

### 2. 构建项目

```Bash
# 克隆项目
git clone [https://github.com/your-username/ojtracker.git](https://github.com/your-username/ojtracker.git)

# 进入目录
cd ojtracker/oj_tracker

# Maven 打包 (生成 WAR)
mvn clean package
```

### 3. 启动运行

**方式 A: 本地开发 (IntelliJ IDEA + SmartTomcat)**

1. 配置 SmartTomcat 插件。
2. Deployment Directory 选择 `src/main/webapp`。
3. Context Path 设置为 `/oj_tracker`。

**方式 B: 生产部署**

1. 将 `target/oj_tracker.war` 复制到 Tomcat 的 `webapps` 目录下。
2. 启动 Tomcat (`bin/startup.sh`)。
3. 访问 `http://localhost:8080/oj_tracker`。