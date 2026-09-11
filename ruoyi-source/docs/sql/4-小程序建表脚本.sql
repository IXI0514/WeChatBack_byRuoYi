SET NAMES utf8mb4;

-- =====================================
-- 小程序模块建表脚本
-- 表: miniapp_user(小程序用户) + miniapp_api_log(接口日志)
-- 配置: sys_config 的 miniapp.valid.ids
-- 数据库: ry-vue
-- 执行方式: mysql -uroot -p --default-character-set=utf8mb4 ry-vue < miniapp_init.sql
-- =====================================

-- 1. 小程序用户表
DROP TABLE IF EXISTS `miniapp_user`;
CREATE TABLE `miniapp_user` (
  `user_id`          bigint(20)    NOT NULL AUTO_INCREMENT  COMMENT '用户ID',
  `miniapp_id`       varchar(64)   NOT NULL                 COMMENT '小程序标识',
  `openid`           varchar(64)   NOT NULL                 COMMENT '用户openid',
  `wx_id`            varchar(64)   NOT NULL                 COMMENT '微信id(union_id,方便管理)',
  `nickname`         varchar(64)   DEFAULT NULL             COMMENT '昵称(可选)',
  `is_member`        tinyint(1)    NOT NULL DEFAULT 0       COMMENT '是否会员 0否 1是',
  `member_expire`    datetime      DEFAULT NULL             COMMENT '会员到期时间(null=非会员或永久)',
  `status`           char(1)       NOT NULL DEFAULT '0'     COMMENT '状态 0正常 1停用',
  `create_time`      datetime      NOT NULL                 COMMENT '创建时间',
  `update_time`      datetime      NOT NULL                 COMMENT '更新时间',
  `remark`           varchar(500)  DEFAULT NULL             COMMENT '备注(后台管理用)',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_miniapp_openid` (`miniapp_id`, `openid`),
  KEY `idx_wx_id` (`wx_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='小程序用户表';

-- 2. 接口日志表
DROP TABLE IF EXISTS `miniapp_api_log`;
CREATE TABLE `miniapp_api_log` (
  `log_id`           bigint(20)    NOT NULL AUTO_INCREMENT  COMMENT '日志ID',
  `type`             varchar(50)   NOT NULL                 COMMENT '日志类型(接口日志)',
  `des`              varchar(200)  NOT NULL                 COMMENT '接口名称',
  `result`           varchar(50)   NOT NULL                 COMMENT '请求结果(成功/失败/拒绝)',
  `details`          text                                   COMMENT '接口入参记录(JSON格式)',
  `miniapp_id`       varchar(64)   DEFAULT NULL             COMMENT '小程序标识',
  `openid`           varchar(64)   DEFAULT NULL             COMMENT '用户openid',
  `req_url`          varchar(500)  DEFAULT NULL             COMMENT '请求URL',
  `resp_time`        int(11)       DEFAULT NULL             COMMENT '响应耗时(毫秒)',
  `ip`               varchar(50)   DEFAULT NULL             COMMENT '请求IP',
  `create_time`      datetime      NOT NULL                 COMMENT '创建时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_miniapp_openid` (`miniapp_id`, `openid`),
  KEY `idx_type` (`type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='接口日志表';

-- 3. 配置: 小程序白名单标识(复用基础 sys_config 表)
INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `remark`)
SELECT '小程序白名单标识', 'miniapp.valid.ids', 'wx_hnml,wx_ham', 'Y', 'admin', NOW(), '逗号分隔的小程序标识列表,配置存在才执行业务逻辑'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'miniapp.valid.ids');

-- 未配置/留空为不过期；仅补充参数，不覆盖已有设置。
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '小程序Token有效期（分钟）', 'miniapp.token.expire.minutes', '', 'Y', 'admin', NOW(), '正整数分钟；留空或未配置不过期。修改后按新规则校验已有token，不影响后台JWT。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'miniapp.token.expire.minutes');
