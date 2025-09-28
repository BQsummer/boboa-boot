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

-- 插入默认角色
INSERT INTO `roles` (`role_name`, `description`) VALUES
                                                     ('ROLE_USER', '普通用户'),
                                                     ('ROLE_ADMIN', '管理员');



commit;