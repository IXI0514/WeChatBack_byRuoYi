SET NAMES utf8mb4;

-- 中继台审核提交与名称去重：已有环境增量脚本。
-- 先执行下方重复检查；如返回记录，先在后台合并/改名，再继续创建唯一索引。
SELECT LOWER(REPLACE(TRIM(repeater_name), ' ', '')) AS name_normalized, COUNT(*) AS duplicate_count
FROM miniapp_repeater GROUP BY LOWER(REPLACE(TRIM(repeater_name), ' ', '')) HAVING COUNT(*) > 1;

-- MySQL 8：为已有主表添加名称去重键；已存在字段时跳过。
SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE miniapp_repeater ADD COLUMN name_normalized varchar(100) NULL COMMENT ''名称去重键：去空白、小写''',
  'SELECT 1') FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'miniapp_repeater' AND column_name = 'name_normalized');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE miniapp_repeater SET name_normalized = LOWER(REPLACE(TRIM(repeater_name), ' ', ''))
WHERE name_normalized IS NULL OR name_normalized = '';
SET @sql = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE miniapp_repeater ADD UNIQUE KEY uk_name_normalized (name_normalized)',
  'SELECT 1') FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'miniapp_repeater' AND index_name = 'uk_name_normalized');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS miniapp_repeater_submission (
  submission_id bigint(20) NOT NULL AUTO_INCREMENT COMMENT '提交ID',
  miniapp_id varchar(64) NOT NULL COMMENT '提交小程序标识',
  repeater_name varchar(100) NOT NULL COMMENT '中继台名称',
  name_normalized varchar(100) NOT NULL COMMENT '名称去重键',
  call_sign varchar(50) DEFAULT NULL,
  province varchar(30) NOT NULL,
  city varchar(30) NOT NULL,
  operation_mode varchar(16) NOT NULL,
  uplink_frequency_mhz decimal(10,5) NOT NULL,
  downlink_frequency_mhz decimal(10,5) NOT NULL,
  radio_config json DEFAULT NULL,
  remark varchar(500) DEFAULT NULL,
  review_status char(1) NOT NULL DEFAULT '0' COMMENT '0待审核 1通过 2驳回',
  review_remark varchar(500) DEFAULT NULL,
  reviewer varchar(64) DEFAULT NULL,
  review_time datetime DEFAULT NULL,
  repeater_id bigint(20) DEFAULT NULL COMMENT '通过后生成的中继台ID',
  create_time datetime NOT NULL,
  PRIMARY KEY (submission_id),
  UNIQUE KEY uk_submission_name_normalized (name_normalized),
  KEY idx_review_status_create_time (review_status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='小程序中继台审核提交';

-- 审核权限；需要为非管理员角色另行授权。
INSERT INTO sys_menu
SELECT 2010, '中继台审核', 2006, 4, '', NULL, '', '', 1, 0, 'F', '0', '0', 'miniapp:repeater:review', '#', 'admin', NOW(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2010);
