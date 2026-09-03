# SmartWMS（星辰WMS）

基于 **JEECG BOOT 3.8.1** 的仓库管理系统（WMS）后端。JDK 17 + Spring Boot 3.x + MyBatis-Plus。

## 技术栈

| 层 | 选型 |
|---|---|
| 基础框架 | JEECG BOOT 3.8.1 / Spring Boot 3.x |
| JDK | 17 |
| ORM | MyBatis-Plus |
| 数据库 | MySQL 8.x（库名 `xingchenwms`），Flyway 管理脚本 |
| 缓存 | Redis（分布式发号器 / 缓存） |
| 权限 | Apache Shiro |
| 接口文档 | Knife4j（Swagger3） |
| 构建 | Maven 多模块 |

## 模块结构

```
xincheng-wms-java-course
├── jeecg-boot-base-core      # 框架核心（RedisUtil、字典切面 DictAspect、公共 Result 等）
├── jeecg-boot-module         # 框架公共模块（积木报表等）
├── jeecg-module-system       # 系统模块
│   ├── jeecg-system-api      #   API 接口定义
│   ├── jeecg-system-biz      #   业务实现（用户/角色/菜单/字典）
│   └── jeecg-system-start    #   启动模块（唯一 Spring Boot 入口）
└── jeecg-module-wms          # WMS 业务模块（本项目的主战场）
    └── org.jeecg.modules.wms
        ├── goods             # 货主 / 商品 / 品类等基础资料
        └── config            # 业务枚举
```

启动类：`org.jeecg.JeecgSystemApplication`（位于 `jeecg-module-system/jeecg-system-start`）

## 环境要求

| 组件 | 版本/地址 |
|---|---|
| JDK | 17 |
| Maven | 3.6+ |
| MySQL | `127.0.0.1:3306`，库 `xingchenwms`，默认账号 `root/root` |
| Redis | `127.0.0.1:6379` |

## 快速开始

### 1. 初始化数据库

```bash
# 建库后导入（按时间倒序取最新的那份）
mysql -uroot -proot xingchenwms < db/xingchenwms-20251114.sql
```

### 2. 修改本地配置

`jeecg-module-system/jeecg-system-start/src/main/resources/application-dev.yml`

```yaml
spring:
  datasource:
    dynamic:
      datasource:
        master:
          url: jdbc:mysql://127.0.0.1:3306/xingchenwms?...
          username: root
          password: root      # ← 改成你本机的真实密码
```

### 3. 启动

```bash
# 命令行（推荐，不受 Windows 命令行长度限制）
mvn -pl jeecg-module-system/jeecg-system-start -am spring-boot:run -DskipTests

# 或 IDEA 运行 JeecgSystemApplication
#   Windows 下需把 Shorten command line 设为 @argfile（Run → Edit Configurations）
```

### 4. 访问

| 项 | 地址 |
|---|---|
| 接口文档 | http://localhost:8080/jeecg-boot/doc.html |
| 默认账号 | `admin` / `123456` |

## 当前进度

| 模块 | 状态 | 说明 |
|---|---|---|
| 货主表 `wms_cargo_owners` | ✅ 已完成 | 低代码生成 CRUD + 自定义业务编码 |
| 货主编码生成 | ✅ 已完成 | Redis `INCR` 全局发号器，格式 `C` + 5 位序号 |
| 数据字典翻译 | ✅ 已完成 | `@Dict` 注解翻译 `status` / `settlement_currency` |

### 货主编码发号器

`WmsCargoOwnersServiceImpl#generateOwnerCode()`：

```java
code = redisUtil.incr("WMS_CARGO_OWNERS_CODE", 1);
return "C" + String.format("%05d", code);
```

`wms_cargo_owners.owner_code` 上有唯一索引 `wms_cargo_owners_unique1`，重复插入会直接失败。

> ⚠️ 已知待优化：Redis 计数器从 0 起算，与库中已有的 `C00001` 冲突。启动时应用 `MAX(owner_code)` 初始化计数器（配合 `SETNX`），并对唯一键冲突做有限次数重试。详见 `Log-README.md` 的 2026/9/3 记录。

## 相关文档

| 文件 | 用途 |
|---|---|
| [Fix-README.md](./Fix-README.md) | 环境搭建与启动问题的修复记录 |
| [Log-README.md](./Log-README.md) | 项目学习日记（含 AI 纠正与评价） |

## 常用命令

```bash
# 只编译 wms 模块及其依赖
mvn -pl jeecg-module-wms -am compile -DskipTests

# 编译启动模块
mvn -pl jeecg-module-system/jeecg-system-start -am compile -DskipTests

# 全量打包
mvn clean package -DskipTests
```

## 注意事项

- `.gitignore` 已忽略 `Fix-README.md`、`Log-README.md`、`**/doc`，这三类文件不会进版本库。
- `jeecg_config.properties` 中的 `bussi_package` 已改为 `org.jeecg.modules.wms`，`project_path` 需改成你本机的项目绝对路径，否则代码生成器会写到错误位置。
