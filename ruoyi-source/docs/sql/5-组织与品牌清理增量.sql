-- ============================================================
-- 文件名: 5-组织与品牌清理增量.sql
-- 用途: 已部署环境的默认组织、菜单入口、公告和展示名称清理
-- 适用: 已执行过基础数据 SQL 的数据库；可重复执行
--
-- 执行前:
--   1. 先备份目标数据库。
--   2. 本脚本仅收敛默认部门 ID 100-109；自建部门不会被删除。
--   3. 如需保留旧部门，请删除本脚本中“删除旧默认部门”一段再执行。
-- ============================================================

START TRANSACTION;

-- 保留原有用户和角色的数据可见范围：
-- 先为关联旧默认部门的角色补充根部门，再清理旧关联。
INSERT IGNORE INTO sys_role_dept (role_id, dept_id)
SELECT DISTINCT role_id, 100
FROM sys_role_dept
WHERE dept_id IN (103, 104, 105, 106, 107, 108, 109);

-- 原默认叶子部门中的用户统一归属到平台管理中心。
UPDATE sys_user
SET dept_id = 100,
    update_by = 'admin',
    update_time = NOW()
WHERE dept_id IN (103, 104, 105, 106, 107, 108, 109);

-- 若旧默认部门下存在后续创建的子部门，先挂到根节点，避免产生孤儿节点。
UPDATE sys_dept
SET parent_id = 100,
    ancestors = '0,100',
    update_by = 'admin',
    update_time = NOW()
WHERE parent_id IN (103, 104, 105, 106, 107, 108, 109)
  AND dept_id NOT IN (103, 104, 105, 106, 107, 108, 109);

-- 三个保留节点：平台管理中心、业务运营部、技术支持部。
UPDATE sys_dept
SET parent_id = 0, ancestors = '0', dept_name = '平台管理中心', order_num = 0,
    leader = '系统管理员', phone = '', email = '', status = '0', del_flag = '0',
    update_by = 'admin', update_time = NOW()
WHERE dept_id = 100;

UPDATE sys_dept
SET parent_id = 100, ancestors = '0,100', dept_name = '业务运营部', order_num = 1,
    leader = '', phone = '', email = '', status = '0', del_flag = '0',
    update_by = 'admin', update_time = NOW()
WHERE dept_id = 101;

UPDATE sys_dept
SET parent_id = 100, ancestors = '0,100', dept_name = '技术支持部', order_num = 2,
    leader = '', phone = '', email = '', status = '0', del_flag = '0',
    update_by = 'admin', update_time = NOW()
WHERE dept_id = 102;

DELETE FROM sys_role_dept
WHERE dept_id IN (103, 104, 105, 106, 107, 108, 109);

-- 删除初始组织中的旧叶子部门；不会删除 ID 不在该列表内的自建部门。
DELETE FROM sys_dept
WHERE dept_id IN (103, 104, 105, 106, 107, 108, 109);

-- 移除外部框架官网菜单及其角色关联，保留其他菜单权限。
DELETE FROM sys_role_menu WHERE menu_id = 4;
DELETE FROM sys_menu WHERE menu_id = 4;

-- 清理默认展示名称与公告内容，不改动账号 admin 的登录密码。
UPDATE sys_user
SET nick_name = '系统管理员',
    update_by = 'admin',
    update_time = NOW()
WHERE user_id = 1;

UPDATE sys_user
SET nick_name = '演示用户',
    update_by = 'admin',
    update_time = NOW()
WHERE user_id = 2;

UPDATE sys_notice
SET notice_title = '系统使用提醒',
    notice_content = '请妥善保管账号信息，并按需维护组织与权限数据。',
    update_by = 'admin',
    update_time = NOW()
WHERE notice_id = 1;

UPDATE sys_notice
SET notice_title = '系统维护通知',
    notice_content = '进行部署、升级或数据维护前，请先完成数据库备份。',
    update_by = 'admin',
    update_time = NOW()
WHERE notice_id = 2;

UPDATE sys_notice
SET notice_title = '平台使用说明',
    notice_content = '<p>本平台用于档案与基础系统管理。请根据实际业务维护用户、角色、组织和菜单权限。</p>',
    update_by = 'admin',
    update_time = NOW()
WHERE notice_id = 3;

COMMIT;

-- 执行后核验（应只返回 100、101、102 及后续自建部门）：
-- SELECT dept_id, parent_id, ancestors, dept_name FROM sys_dept ORDER BY parent_id, order_num, dept_id;
-- SELECT user_id, user_name, nick_name, dept_id FROM sys_user WHERE user_id IN (1, 2);
-- SELECT role_id, dept_id FROM sys_role_dept ORDER BY role_id, dept_id;
