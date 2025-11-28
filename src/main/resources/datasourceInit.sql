DROP TABLE IF EXISTS QRTZ_FIRED_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_PAUSED_TRIGGER_GRPS;
DROP TABLE IF EXISTS QRTZ_SCHEDULER_STATE;
DROP TABLE IF EXISTS QRTZ_LOCKS;
DROP TABLE IF EXISTS QRTZ_SIMPLE_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_SIMPROP_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_CRON_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_BLOB_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_JOB_DETAILS;
DROP TABLE IF EXISTS QRTZ_CALENDARS;

CREATE TABLE QRTZ_JOB_DETAILS(
                                 SCHED_NAME VARCHAR(120) NOT NULL,
                                 JOB_NAME VARCHAR(190) NOT NULL,
                                 JOB_GROUP VARCHAR(190) NOT NULL,
                                 DESCRIPTION VARCHAR(250) NULL,
                                 JOB_CLASS_NAME VARCHAR(250) NOT NULL,
                                 IS_DURABLE VARCHAR(1) NOT NULL,
                                 IS_NONCONCURRENT VARCHAR(1) NOT NULL,
                                 IS_UPDATE_DATA VARCHAR(1) NOT NULL,
                                 REQUESTS_RECOVERY VARCHAR(1) NOT NULL,
                                 JOB_DATA BLOB NULL,
                                 PRIMARY KEY (SCHED_NAME,JOB_NAME,JOB_GROUP))
    ENGINE=InnoDB;

CREATE TABLE QRTZ_TRIGGERS (
                               SCHED_NAME VARCHAR(120) NOT NULL,
                               TRIGGER_NAME VARCHAR(190) NOT NULL,
                               TRIGGER_GROUP VARCHAR(190) NOT NULL,
                               JOB_NAME VARCHAR(190) NOT NULL,
                               JOB_GROUP VARCHAR(190) NOT NULL,
                               DESCRIPTION VARCHAR(250) NULL,
                               NEXT_FIRE_TIME BIGINT(13) NULL,
                               PREV_FIRE_TIME BIGINT(13) NULL,
                               PRIORITY INTEGER NULL,
                               TRIGGER_STATE VARCHAR(16) NOT NULL,
                               TRIGGER_TYPE VARCHAR(8) NOT NULL,
                               START_TIME BIGINT(13) NOT NULL,
                               END_TIME BIGINT(13) NULL,
                               CALENDAR_NAME VARCHAR(190) NULL,
                               MISFIRE_INSTR SMALLINT(2) NULL,
                               JOB_DATA BLOB NULL,
                               PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
                               FOREIGN KEY (SCHED_NAME,JOB_NAME,JOB_GROUP)
                                   REFERENCES QRTZ_JOB_DETAILS(SCHED_NAME,JOB_NAME,JOB_GROUP))
    ENGINE=InnoDB;

CREATE TABLE QRTZ_SIMPLE_TRIGGERS (
                                      SCHED_NAME VARCHAR(120) NOT NULL,
                                      TRIGGER_NAME VARCHAR(190) NOT NULL,
                                      TRIGGER_GROUP VARCHAR(190) NOT NULL,
                                      REPEAT_COUNT BIGINT(7) NOT NULL,
                                      REPEAT_INTERVAL BIGINT(12) NOT NULL,
                                      TIMES_TRIGGERED BIGINT(10) NOT NULL,
                                      PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
                                      FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
                                          REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP))
    ENGINE=InnoDB;

CREATE TABLE QRTZ_CRON_TRIGGERS (
                                    SCHED_NAME VARCHAR(120) NOT NULL,
                                    TRIGGER_NAME VARCHAR(190) NOT NULL,
                                    TRIGGER_GROUP VARCHAR(190) NOT NULL,
                                    CRON_EXPRESSION VARCHAR(120) NOT NULL,
                                    TIME_ZONE_ID VARCHAR(80),
                                    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
                                    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
                                        REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP))
    ENGINE=InnoDB;

CREATE TABLE QRTZ_SIMPROP_TRIGGERS
(
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(190) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    STR_PROP_1 VARCHAR(512) NULL,
    STR_PROP_2 VARCHAR(512) NULL,
    STR_PROP_3 VARCHAR(512) NULL,
    INT_PROP_1 INT NULL,
    INT_PROP_2 INT NULL,
    LONG_PROP_1 BIGINT NULL,
    LONG_PROP_2 BIGINT NULL,
    DEC_PROP_1 NUMERIC(13,4) NULL,
    DEC_PROP_2 NUMERIC(13,4) NULL,
    BOOL_PROP_1 VARCHAR(1) NULL,
    BOOL_PROP_2 VARCHAR(1) NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP))
    ENGINE=InnoDB;

CREATE TABLE QRTZ_BLOB_TRIGGERS (
                                    SCHED_NAME VARCHAR(120) NOT NULL,
                                    TRIGGER_NAME VARCHAR(190) NOT NULL,
                                    TRIGGER_GROUP VARCHAR(190) NOT NULL,
                                    BLOB_DATA BLOB NULL,
                                    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
                                    INDEX (SCHED_NAME,TRIGGER_NAME, TRIGGER_GROUP),
                                    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
                                        REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP))
    ENGINE=InnoDB;

