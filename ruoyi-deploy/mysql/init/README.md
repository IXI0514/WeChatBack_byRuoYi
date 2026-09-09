# MySQL 初始化脚本目录

把若依的 SQL 脚本拷到本目录，MySQL 容器**首次启动**时会按文件名字母序自动执行。

## 需要的脚本

从若依源码 `sql/` 目录拷贝以下文件：

- `ry_20250101.sql`（或类似命名的主库脚本）—— 包含用户、角色、菜单、字典等核心数据
- `ry_config_20250101.sql`（可选）—— 配置表脚本

## 建议命名

MySQL 按文件名字母序执行，建议加数字前缀保证顺序：

```
mysql/init/
├── 01_ry_20250101.sql          # 主库脚本(建表 + 核心数据)
└── 02_ry_config_20250101.sql   # 配置表脚本(可选)
```

## 重要说明

- 这些脚本**只在 `data/mysql/` 目录为空时（即首次启动）执行**
- 如果要重新初始化，需要先停止容器、清空 `data/mysql/` 目录、再重启
- 清空数据卷命令（谨慎）：
  ```bash
  docker compose down
  rm -rf data/mysql/*
  docker compose up -d mysql
  ```

## 获取脚本

从若依官方仓库下载：
- Gitee: https://gitee.com/y_project/RuoYi-Vue
- 仓库 `sql/` 目录下有最新脚本
