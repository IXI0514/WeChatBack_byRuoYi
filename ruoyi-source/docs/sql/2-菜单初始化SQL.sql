SET NAMES utf8mb4;

-- =====================================
-- 文件名: 2-菜单初始化SQL.sql
-- 功能: 添加小程序管理菜单(用户管理 + 接口日志 + 中继台管理)
-- 菜单ID: 2000-2009(从 2000 开始,不与基础系统菜单冲突)
-- 执行顺序: 在 1-基础数据.sql 之后执行
-- 数据库: ry-vue
-- 执行方式: mysql -uroot -p --default-character-set=utf8mb4 ry-vue < "2-菜单初始化SQL.sql"
-- =====================================

-- 1. 一级目录: 小程序管理(目录类型 M, path=miniapp, icon=people)
INSERT INTO sys_menu VALUES (2000, '小程序管理', 0, 1, 'miniapp', NULL, '', '', 1, 0, 'M', '0', '0', '', 'people', 'admin', sysdate(), '', NULL, '小程序管理目录');

-- 2. 二级菜单: 用户管理(菜单类型 C, 组件指向 miniapp/user/index, 权限标识 miniapp:user:list)
INSERT INTO sys_menu VALUES (2001, '用户管理', 2000, 1, 'user', 'miniapp/user/index', '', '', 1, 0, 'C', '0', '0', 'miniapp:user:list', 'user', 'admin', sysdate(), '', NULL, '小程序用户管理');

-- 3. 按钮: 用户管理-编辑(按钮类型 F, 权限标识 miniapp:user:edit)
INSERT INTO sys_menu VALUES (2002, '用户编辑', 2001, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'miniapp:user:edit', '#', 'admin', sysdate(), '', NULL, '');

-- 4. 按钮: 用户管理-导出(按钮类型 F, 权限标识 miniapp:user:export)
INSERT INTO sys_menu VALUES (2003, '用户导出', 2001, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'miniapp:user:export', '#', 'admin', sysdate(), '', NULL, '');

-- 5. 二级菜单: 接口日志(菜单类型 C, 组件指向 miniapp/log/index, 权限标识 miniapp:log:list)
INSERT INTO sys_menu VALUES (2004, '接口日志', 2000, 2, 'log', 'miniapp/log/index', '', '', 1, 0, 'C', '0', '0', 'miniapp:log:list', 'log', 'admin', sysdate(), '', NULL, '小程序接口日志查看');

-- 6. 按钮: 接口日志-删除/清空(按钮类型 F, 权限标识 miniapp:log:remove)
INSERT INTO sys_menu VALUES (2005, '日志删除', 2004, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'miniapp:log:remove', '#', 'admin', sysdate(), '', NULL, '');

-- 7. 二级菜单及按钮: 中继台管理
INSERT INTO sys_menu VALUES (2006, '中继台管理', 2000, 3, 'repeater', 'miniapp/repeater/index', '', '', 1, 0, 'C', '0', '0', 'miniapp:repeater:list', 'radio', 'admin', sysdate(), '', NULL, '中继台管理');
INSERT INTO sys_menu VALUES (2007, '中继台新增', 2006, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'miniapp:repeater:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES (2008, '中继台修改', 2006, 2, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'miniapp:repeater:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES (2009, '中继台删除', 2006, 3, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'miniapp:repeater:remove', '#', 'admin', sysdate(), '', NULL, '');
