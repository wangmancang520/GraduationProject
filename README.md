# 蓝星智能教室 · 后端

基于 Spring Boot、MyBatis-Plus 的本科毕业设计，提供教室管理、预约审核、用户管理和站内通知 API。本仓库在原手写项目基础上整理了运行配置、修复业务错误，并补齐测试与演示数据。

[前端仓库](https://github.com/wangmancang520/GraduationProjectWeb) · [原始后端](https://github.com/Kaolennoodle/GraduationProject)

## 功能与角色

| 角色 | 类型值 | 已实现能力 |
| --- | --- | --- |
| 学生 / 教职工 | 1 / 2 | 注册登录、查看教室与预约时间、创建及修改自己的预约、个人资料、消息已读 |
| 教室管理员 | 3 | 维护本人管理的教室、审核与拒绝预约、查看预约和通知 |
| 系统管理员 | 4 | 用户与教室管理、预约管理、重置用户密码、统计信息 |

预约状态为待开始 `1`、进行中 `2`、已过期 `3`、已失效 `4`；审核状态为待审核 `1`、通过 `2`、拒绝 `3`。定时任务每分钟更新预约状态。相邻预约可以共享边界，例如 09:00–10:00 与 10:00–11:00 不冲突；重叠预约会被拒绝。

设备控制页面和部分系统设置是原项目的预留入口，**尚未接入真实教室硬件**。旧 WebSocket 聊天原型默认关闭，也不属于已验证的业务流程。

## 技术栈

- Java 17、Maven 3.9+、Spring Boot 2.7.18
- MyBatis-Plus 3.5.1、MySQL 8（正常模式）/ H2（演示及测试）
- BCrypt 密码哈希、JWT 登录凭证（2 小时有效，密码变更使旧凭证失效）
- JUnit 5、MockMvc；Swagger 接口文档

该版本保留原项目架构，适合学习和演示；Spring Boot 2.x 与前端 Vue 2 属于历史技术栈，正式部署前需要进一步升级依赖并完成安全评审。

## 最快体验：无需安装数据库

```bash
git clone https://github.com/wangmancang520/GraduationProject.git
cd GraduationProject
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

后端地址为 `http://localhost:8081`。然后按照[前端 README](https://github.com/wangmancang520/GraduationProjectWeb#readme)启动网页。

演示账号仅在 `demo` 模式创建，密码统一为 `Demo12345`：

| 登录名 | 角色 |
| --- | --- |
| manager@example.com | 系统管理员 |
| teacher@example.com | 教室管理员 |
| student@example.com | 学生 |

演示模式自动建立内存数据库和一间教室，**进程重启后数据清空**。不要在公网提供带有这些演示账号的服务。

也可把前后端克隆到同一父目录，以 Docker Compose 启动本地完整演示：

```bash
# 当前目录为 GraduationProject，旁边需要有 GraduationProjectWeb
docker compose up --build
```

浏览器打开 `http://localhost:8080`。容器端口仅绑定本机。

## 使用 MySQL

1. 创建 UTF-8 数据库 `blue_star` 和专用数据库账号，授予该库的读写权限。
2. 在**空数据库**执行 `db/schema.sql`。这份结构根据实体和查询重建，不包含旧账号或历史数据，也不是已有数据库的升级脚本。
3. 参考 `.env.example` 设置进程环境变量。Spring Boot 不会自动加载 `.env`，请通过终端、IDE 或容器注入。

```bash
export DB_URL='jdbc:mysql://localhost:3306/blue_star?serverTimezone=Asia/Shanghai&characterEncoding=utf8'
export DB_USERNAME='blue_star'
# 在终端设置 DB_PASSWORD，使用你自己的数据库密码
# 首次运行时另外设置 BOOTSTRAP_ADMIN_EMAIL 和 BOOTSTRAP_ADMIN_PASSWORD
mvn spring-boot:run
```

`BOOTSTRAP_ADMIN_PASSWORD` 至少 12 个字符。初始化逻辑只创建不存在的登录名，不修改已有账号；创建成功后移除这两个初始化变量。正常模式不会生成演示账号。旧数据库中的明文密码不能直接用于此版本登录，应通过受控迁移重新设置为 BCrypt 哈希。

| 环境变量 | 默认值 / 用途 |
| --- | --- |
| SERVER_PORT | `8081` |
| DB_URL | 本地 `blue_star` 数据库 |
| DB_USERNAME / DB_PASSWORD | `blue_star` / 空，实际密码由环境提供 |
| UPLOAD_PATH | `./uploads` |
| FILE_PUBLIC_URL | `http://localhost:8081/file`，返回给前端的文件访问前缀 |
| CORS_ORIGINS | `http://localhost:8080`，多个来源用逗号分隔 |
| BOOTSTRAP_ADMIN_EMAIL / BOOTSTRAP_ADMIN_PASSWORD | 仅首次初始化系统管理员时设置 |

使用统一域名反向代理时，将 `FILE_PUBLIC_URL` 设为 `/api/file`。服务器需持久化上传目录；MySQL 数据持久化由部署环境负责。

## API 约定

登录：`POST /login`，注册：`POST /user/register`。JSON 字段沿用原前端约定，如 `uloginName`、`upassword`、`uid`、`utype`。

```json
{"uloginName":"student@example.com","upassword":"Demo12345"}
```

登录后的 API 在请求头传入 `token: <JWT>`。大多数接口返回 `{ "code": "200", "msg": "", "data": ... }`；原有部分 CRUD 保留布尔值、数组或分页响应，前端已兼容。认证失败和越权分别返回 HTTP 401 / 403。用户实体响应不包含密码或哈希。

| 接口 | 用途 |
| --- | --- |
| GET /classroom/page | 教室分页 |
| POST /appointment/new | 新建预约，参数 uid、cid、date、startTime、endTime |
| POST /appointment/edit | 编辑预约，另传 aid |
| POST /appointment/approve/{a_id} | 审核通过 |
| POST /appointment/reject | 拒绝预约，参数 a_id、rejectReason |
| GET /appointment/page | 按用户、教室、日期及时间筛选预约 |
| POST /user/change-password | 验证旧密码并更新密码 |
| GET /message/get-by-receiver/{uid} | 获取自己的通知 |
| POST /file/upload | 上传 2MB 以内 JPG / PNG / WEBP 头像，需要登录 |

预约写操作使用 POST，参数仍采用查询参数，兼容本次同步更新的前端。日期可传 `yyyy-MM-dd` 或 ISO 时间戳，起止时间传 ISO 时间戳，按 `Asia/Shanghai` 校园时区组合日期与时间。文件下载通过不可预测的 UUID URL 提供给头像组件，不要求登录；不要把此接口当作私密文档存储。

API 文档：启动后访问 `http://localhost:8081/swagger-ui/index.html`。

## 目录

```text
src/main/java/com/example/springboot/
  controller/   HTTP API 与参数
  service/      业务、权限、预约和通知
  mapper/       MyBatis-Plus 数据访问
  entity/       数据实体
  config/       认证、跨域、分页、演示数据、定时任务
src/main/resources/  环境配置与演示建表脚本
db/schema.sql         MySQL 空库初始化
src/test/            回归测试
```

## 验证与维护

```bash
mvn test      # 自动使用独立 H2 演示环境，无需连接旧数据库
mvn verify    # 编译、测试并打包
java -jar target/springboot-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo
```

测试覆盖登录与非法令牌、公开注册与角色保护、密码不泄露、密码变更撤销旧令牌、预约创建/审核/通知、跨月跨年日期、相邻时间边界及大 ID 自身编辑。GitHub Actions 自动执行 `mvn verify`。仓库内 `.mvn/` 将依赖解析统一到 Maven Central，避免旧依赖引入已失效的快照仓库。

本轮修复还包括：移除写死的旧数据库连接和 Windows 路径；修复单例中共享查询条件；用去重统计和 SQL 时间范围筛选替代错误计数与全表内存过滤；同教室预约写入加事务与行锁；文件下载改为流式传输。权限控制位于后端，前端菜单与路由限制仅用于交互。

仍保留的边界：未完成硬件控制、邮件找回密码、分页外接口的全面重构、审计日志、登录限流和大规模并发压测；MySQL 部署应单独验证。旧重置密码功能仅系统管理员可用，仍会重置为固定初始密码，应要求用户立即更改，生产化时需替换为一次性重置流程。

## 项目来源

原作者账号：[Kaolennoodle](https://github.com/Kaolennoodle)。整理基线：`28521d55f7a1c6c359f913f5b295cd8c0c005975`。

新仓库使用整理后的源码快照，避免重复发布旧凭证与生成文件；原始提交历史仍可在旧仓库查阅。原仓库没有许可证，本次未擅自添加开源授权；使用或分发时请先确认授权。
