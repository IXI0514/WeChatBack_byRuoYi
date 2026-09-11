-- 未配置/留空为不过期；仅补充参数，不覆盖已有设置。
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '小程序Token有效期（分钟）', 'miniapp.token.expire.minutes', '', 'Y', 'admin', NOW(), '正整数分钟；留空或未配置不过期。修改后按新规则校验已有token，不影响后台JWT。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'miniapp.token.expire.minutes');
