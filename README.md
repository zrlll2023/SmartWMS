# 2026-09-03 启动修复记录（xincheng-wms）

> 原 README 已归档，本文件仅记录本次使项目可启动的全部修改。

## 1. 问题现象
- `JeecgSystemApplication` 启动报 `Access denied for user 'root'` 连不上 `xingchenwms`。
- `mvn compile` 告警 `minidao-spring-boot-starter-jsqlparser-4.9` 重复声明。
- Windows + IDEA 2024.3（JDK 17）运行报 `命令行过长。缩短命令行并重新运行`，切 `classpath file` 后再报 `CommandLineWrapper is ill-suited for Java 9+`。

## 2. 根因
| # | 位置 | 根因 |
|---|---|---|
| 1 | `jeecg-system-start/.../application-dev.yml` | `master.password=mysql` 与本机 MySQL 真实密码 `root` 不一致 |
| 2 | `pom.xml` | `dependencyManagement` 中 `minidao-spring-boot-starter-jsqlparser-4.9` 重复声明两遍，构建告警 |
| 3 | `.idea/workspace.xml` | Windows classpath 超长（`JeecgSystemApplication` 依赖多），需缩短命令行；JDK17 下 `classpath file` 的 `CommandLineWrapper` 已废弃，须用 `@argfile`/`JAR manifest` |

## 3. 修改清单
| 文件 | 变更 |
|---|---|
| `jeecg-module-system/jeecg-system-start/src/main/resources/application-dev.yml:155` | `password: mysql → root` |
| `pom.xml:467` | 删除重复的 `minidao-spring-boot-starter-jsqlparser-4.9` 声明（保留一处） |
| `.idea/workspace.xml:JeecgSystemApplication` | 新增 `<option name="SHORTEN_COMMAND_LINE" value="ARGS_FILE" />`（等价 IDEA: Run → Edit Configurations → Shorten command line → `@argfile`） |

> `.idea/` 被 `.gitignore` 忽略，该项为本地配置；新克隆需手动在 IDEA 选 `@argfile` 或 `JAR manifest`，或改用 `mvn spring-boot:run` 启动。

## 4. 验证
- MySQL `127.0.0.1:3306 root/root` 存在 `xingchenwms` 库且有表，Redis `127.0.0.1:6379` PONG
- `mvn -pl jeecg-module-wms -am compile -DskipTests` → BUILD SUCCESS
- `mvn -pl jeecg-module-system/jeecg-system-start -am compile -DskipTests` → BUILD SUCCESS

## 5. 启动方式
1. 确保 MySQL/Redis 已启动
2. 方式一（IDEA）：直接 Run `org.jeecg.JeecgSystemApplication`（Shorten 已设为 `@argfile`）
3. 方式二（命令行）：`mvn -pl jeecg-module-system/jeecg-system-start -am spring-boot:run -DskipTests`
4. 访问 `http://localhost:8080/jeecg-boot/doc.html`
