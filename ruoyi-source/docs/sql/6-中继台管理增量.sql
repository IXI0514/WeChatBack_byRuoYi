-- 中继台管理：已部署环境增量脚本，可重复执行

CREATE TABLE IF NOT EXISTS miniapp_repeater (
  repeater_id bigint(20) NOT NULL AUTO_INCREMENT COMMENT '中继台ID',
  repeater_name varchar(100) NOT NULL COMMENT '中继台名称',
  call_sign varchar(50) DEFAULT NULL COMMENT '呼号',
  province varchar(30) NOT NULL COMMENT '省份',
  city varchar(30) NOT NULL COMMENT '城市',
  operation_mode varchar(16) NOT NULL COMMENT 'ANALOG、DIGITAL、MIXED',
  uplink_frequency_mhz decimal(10,5) NOT NULL COMMENT '上行频率MHz',
  downlink_frequency_mhz decimal(10,5) NOT NULL COMMENT '下行频率MHz',
  status char(1) NOT NULL DEFAULT '0' COMMENT '0正常 1维护 2停用',
  is_public char(1) NOT NULL DEFAULT '0' COMMENT '0公开 1后台保留',
  last_verified_at datetime DEFAULT NULL COMMENT '最后核验时间',
  radio_config json DEFAULT NULL COMMENT '模拟、数字及射频详细配置',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  create_by varchar(64) DEFAULT '' COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT '' COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (repeater_id),
  KEY idx_region (province, city),
  KEY idx_name (repeater_name),
  KEY idx_call_sign (call_sign),
  KEY idx_mode_status (operation_mode, status, is_public)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='小程序中继台';

-- 菜单与按钮；管理员角色不需要额外关联。普通角色需要时请按权限策略另行授权。
INSERT INTO sys_menu
SELECT 2006, '中继台管理', 2000, 3, 'repeater', 'miniapp/repeater/index', '', '', 1, 0, 'C', '0', '0', 'miniapp:repeater:list', 'radio', 'admin', NOW(), '', NULL, '中继台管理'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2006);
INSERT INTO sys_menu
SELECT 2007, '中继台新增', 2006, 1, '', NULL, '', '', 1, 0, 'F', '0', '0', 'miniapp:repeater:add', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2007);
INSERT INTO sys_menu
SELECT 2008, '中继台修改', 2006, 2, '', NULL, '', '', 1, 0, 'F', '0', '0', 'miniapp:repeater:edit', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2008);
INSERT INTO sys_menu
SELECT 2009, '中继台删除', 2006, 3, '', NULL, '', '', 1, 0, 'F', '0', '0', 'miniapp:repeater:remove', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2009);
