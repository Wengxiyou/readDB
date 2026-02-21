# SQLite数据库编辑器

这是一个用Java Swing开发的SQLite数据库编辑器，提供了全面的数据库操作和管理功能。

## 功能特点

### 数据库操作
- 新建SQLite数据库文件（.db）
- 打开和连接已有的SQLite数据库文件
- 保存和优化数据库（执行VACUUM操作）
- 断开数据库连接

### SQL查询
- 执行各种SQL语句（SELECT、INSERT、UPDATE、DELETE、CREATE TABLE等）
- 表格形式显示查询结果
- 自动调整列宽以适应内容

### 表管理
- 浏览数据库中的所有表
- 查看表结构信息（列名、数据类型、约束等）
- 查看表索引信息
- 一键查看表数据内容
- 自动刷新表列表（当创建或修改表时）

### 用户界面
- 中文字体支持，确保中文显示正常
- 美观的代码编辑器（使用等宽字体）
- 直观的菜单和工具栏
- 标签页式界面，方便在不同功能间切换

## 所需依赖

要运行此程序，您需要下载SQLite JDBC驱动：

1. 访问 [SQLite JDBC驱动下载页面](https://github.com/xerial/sqlite-jdbc/releases)
2. 下载最新版本的 `sqlite-jdbc-x.x.x.jar` 文件（建议使用3.40.0或更高版本）
3. 将下载的jar文件放在与编译后的程序相同的目录中

## 编译和运行

### 打包
```
mvn package
```

### 
```
mvn package
```



## 使用说明

### 基本操作
1. **打开数据库**：点击"打开数据库"按钮或通过文件菜单选择一个SQLite数据库文件（.db）
2. **新建数据库**：点击"新建数据库"按钮，选择保存位置并输入文件名
3. **执行SQL查询**：在SQL查询区域输入SQL语句，点击"执行查询"按钮
4. **查看表结构**：切换到"表结构"标签页，选择一个表，点击"查看表结构"按钮
5. **查看表数据**：切换到"表结构"标签页，选择一个表，点击"查看表数据"按钮
6. **保存数据库**：点击"保存"按钮优化数据库

### 示例SQL语句
- 创建表：`CREATE TABLE users (id INTEGER PRIMARY KEY, name TEXT NOT NULL, age INTEGER);`
- 插入数据：`INSERT INTO users (name, age) VALUES ('张三', 25);`
- 查询数据：`SELECT * FROM users;`
- 更新数据：`UPDATE users SET age = 26 WHERE name = '张三';`
- 删除数据：`DELETE FROM users WHERE id = 1;`

## 常见问题

### 程序无法启动，提示找不到SQLite JDBC驱动
请确保您已下载SQLite JDBC驱动并放在正确的位置。启动脚本会自动检测驱动文件，如果找不到会给出提示。

### 执行SQL语句时出错
- 检查SQL语句语法是否正确
- 确保表名和列名正确
- 对于修改表结构的操作，确保有适当的权限

### 中文显示异常
程序已设置中文字体支持，如果仍有显示问题，请检查您的系统字体设置。

## 系统要求

- Java 8或更高版本（JDK或JRE均可）
- SQLite JDBC驱动
- Windows、Linux或macOS操作系统

## 技术说明

- 使用Java Swing构建图形用户界面
- 使用JDBC连接和操作SQLite数据库
- 支持SQLite特有的PRAGMA命令查询表结构和索引信息