CREATE TABLE QRTZ_CALENDARS (
                                SCHED_NAME VARCHAR(120) NOT NULL,
                                CALENDAR_NAME VARCHAR(190) NOT NULL,
                                CALENDAR BLOB NOT NULL,
                                PRIMARY KEY (SCHED_NAME,CALENDAR_NAME))
    ENGINE=InnoDB;

CREATE TABLE QRTZ_PAUSED_TRIGGER_GRPS (
                                          SCHED_NAME VARCHAR(120) NOT NULL,
                                          TRIGGER_GROUP VARCHAR(190) NOT NULL,
                                          PRIMARY KEY (SCHED_NAME,TRIGGER_GROUP))
    ENGINE=InnoDB;

CREATE TABLE QRTZ_FIRED_TRIGGERS (
                                     SCHED_NAME VARCHAR(120) NOT NULL,
                                     ENTRY_ID VARCHAR(95) NOT NULL,
                                     TRIGGER_NAME VARCHAR(190) NOT NULL,
                                     TRIGGER_GROUP VARCHAR(190) NOT NULL,
                                     INSTANCE_NAME VARCHAR(190) NOT NULL,
                                     FIRED_TIME BIGINT(13) NOT NULL,
                                     SCHED_TIME BIGINT(13) NOT NULL,
                                     PRIORITY INTEGER NOT NULL,
                                     STATE VARCHAR(16) NOT NULL,
                                     JOB_NAME VARCHAR(190) NULL,
                                     JOB_GROUP VARCHAR(190) NULL,
                                     IS_NONCONCURRENT VARCHAR(1) NULL,
                                     REQUESTS_RECOVERY VARCHAR(1) NULL,
                                     PRIMARY KEY (SCHED_NAME,ENTRY_ID))
    ENGINE=InnoDB;

CREATE TABLE QRTZ_SCHEDULER_STATE (
                                      SCHED_NAME VARCHAR(120) NOT NULL,
                                      INSTANCE_NAME VARCHAR(190) NOT NULL,
                                      LAST_CHECKIN_TIME BIGINT(13) NOT NULL,
                                      CHECKIN_INTERVAL BIGINT(13) NOT NULL,
                                      PRIMARY KEY (SCHED_NAME,INSTANCE_NAME))
    ENGINE=InnoDB;

CREATE TABLE QRTZ_LOCKS (
                            SCHED_NAME VARCHAR(120) NOT NULL,
                            LOCK_NAME VARCHAR(40) NOT NULL,
                            PRIMARY KEY (SCHED_NAME,LOCK_NAME))
    ENGINE=InnoDB;

CREATE INDEX IDX_QRTZ_J_REQ_RECOVERY ON QRTZ_JOB_DETAILS(SCHED_NAME,REQUESTS_RECOVERY);
CREATE INDEX IDX_QRTZ_J_GRP ON QRTZ_JOB_DETAILS(SCHED_NAME,JOB_GROUP);

CREATE INDEX IDX_QRTZ_T_J ON QRTZ_TRIGGERS(SCHED_NAME,JOB_NAME,JOB_GROUP);
CREATE INDEX IDX_QRTZ_T_JG ON QRTZ_TRIGGERS(SCHED_NAME,JOB_GROUP);
CREATE INDEX IDX_QRTZ_T_C ON QRTZ_TRIGGERS(SCHED_NAME,CALENDAR_NAME);
CREATE INDEX IDX_QRTZ_T_G ON QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_GROUP);
CREATE INDEX IDX_QRTZ_T_STATE ON QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_N_STATE ON QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP,TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_N_G_STATE ON QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_GROUP,TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_NEXT_FIRE_TIME ON QRTZ_TRIGGERS(SCHED_NAME,NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_ST ON QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_STATE,NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_MISFIRE ON QRTZ_TRIGGERS(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_ST_MISFIRE ON QRTZ_TRIGGERS(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME,TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_NFT_ST_MISFIRE_GRP ON QRTZ_TRIGGERS(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME,TRIGGER_GROUP,TRIGGER_STATE);

CREATE INDEX IDX_QRTZ_FT_TRIG_INST_NAME ON QRTZ_FIRED_TRIGGERS(SCHED_NAME,INSTANCE_NAME);
CREATE INDEX IDX_QRTZ_FT_INST_JOB_REQ_RCVRY ON QRTZ_FIRED_TRIGGERS(SCHED_NAME,INSTANCE_NAME,REQUESTS_RECOVERY);
CREATE INDEX IDX_QRTZ_FT_J_G ON QRTZ_FIRED_TRIGGERS(SCHED_NAME,JOB_NAME,JOB_GROUP);
CREATE INDEX IDX_QRTZ_FT_JG ON QRTZ_FIRED_TRIGGERS(SCHED_NAME,JOB_GROUP);
CREATE INDEX IDX_QRTZ_FT_T_G ON QRTZ_FIRED_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP);
CREATE INDEX IDX_QRTZ_FT_TG ON QRTZ_FIRED_TRIGGERS(SCHED_NAME,TRIGGER_GROUP);

create table config
(
    id bigint auto_increment comment '自增id',
    env varchar(10) not null comment '环境',
    application varchar(50) null comment '应用名',
    name varchar(50) not null comment '配置名',
    `desc` varchar(255) null comment '配置描述',
    value varchar(5000) not null comment '配置值',
    type varchar(10) not null comment '类型',
    `sensitive` varchar(10) not null comment '是否敏感',
    status varchar(10) not null comment '状态',
    catalog varchar(50) null comment '目录',
    created_at timestamp default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at timestamp default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP not null comment '更新时间',
    created_by varchar(50) null comment '创建人',
    updated_by varchar(50) null comment '更新人',
    constraint config_pk
        primary key (id),
    constraint idx_name_env
        unique (name, env)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 comment '配置表';



create table config_history
(
    id bigint auto_increment comment '自增id',
    env varchar(10) not null comment '环境',
    application varchar(50) null comment '应用名',
    name varchar(50) not null comment '配置名',
    `desc` varchar(255) null comment '配置描述',
    value varchar(5000) not null comment '配置值',
    type varchar(10) not null comment '类型',
    `sensitive` varchar(10) not null comment '是否敏感',
    status varchar(10) not null comment '状态',
    catalog varchar(50) null comment '目录',
    created_at timestamp default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at timestamp default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP not null comment '更新时间',
    created_by varchar(50) null comment '创建人',
    updated_by varchar(50) null comment '更新人',
    constraint config_pk
        primary key (id),
    constraint idx_name_env
        unique (name, env)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 comment '配置历史表';


-- 用户表
CREATE TABLE `users` (
                         `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                         `username` varchar(50) NOT NULL COMMENT '用户名',
                         `email` varchar(100) NOT NULL COMMENT '邮箱',
                         `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
                         `password` varchar(255) NOT NULL COMMENT '密码(加密)',
                         `nick_name` varchar(100) DEFAULT NULL COMMENT '昵称',
                         `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
                         `status` tinyint(1) DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
                         `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否, 1-是',
                         `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
                         `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                         `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                         PRIMARY KEY (`id`),
                         UNIQUE KEY `uk_username` (`username`),
                         UNIQUE KEY `uk_email` (`email`),
                         KEY `idx_phone` (`phone`),
                         KEY `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 角色表
CREATE TABLE `roles` (
                         `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
                         `role_name` varchar(50) NOT NULL COMMENT '角色名称',
                         `description` varchar(200) DEFAULT NULL COMMENT '角色描述',
                         `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                         `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                         PRIMARY KEY (`id`),
                         UNIQUE KEY `uk_role_name` (`role_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 用户角色关系表
CREATE TABLE `user_roles` (
                              `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
                              `user_id` bigint NOT NULL COMMENT '用户ID',
                              `role_id` bigint NOT NULL COMMENT '角色ID',
                              `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              PRIMARY KEY (`id`),
                              UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
                              KEY `idx_user_id` (`user_id`),
                              KEY `idx_role_id` (`role_id`),
                              CONSTRAINT `fk_user_roles_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
                              CONSTRAINT `fk_user_roles_role_id` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关系表';

-- 刷新令牌表
CREATE TABLE `refresh_tokens` (
                                  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
                                  `user_id` bigint NOT NULL COMMENT '用户ID',
                                  `token` varchar(255) NOT NULL COMMENT '刷新令牌',
                                  `expires_at` datetime NOT NULL COMMENT '过期时间',
                                  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  PRIMARY KEY (`id`),
                                  UNIQUE KEY `uk_token` (`token`),
                                  KEY `idx_user_id` (`user_id`),
                                  KEY `idx_expires_at` (`expires_at`),
                                  CONSTRAINT `fk_refresh_tokens_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='刷新令牌表';

-- 积分系统表
CREATE TABLE IF NOT EXISTS `points_account` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `balance` BIGINT NOT NULL DEFAULT 0 COMMENT '当前可用积分',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分账户';

CREATE TABLE IF NOT EXISTS `points_activity` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code` VARCHAR(64) NOT NULL COMMENT '唯一活动编码',
    `name` VARCHAR(128) NOT NULL COMMENT '活动名称',
    `description` VARCHAR(512) NULL COMMENT '活动描述',
    `status` VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    `start_time` DATETIME NULL COMMENT '开始时间',
    `end_time` DATETIME NULL COMMENT '结束时间',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分活动';

CREATE TABLE IF NOT EXISTS `points_bucket` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `activity_code` VARCHAR(64) NULL COMMENT '活动编码',
    `amount` BIGINT NOT NULL COMMENT '获得积分',
    `remaining` BIGINT NOT NULL COMMENT '剩余可用积分',
    `expire_at` DATETIME NULL COMMENT '过期时间',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_expire` (`user_id`, `expire_at`),
    KEY `idx_user_remaining` (`user_id`, `remaining`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分桶（按过期批次管理剩余积分）';

CREATE TABLE IF NOT EXISTS `points_transaction` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `type` VARCHAR(16) NOT NULL COMMENT 'EARN/CONSUME/EXPIRE/REFUND',
    `amount` BIGINT NOT NULL COMMENT '本次变动积分（正数）',
    `activity_code` VARCHAR(64) NULL COMMENT '活动编码',
    `description` VARCHAR(512) NULL COMMENT '备注',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_time` (`user_id`, `created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分流水';

CREATE TABLE IF NOT EXISTS `points_deduction_detail` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tx_id` BIGINT NOT NULL COMMENT '消费流水ID',
    `bucket_id` BIGINT NOT NULL COMMENT '被扣减的桶ID',
    `amount` BIGINT NOT NULL COMMENT '从该桶扣减的积分',
    PRIMARY KEY (`id`),
    KEY `idx_tx` (`tx_id`),
    KEY `idx_bucket` (`bucket_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分消费扣减明细';

-- 插入默认角色
INSERT INTO `roles` (`role_name`, `description`) VALUES
                                                     ('ROLE_USER', '普通用户'),
                                                     ('ROLE_ADMIN', '管理员');

-- 消息表（如不存在则创建）
CREATE TABLE IF NOT EXISTS `message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `sender_id` BIGINT NOT NULL COMMENT '发送者用户ID',
  `receiver_id` BIGINT NOT NULL COMMENT '接收者用户ID',
  `type` VARCHAR(32) NOT NULL COMMENT '消息类型',
  `content` VARCHAR(2048) NOT NULL COMMENT '消息内容',
  `status` VARCHAR(16) NOT NULL DEFAULT 'sent' COMMENT '消息状态',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除(0=否,1=是)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_receiver_id_id` (`receiver_id`, `id`),
  KEY `idx_both_users_time` (`sender_id`, `receiver_id`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- 好友关系表（双向各存一条记录）
CREATE TABLE IF NOT EXISTS `friends` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `friend_user_id` BIGINT NOT NULL COMMENT '好友用户ID',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_friend` (`user_id`, `friend_user_id`),
  KEY `idx_user` (`user_id`),
  CONSTRAINT `fk_friends_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_friends_friend` FOREIGN KEY (`friend_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友关系表';

-- 会话表（按用户维度管理与对端的会话）
CREATE TABLE IF NOT EXISTS `conversations` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '会话所属用户',
  `peer_id` BIGINT NOT NULL COMMENT '对端用户ID',
  `last_message_id` BIGINT NULL COMMENT '最后一条消息ID',
  `last_message_time` DATETIME NULL COMMENT '最后消息时间',
  `unread_count` INT NOT NULL DEFAULT 0 COMMENT '未读数',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_peer` (`user_id`, `peer_id`),
  KEY `idx_user_updated` (`user_id`, `updated_time`),
  CONSTRAINT `fk_conversations_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_conversations_peer` FOREIGN KEY (`peer_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户会话表';

-- 邀请码主表
CREATE TABLE IF NOT EXISTS `invite_codes` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` VARCHAR(32) NOT NULL COMMENT '邀请码明文',
  `code_hash` CHAR(64) NOT NULL COMMENT '邀请码哈希(SHA-256 HEX)',
  `creator_user_id` BIGINT NULL COMMENT '创建者用户ID',
  `max_uses` INT NOT NULL DEFAULT 1 COMMENT '最多可使用次数(>=1)',
  `used_count` INT NOT NULL DEFAULT 0 COMMENT '已使用次数',
  `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/USED/EXPIRED/REVOKED',
  `expire_at` DATETIME NULL COMMENT '过期时间(null表示长期有效)',
  `remark` VARCHAR(255) NULL COMMENT '备注',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  UNIQUE KEY `uk_code_hash` (`code_hash`),
  KEY `idx_creator` (`creator_user_id`),
  KEY `idx_status_expire` (`status`, `expire_at`),
  CONSTRAINT `fk_invite_codes_creator` FOREIGN KEY (`creator_user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邀请码表';

-- 邀请码使用记录表
CREATE TABLE IF NOT EXISTS `invite_usage` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `invite_code_id` BIGINT NOT NULL COMMENT '邀请码ID',
  `code_hash` CHAR(64) NOT NULL COMMENT '邀请码哈希(冗余便于查询)',
  `inviter_user_id` BIGINT NULL COMMENT '邀请人(冗余)',
  `invitee_user_id` BIGINT NOT NULL COMMENT '受邀用户',
  `client_ip` VARCHAR(64) NULL COMMENT '使用时IP',
  `user_agent` VARCHAR(255) NULL COMMENT 'UA',
  `used_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '使用时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_invitee` (`invitee_user_id`),
  KEY `idx_code` (`invite_code_id`),
  KEY `idx_hash_time` (`code_hash`, `used_at`),
  KEY `idx_ip_time` (`client_ip`, `used_at`),
  CONSTRAINT `fk_invite_usage_code` FOREIGN KEY (`invite_code_id`) REFERENCES `invite_codes` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_invite_usage_invitee` FOREIGN KEY (`invitee_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邀请码使用记录表';

-- AI 人物模板表
CREATE TABLE IF NOT EXISTS `ai_characters` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(128) NOT NULL COMMENT '人物名称',
  `image_url` VARCHAR(512) NULL COMMENT '图片/头像URL',
  `author` VARCHAR(128) NULL COMMENT '传作者/出处',
  `created_by_user_id` BIGINT NULL COMMENT '创建者用户ID',
  `visibility` VARCHAR(16) NOT NULL DEFAULT 'PUBLIC' COMMENT 'PUBLIC/PRIVATE',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1=启用,0=禁用',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `associated_user_id` BIGINT NULL COMMENT '关联的用户账户ID（AI角色对应的User记录）'
  PRIMARY KEY (`id`),
  KEY `idx_visibility_status` (`visibility`, `status`),
  KEY `idx_creator` (`created_by_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 人物模板';

-- AI 人物用户个性化设置表
CREATE TABLE IF NOT EXISTS `ai_character_settings` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `character_id` BIGINT NOT NULL COMMENT '人物ID',
  `name` VARCHAR(128) NULL COMMENT '自定义人名',
  `avatar_url` VARCHAR(512) NULL COMMENT '自定义头像URL',
  `memorial_day` DATE NULL COMMENT '纪念日',
  `relationship` VARCHAR(64) NULL COMMENT '关系',
  `background` VARCHAR(1024) NULL COMMENT '背景信息',
  `language` VARCHAR(32) NULL COMMENT '语言',
  `custom_params` VARCHAR(2000) NULL COMMENT '自定义参数(JSON)',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_character` (`user_id`, `character_id`),
  KEY `idx_character` (`character_id`),
  key idx_associated_user_id ON ai_characters(associated_user_id);
  CONSTRAINT `fk_ai_settings_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ai_settings_character` FOREIGN KEY (`character_id`) REFERENCES `ai_characters` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 人物用户设置';

-- 充值/支付模块表结构
CREATE TABLE IF NOT EXISTS `recharge_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `amount_cents` BIGINT NOT NULL COMMENT '充值金额(分)',
  `currency` VARCHAR(8) NOT NULL DEFAULT 'CNY' COMMENT '币种',
  `points` BIGINT NOT NULL DEFAULT 0 COMMENT '充值获得积分',
  `channel` VARCHAR(32) NOT NULL COMMENT '支付渠道',
  `channel_order_no` VARCHAR(128) NULL COMMENT '渠道订单号',
  `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SUCCESS/FAILED/CLOSED/REFUNDED',
  `client_req_id` VARCHAR(64) NOT NULL COMMENT '客户端幂等ID',
  `extra` VARCHAR(1024) NULL COMMENT '额外信息(JSON)',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `paid_time` DATETIME NULL COMMENT '支付成功时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  UNIQUE KEY `uk_user_client_req` (`user_id`, `client_req_id`),
  KEY `idx_user_created` (`user_id`, `created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='充值订单';

CREATE TABLE IF NOT EXISTS `wallet_account` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `balance_cents` BIGINT NOT NULL DEFAULT 0 COMMENT '余额(分)',
  `freeze_cents` BIGINT NOT NULL DEFAULT 0 COMMENT '冻结余额(分)',
  `version` BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='钱包账户';

CREATE TABLE IF NOT EXISTS `wallet_tx` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `order_no` VARCHAR(64) NULL COMMENT '关联订单号',
  `type` VARCHAR(16) NOT NULL COMMENT 'RECHARGE/ADJUST/REFUND/DEDUCT',
  `amount_cents` BIGINT NOT NULL COMMENT '变动金额(分, 正数)',
  `balance_after` BIGINT NOT NULL COMMENT '变动后余额(分)',
  `trace_id` VARCHAR(64) NULL COMMENT '追踪ID/幂等键',
  `remark` VARCHAR(255) NULL COMMENT '备注',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `created_time`),
  KEY `idx_order_no` (`order_no`),
  UNIQUE KEY `uk_order_type` (`order_no`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='钱包资金流水';

CREATE TABLE IF NOT EXISTS `idempotent_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `biz_type` VARCHAR(32) NOT NULL COMMENT '业务类型',
  `idem_key` VARCHAR(128) NOT NULL COMMENT '幂等键',
  `biz_id` VARCHAR(128) NULL COMMENT '业务ID(如订单号)',
  `status` VARCHAR(16) NOT NULL DEFAULT 'CREATED' COMMENT 'CREATED/SUCCESS/FAILED',
  `result_hash` VARCHAR(64) NULL COMMENT '结果校验',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_key` (`biz_type`,`idem_key`),
  KEY `idx_biz` (`biz_type`,`biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='幂等控制记录';

CREATE TABLE IF NOT EXISTS `risk_check_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `order_no` VARCHAR(64) NULL COMMENT '订单号',
  `amount_cents` BIGINT NOT NULL COMMENT '金额(分)',
  `risk_code` VARCHAR(32) NOT NULL COMMENT '规则编码',
  `pass` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否通过',
  `detail` VARCHAR(512) NULL COMMENT '详情',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`,`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='风控记录';

CREATE TABLE IF NOT EXISTS `payment_notify_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
  `channel` VARCHAR(32) NOT NULL COMMENT '渠道',
  `status` VARCHAR(16) NOT NULL COMMENT '收到/验签/已处理',
  `notify_body` TEXT NULL COMMENT '通知原文',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_channel` (`order_no`,`channel`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付回调日志';

-- 用户扩展资料表（1:1）
CREATE TABLE IF NOT EXISTS `user_profiles` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `gender` VARCHAR(16) NULL COMMENT '性别',
  `birthday` DATE NULL COMMENT '生日',
  `height_cm` INT NULL COMMENT '身高(厘米)',
  `mbti` VARCHAR(16) NULL COMMENT 'MBTI',
  `occupation` VARCHAR(128) NULL COMMENT '职业',
  `interests` VARCHAR(2048) NULL COMMENT '兴趣（逗号分隔或JSON）',
  `photos` VARCHAR(4096) NULL COMMENT '照片URL（逗号分隔或JSON）',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  CONSTRAINT `fk_user_profiles_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户扩展资料表';

-- 用户反馈表
CREATE TABLE IF NOT EXISTS `feedback` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type` VARCHAR(32) NOT NULL COMMENT 'bug|suggestion|content|ux|other',
  `content` VARCHAR(4096) NOT NULL COMMENT '反馈内容',
  `contact` VARCHAR(255) NULL COMMENT '联系方式',
  `images` VARCHAR(4096) NULL COMMENT '图片(数组JSON)',
  `app_version` VARCHAR(64) NULL COMMENT '应用版本',
  `os_version` VARCHAR(64) NULL COMMENT '系统版本',
  `device_model` VARCHAR(128) NULL COMMENT '设备型号',
  `network_type` VARCHAR(16) NULL COMMENT 'wifi|4g|5g',
  `page_route` VARCHAR(255) NULL COMMENT '页面路由',
  `user_id` BIGINT NULL COMMENT '用户ID',
  `extra_data` VARCHAR(4000) NULL COMMENT '扩展信息(JSON)',
  `status` VARCHAR(16) NOT NULL DEFAULT 'NEW' COMMENT 'NEW/IN_PROGRESS/RESOLVED/REJECTED',
  `handler_user_id` BIGINT NULL COMMENT '处理人',
  `handler_remark` VARCHAR(1024) NULL COMMENT '处理备注',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status_time` (`status`, `created_time`),
  KEY `idx_type_time` (`type`, `created_time`),
  KEY `idx_user_time` (`user_id`, `created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户反馈';

-- 语音资源表（保存语音文件元数据）
CREATE TABLE IF NOT EXISTS `voice_assets` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `message_id` BIGINT NULL COMMENT '关联消息ID',
  `file_key` VARCHAR(512) NOT NULL COMMENT '存储键',
  `content_type` VARCHAR(64) NULL COMMENT 'MIME 类型',
  `size_bytes` BIGINT NULL COMMENT '大小(字节)',
  `duration_ms` INT NULL COMMENT '时长(毫秒)',
  `format` VARCHAR(16) NULL COMMENT 'mp3/wav/ogg等',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `id`),
  KEY `idx_message` (`message_id`),
  UNIQUE KEY `uk_file_key` (`file_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='语音资源表';

-- ============================================
-- 机器人延迟调度系统数据表
-- 版本: V1
-- 日期: 2025-10-17
-- ============================================

-- 1. 创建任务表
CREATE TABLE IF NOT EXISTS robot_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '任务ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    robot_id BIGINT NULL COMMENT '机器人ID（可为空）',
    task_type VARCHAR(50) NOT NULL COMMENT '任务类型：IMMEDIATE, SHORT_DELAY, LONG_DELAY',
    action_type VARCHAR(50) NOT NULL COMMENT '行为类型：SEND_MESSAGE, SEND_VOICE, SEND_NOTIFICATION',
    action_payload TEXT NOT NULL COMMENT '任务载荷（JSON格式）',
    scheduled_at DATETIME NOT NULL COMMENT '计划执行时间（UTC）',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING, RUNNING, DONE, FAILED, TIMEOUT',
    locked_by VARCHAR(255) DEFAULT NULL COMMENT '领取任务的实例ID（hostname:pid格式），用于验证所有权',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '当前重试次数',
    max_retry_count INT NOT NULL DEFAULT 3 COMMENT '最大重试次数',
    started_at DATETIME NULL COMMENT '开始执行时间',
    completed_at DATETIME NULL COMMENT '完成时间',
    heartbeat_at DATETIME NULL COMMENT '最后心跳时间',
    error_message TEXT NULL COMMENT '错误信息',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_status_scheduled (status, scheduled_at),
    INDEX idx_user_id (user_id),
    INDEX idx_robot_id (robot_id),
    INDEX idx_timeout_check (status, heartbeat_at),
    INDEX idx_cleanup (status, completed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机器人调度任务表';

-- 2. 创建执行日志表
CREATE TABLE IF NOT EXISTS robot_task_execution_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    execution_attempt INT NOT NULL COMMENT '执行尝试次数',
    status VARCHAR(20) NOT NULL COMMENT '执行结果：SUCCESS, FAILED, TIMEOUT',
    started_at DATETIME NOT NULL COMMENT '开始时间',
    completed_at DATETIME NOT NULL COMMENT '完成时间',
    execution_duration_ms BIGINT NOT NULL COMMENT '执行耗时（毫秒）',
    delay_from_scheduled_ms BIGINT NOT NULL COMMENT '延迟时间（毫秒）',
    error_message TEXT NULL COMMENT '错误信息',
    instance_id VARCHAR(100) NOT NULL COMMENT '实例标识',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    INDEX idx_task_id (task_id),
    INDEX idx_started_at (started_at),
    INDEX idx_instance_id (instance_id),
    FOREIGN KEY (task_id) REFERENCES robot_task(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务执行日志表';


CREATE TABLE ai_model (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '模型ID',
    name VARCHAR(100) NOT NULL COMMENT '模型名称，如 GPT-4',
    version VARCHAR(50) NOT NULL COMMENT '模型版本，如 gpt-4-turbo',
    provider VARCHAR(50) NOT NULL COMMENT '提供商：openai/azure/qwen/gemini 等',
    model_type VARCHAR(20) NOT NULL COMMENT '模型类型：CHAT/EMBEDDING/RERANKER',
    
    api_endpoint VARCHAR(500) NOT NULL COMMENT 'API 端点 URL',
    api_key TEXT NOT NULL COMMENT 'API 密钥（AES-256 加密存储）',
    
    context_length INT COMMENT '上下文长度（token 数），如 8192',
    parameter_count VARCHAR(20) COMMENT '参数量，如 175B',
    
    tags JSON COMMENT '自定义标签，如 ["fast", "cheap"]',
    weight INT DEFAULT 1 COMMENT '路由权重，用于加权负载均衡',
    
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用：1-启用 0-禁用',
    
    created_by BIGINT COMMENT '创建人用户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '最后更新人用户ID',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_name_version (name, version),
    INDEX idx_provider (provider),
    INDEX idx_model_type (model_type),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI模型实例配置表';

-- 2. 路由策略表
CREATE TABLE routing_strategy (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '策略ID',
    name VARCHAR(100) NOT NULL COMMENT '策略名称',
    description VARCHAR(500) COMMENT '策略描述',
    
    strategy_type VARCHAR(30) NOT NULL COMMENT '策略类型：ROUND_ROBIN/LEAST_CONN/TAG_BASED/PRIORITY',
    config JSON NOT NULL COMMENT '策略配置（JSON格式）',
    
    is_default TINYINT(1) DEFAULT 0 COMMENT '是否默认策略：1-是 0-否',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用：1-启用 0-禁用',
    
    created_by BIGINT COMMENT '创建人用户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '最后更新人用户ID',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_name (name),
    INDEX idx_strategy_type (strategy_type),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='路由策略配置表';

-- 3. 策略与模型关联表（多对多）
CREATE TABLE strategy_model_relation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
    strategy_id BIGINT NOT NULL COMMENT '策略ID',
    model_id BIGINT NOT NULL COMMENT '模型ID',
    priority INT DEFAULT 0 COMMENT '优先级（用于 PRIORITY 策略，数值越大优先级越高）',
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    UNIQUE KEY uk_strategy_model (strategy_id, model_id),
    INDEX idx_strategy_id (strategy_id),
    INDEX idx_model_id (model_id),
    
    CONSTRAINT fk_strategy FOREIGN KEY (strategy_id) REFERENCES routing_strategy(id) ON DELETE CASCADE,
    CONSTRAINT fk_model FOREIGN KEY (model_id) REFERENCES ai_model(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='策略与模型关联表';

-- 4. 模型健康状态表
CREATE TABLE model_health_status (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '状态ID',
    model_id BIGINT NOT NULL COMMENT '模型ID',
    
    status VARCHAR(20) NOT NULL COMMENT '健康状态：ONLINE/OFFLINE/TIMEOUT/AUTH_FAILED',
    consecutive_failures INT DEFAULT 0 COMMENT '连续失败次数',
    total_checks INT DEFAULT 0 COMMENT '总检查次数',
    successful_checks INT DEFAULT 0 COMMENT '成功检查次数',
    
    last_check_time DATETIME COMMENT '最后检查时间',
    last_success_time DATETIME COMMENT '最后成功时间',
    last_error TEXT COMMENT '最后错误信息',
    
    last_response_time INT COMMENT '最后响应时间（毫秒）',
    response_time_ms INT COMMENT '平均响应时间（毫秒）',
    uptime_percentage DECIMAL(5,2) COMMENT '可用率（%）',
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_model_id (model_id),
    INDEX idx_status (status),
    INDEX idx_last_check (last_check_time),
    
    CONSTRAINT fk_health_model FOREIGN KEY (model_id) REFERENCES ai_model(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型健康状态表';

-- 5. 模型请求日志表 (带分区)
CREATE TABLE model_request_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    request_id VARCHAR(64) NOT NULL COMMENT '请求唯一标识（UUID）',
    
    model_id BIGINT NOT NULL COMMENT '调用的模型ID',
    model_name VARCHAR(100) COMMENT '模型名称快照',
    
    request_type VARCHAR(20) NOT NULL COMMENT '请求类型：CHAT/EMBEDDING/RERANKER',
    prompt_tokens INT COMMENT '输入 token 数',
    completion_tokens INT COMMENT '输出 token 数',
    total_tokens INT COMMENT '总 token 数',
    
    response_status VARCHAR(20) NOT NULL COMMENT '响应状态：SUCCESS/FAILED/TIMEOUT',
    response_time_ms INT COMMENT '响应耗时（毫秒）',
    error_message TEXT COMMENT '错误信息（如有）',
    
    user_id BIGINT COMMENT '发起请求的用户ID',
    source VARCHAR(100) COMMENT '请求来源（IP或服务名）',
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '请求时间',
    
    INDEX idx_model_id (model_id),
    INDEX idx_request_id (request_id),
    INDEX idx_created_at (created_at),
    INDEX idx_response_status (response_status),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型请求日志表'
PARTITION BY RANGE (TO_DAYS(created_at)) (
    PARTITION p_history VALUES LESS THAN (TO_DAYS('2025-10-01')),
    PARTITION p_2025_10 VALUES LESS THAN (TO_DAYS('2025-11-01')),
    PARTITION p_2025_11 VALUES LESS THAN (TO_DAYS('2025-12-01')),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- 插入默认路由策略
INSERT INTO routing_strategy (name, description, strategy_type, config, is_default, enabled)
VALUES ('默认轮询策略', '按顺序依次选择可用模型', 'ROUND_ROBIN', '{}', 1, 1);


CREATE TABLE `prompt_template` (
                                   `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                   `char_id`  BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
                                   `description` VARCHAR(255) DEFAULT NULL COMMENT '模板描述',

                                   `model_code` VARCHAR(64) DEFAULT NULL COMMENT '适用模型，如 gpt-4.1、qwen-max',
                                   `lang` VARCHAR(16) DEFAULT 'zh-CN' COMMENT '模板语言，如 zh-CN',

                                   `content` MEDIUMTEXT NOT NULL COMMENT '模板内容（Beetl 模板）',
                                   `param_schema` JSON DEFAULT NULL COMMENT '模板参数结构说明（JSON）',

                                   `version` INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '版本号，从1递增',
                                   `is_latest` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否最新版本：1=是，0=否',
                                   `is_stable` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否稳定模板：1=是，0=否（生产默认走稳定版）',
                                   `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0=草稿，1=启用，2=停用',


                                   `gray_strategy` TINYINT NOT NULL DEFAULT 0 COMMENT '灰度策略：0=无灰度，1=按比例，2=按用户白名单',
                                   `gray_ratio` INT DEFAULT NULL COMMENT '灰度比例：0~100，gray_strategy=1时有效',
                                   `gray_user_list` JSON DEFAULT NULL COMMENT '灰度用户白名单（用户ID数组），gray_strategy=2时有效',
                                   `priority` INT NOT NULL DEFAULT 0 COMMENT '模板优先级（值越大越优先匹配）',
                                   `tags` JSON DEFAULT NULL COMMENT '扩展匹配条件，如地区/渠道/设备（可选）',

                                   `post_process_config` JSON DEFAULT NULL COMMENT '后处理配置（JSON），支持过滤标签、正则替换等规则',

                                   `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
                                   `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',

                                   `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                   `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=否，1=是',

                                   PRIMARY KEY (`id`),

                                   UNIQUE KEY `uk_char_id_version` (`char_id`, `version`),

                                   KEY `idx_char_id_latest` (`char_id`, `is_latest`, `status`),


                                   KEY `idx_gray` (`gray_strategy`, `status`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='Prompt 模板表（含灰度发布、版本管理、Beetl 模板内容）';

-- 月度计划表
CREATE TABLE IF NOT EXISTS `monthly_plans` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `character_id` BIGINT NOT NULL COMMENT '关联的虚拟人物ID',
  `day_rule` VARCHAR(64) NOT NULL COMMENT '日期规则，如 day=5 或 weekday=1,week=2',
  `start_time` TIME NOT NULL COMMENT '活动开始时间',
  `duration_min` INT NOT NULL COMMENT '持续时长（分钟）',
  `location` VARCHAR(255) NULL COMMENT '活动地点',
  `action` VARCHAR(512) NOT NULL COMMENT '活动内容',
  `participants` JSON NULL COMMENT '参与者列表（JSON数组）',
  `extra` JSON NULL COMMENT '扩展信息（JSON对象）',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0=否，1=是',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_character_id` (`character_id`),
  KEY `idx_character_deleted` (`character_id`, `is_deleted`),
  CONSTRAINT `fk_monthly_plans_character` FOREIGN KEY (`character_id`) 
    REFERENCES `ai_characters` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='虚拟人物月度计划表';
