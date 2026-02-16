DROP TABLE IF EXISTS QRTZ_FIRED_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_PAUSED_TRIGGER_GRPS;
DROP TABLE IF EXISTS QRTZ_SCHEDULER_STATE;
DROP TABLE IF EXISTS QRTZ_LOCKS;
DROP TABLE IF EXISTS QRTZ_SIMPLE_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_CRON_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_SIMPROP_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_BLOB_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_JOB_DETAILS;
DROP TABLE IF EXISTS QRTZ_CALENDARS;

CREATE TABLE QRTZ_JOB_DETAILS
(
    SCHED_NAME        VARCHAR(120) NOT NULL,
    JOB_NAME          VARCHAR(200) NOT NULL,
    JOB_GROUP         VARCHAR(200) NOT NULL,
    DESCRIPTION       VARCHAR(250) NULL,
    JOB_CLASS_NAME    VARCHAR(250) NOT NULL,
    IS_DURABLE        BOOL         NOT NULL,
    IS_NONCONCURRENT  BOOL         NOT NULL,
    IS_UPDATE_DATA    BOOL         NOT NULL,
    REQUESTS_RECOVERY BOOL         NOT NULL,
    JOB_DATA          BYTEA        NULL,
    PRIMARY KEY (SCHED_NAME, JOB_NAME, JOB_GROUP)
);

CREATE TABLE QRTZ_TRIGGERS
(
    SCHED_NAME     VARCHAR(120) NOT NULL,
    TRIGGER_NAME   VARCHAR(200) NOT NULL,
    TRIGGER_GROUP  VARCHAR(200) NOT NULL,
    JOB_NAME       VARCHAR(200) NOT NULL,
    JOB_GROUP      VARCHAR(200) NOT NULL,
    DESCRIPTION    VARCHAR(250) NULL,
    NEXT_FIRE_TIME BIGINT       NULL,
    PREV_FIRE_TIME BIGINT       NULL,
    PRIORITY       INTEGER      NULL,
    TRIGGER_STATE  VARCHAR(16)  NOT NULL,
    TRIGGER_TYPE   VARCHAR(8)   NOT NULL,
    START_TIME     BIGINT       NOT NULL,
    END_TIME       BIGINT       NULL,
    CALENDAR_NAME  VARCHAR(200) NULL,
    MISFIRE_INSTR  SMALLINT     NULL,
    JOB_DATA       BYTEA        NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, JOB_NAME, JOB_GROUP)
        REFERENCES QRTZ_JOB_DETAILS (SCHED_NAME, JOB_NAME, JOB_GROUP)
);

CREATE TABLE QRTZ_SIMPLE_TRIGGERS
(
    SCHED_NAME      VARCHAR(120) NOT NULL,
    TRIGGER_NAME    VARCHAR(200) NOT NULL,
    TRIGGER_GROUP   VARCHAR(200) NOT NULL,
    REPEAT_COUNT    BIGINT       NOT NULL,
    REPEAT_INTERVAL BIGINT       NOT NULL,
    TIMES_TRIGGERED BIGINT       NOT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE QRTZ_CRON_TRIGGERS
(
    SCHED_NAME      VARCHAR(120) NOT NULL,
    TRIGGER_NAME    VARCHAR(200) NOT NULL,
    TRIGGER_GROUP   VARCHAR(200) NOT NULL,
    CRON_EXPRESSION VARCHAR(120) NOT NULL,
    TIME_ZONE_ID    VARCHAR(80),
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE QRTZ_SIMPROP_TRIGGERS
(
    SCHED_NAME    VARCHAR(120)   NOT NULL,
    TRIGGER_NAME  VARCHAR(200)   NOT NULL,
    TRIGGER_GROUP VARCHAR(200)   NOT NULL,
    STR_PROP_1    VARCHAR(512)   NULL,
    STR_PROP_2    VARCHAR(512)   NULL,
    STR_PROP_3    VARCHAR(512)   NULL,
    INT_PROP_1    INT            NULL,
    INT_PROP_2    INT            NULL,
    LONG_PROP_1   BIGINT         NULL,
    LONG_PROP_2   BIGINT         NULL,
    DEC_PROP_1    NUMERIC(13, 4) NULL,
    DEC_PROP_2    NUMERIC(13, 4) NULL,
    BOOL_PROP_1   BOOL           NULL,
    BOOL_PROP_2   BOOL           NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE QRTZ_BLOB_TRIGGERS
(
    SCHED_NAME    VARCHAR(120) NOT NULL,
    TRIGGER_NAME  VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    BLOB_DATA     BYTEA        NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE QRTZ_CALENDARS
(
    SCHED_NAME    VARCHAR(120) NOT NULL,
    CALENDAR_NAME VARCHAR(200) NOT NULL,
    CALENDAR      BYTEA        NOT NULL,
    PRIMARY KEY (SCHED_NAME, CALENDAR_NAME)
);


CREATE TABLE QRTZ_PAUSED_TRIGGER_GRPS
(
    SCHED_NAME    VARCHAR(120) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_GROUP)
);

CREATE TABLE QRTZ_FIRED_TRIGGERS
(
    SCHED_NAME        VARCHAR(120) NOT NULL,
    ENTRY_ID          VARCHAR(95)  NOT NULL,
    TRIGGER_NAME      VARCHAR(200) NOT NULL,
    TRIGGER_GROUP     VARCHAR(200) NOT NULL,
    INSTANCE_NAME     VARCHAR(200) NOT NULL,
    FIRED_TIME        BIGINT       NOT NULL,
    SCHED_TIME        BIGINT       NOT NULL,
    PRIORITY          INTEGER      NOT NULL,
    STATE             VARCHAR(16)  NOT NULL,
    JOB_NAME          VARCHAR(200) NULL,
    JOB_GROUP         VARCHAR(200) NULL,
    IS_NONCONCURRENT  BOOL         NULL,
    REQUESTS_RECOVERY BOOL         NULL,
    PRIMARY KEY (SCHED_NAME, ENTRY_ID)
);

CREATE TABLE QRTZ_SCHEDULER_STATE
(
    SCHED_NAME        VARCHAR(120) NOT NULL,
    INSTANCE_NAME     VARCHAR(200) NOT NULL,
    LAST_CHECKIN_TIME BIGINT       NOT NULL,
    CHECKIN_INTERVAL  BIGINT       NOT NULL,
    PRIMARY KEY (SCHED_NAME, INSTANCE_NAME)
);

CREATE TABLE QRTZ_LOCKS
(
    SCHED_NAME VARCHAR(120) NOT NULL,
    LOCK_NAME  VARCHAR(40)  NOT NULL,
    PRIMARY KEY (SCHED_NAME, LOCK_NAME)
);

CREATE INDEX IDX_QRTZ_J_REQ_RECOVERY
    ON QRTZ_JOB_DETAILS (SCHED_NAME, REQUESTS_RECOVERY);
CREATE INDEX IDX_QRTZ_J_GRP
    ON QRTZ_JOB_DETAILS (SCHED_NAME, JOB_GROUP);

CREATE INDEX IDX_QRTZ_T_J
    ON QRTZ_TRIGGERS (SCHED_NAME, JOB_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_T_JG
    ON QRTZ_TRIGGERS (SCHED_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_T_C
    ON QRTZ_TRIGGERS (SCHED_NAME, CALENDAR_NAME);
CREATE INDEX IDX_QRTZ_T_G
    ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_GROUP);
CREATE INDEX IDX_QRTZ_T_STATE
    ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_N_STATE
    ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_N_G_STATE
    ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_GROUP, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_NEXT_FIRE_TIME
    ON QRTZ_TRIGGERS (SCHED_NAME, NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_ST
    ON QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_STATE, NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_MISFIRE
    ON QRTZ_TRIGGERS (SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_ST_MISFIRE
    ON QRTZ_TRIGGERS (SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_NFT_ST_MISFIRE_GRP
    ON QRTZ_TRIGGERS (SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME, TRIGGER_GROUP, TRIGGER_STATE);

CREATE INDEX IDX_QRTZ_FT_TRIG_INST_NAME
    ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, INSTANCE_NAME);
CREATE INDEX IDX_QRTZ_FT_INST_JOB_REQ_RCVRY
    ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, INSTANCE_NAME, REQUESTS_RECOVERY);
CREATE INDEX IDX_QRTZ_FT_J_G
    ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, JOB_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_FT_JG
    ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_FT_T_G
    ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);
CREATE INDEX IDX_QRTZ_FT_TG
    ON QRTZ_FIRED_TRIGGERS (SCHED_NAME, TRIGGER_GROUP);

CREATE TABLE config (
                        id BIGSERIAL PRIMARY KEY,
                        env VARCHAR(10) NOT NULL,
                        application VARCHAR(50),
                        name VARCHAR(50) NOT NULL,
                        "desc" VARCHAR(255),
                        value VARCHAR(5000) NOT NULL,
                        type VARCHAR(10) NOT NULL,
                        sensitive VARCHAR(10) NOT NULL,
                        status VARCHAR(10) NOT NULL,
                        catalog VARCHAR(50),
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        created_by VARCHAR(50),
                        updated_by VARCHAR(50),
                        CONSTRAINT uk_config_name_env UNIQUE (name, env)
);

CREATE TABLE config_history (
                                id BIGSERIAL PRIMARY KEY,
                                env VARCHAR(10) NOT NULL,
                                application VARCHAR(50),
                                name VARCHAR(50) NOT NULL,
                                "desc" VARCHAR(255),
                                value VARCHAR(5000) NOT NULL,
                                type VARCHAR(10) NOT NULL,
                                sensitive VARCHAR(10) NOT NULL,
                                status VARCHAR(10) NOT NULL,
                                catalog VARCHAR(50),
                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                created_by VARCHAR(50),
                                updated_by VARCHAR(50),
                                CONSTRAINT idx_name_env UNIQUE (name, env)
);

CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       username VARCHAR(50) NOT NULL,
                       email VARCHAR(100) NOT NULL,
                       phone VARCHAR(20),
                       password VARCHAR(255) NOT NULL,
                       nick_name VARCHAR(100),
                       avatar VARCHAR(255),
                       status SMALLINT DEFAULT 1,
                       is_deleted SMALLINT NOT NULL DEFAULT 0,
                       last_login_time TIMESTAMP,
                       user_type VARCHAR(32),
                       created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       CONSTRAINT uk_username UNIQUE (username),
                       CONSTRAINT uk_email UNIQUE (email)
);
CREATE INDEX idx_phone ON users (phone);
CREATE INDEX idx_created_time ON users (created_time);

CREATE TABLE roles (
                       id BIGSERIAL PRIMARY KEY,
                       role_name VARCHAR(50) NOT NULL,
                       description VARCHAR(200),
                       created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       UNIQUE (role_name)
);

-- 插入默认角色
INSERT INTO roles (role_name, description) VALUES
                                               ('ROLE_USER', '普通用户'),
                                               ('ROLE_ADMIN', '管理员');

-- 插入管理员用户 (密码: admin123)
INSERT INTO users (username, email, phone, password, nick_name, avatar, status, is_deleted)
VALUES ('admin', 'admin@boboa.com', '13800138000', '$2a$10$QTVOtMPxoHRFcDPkncO2Q.m4t6doEp3vzTNCOXVyjCwXOsEGAcQXS', '系统管理员', NULL, 1, 0);

-- 为管理员用户分配管理员角色
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'admin' AND r.role_name = 'ROLE_ADMIN';

CREATE TABLE user_roles (
                            id BIGSERIAL PRIMARY KEY,
                            user_id BIGINT NOT NULL,
                            role_id BIGINT NOT NULL,
                            created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            CONSTRAINT uk_user_role UNIQUE (user_id, role_id)
);
CREATE INDEX idx_user_id ON user_roles (user_id);
CREATE INDEX idx_role_id ON user_roles (role_id);

CREATE TABLE refresh_tokens (
                                id BIGSERIAL PRIMARY KEY,
                                user_id BIGINT NOT NULL,
                                token VARCHAR(255) NOT NULL,
                                expires_at TIMESTAMP NOT NULL,
                                created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                UNIQUE (token)
);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);

CREATE TABLE  points_account (
                                 id BIGSERIAL PRIMARY KEY,
                                 user_id BIGINT NOT NULL,
                                 balance BIGINT NOT NULL DEFAULT 0,
                                 created_time TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                 updated_time TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                 UNIQUE (user_id)
);


CREATE TABLE points_activity (
                                 id BIGSERIAL PRIMARY KEY,
                                 code VARCHAR(64) NOT NULL,
                                 name VARCHAR(128) NOT NULL,
                                 description VARCHAR(512),
                                 status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
                                 start_time TIMESTAMP,
                                 end_time TIMESTAMP,
                                 created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                 updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                 UNIQUE (code)
);


CREATE TABLE points_bucket (
                               id BIGSERIAL PRIMARY KEY,
                               user_id BIGINT NOT NULL,
                               activity_code VARCHAR(64),
                               amount BIGINT NOT NULL,
                               remaining BIGINT NOT NULL,
                               expire_at TIMESTAMP,
                               created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_user_expire ON points_bucket (user_id, expire_at);
CREATE INDEX idx_user_remaining ON points_bucket (user_id, remaining);


CREATE TABLE points_transaction (
                                    id BIGSERIAL PRIMARY KEY,
                                    user_id BIGINT NOT NULL,
                                    type VARCHAR(16) NOT NULL,
                                    amount BIGINT NOT NULL,
                                    activity_code VARCHAR(64),
                                    description VARCHAR(512),
                                    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_user_time ON points_transaction (user_id, created_time);


CREATE TABLE points_deduction_detail (
                                         id BIGSERIAL PRIMARY KEY,
                                         tx_id BIGINT NOT NULL,
                                         bucket_id BIGINT NOT NULL,
                                         amount BIGINT NOT NULL
);
CREATE INDEX idx_tx ON points_deduction_detail (tx_id);
CREATE INDEX idx_bucket ON points_deduction_detail (bucket_id);


CREATE TABLE message (
                         id BIGSERIAL PRIMARY KEY,
                         sender_id BIGINT NOT NULL,
                         receiver_id BIGINT NOT NULL,
                         type VARCHAR(32) NOT NULL,
                         content VARCHAR(2048) NOT NULL,
                         status VARCHAR(16) NOT NULL DEFAULT 'sent',
                         is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_receiver_id_id ON message (receiver_id, id);
CREATE INDEX idx_both_users_time ON message (sender_id, receiver_id, id);


CREATE TABLE friends (
                         id BIGSERIAL PRIMARY KEY,
                         user_id BIGINT NOT NULL,
                         friend_user_id BIGINT NOT NULL,
                         created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_user_friend ON friends (user_id, friend_user_id);
CREATE INDEX idx_user ON friends (user_id);


CREATE TABLE conversations (
                               id BIGSERIAL PRIMARY KEY,
                               user_id BIGINT NOT NULL,
                               peer_id BIGINT NOT NULL,
                               last_message_id BIGINT,
                               last_message_time TIMESTAMP,
                               unread_count INT NOT NULL DEFAULT 0,
                               is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                               created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_user_peer ON conversations (user_id, peer_id);
CREATE INDEX idx_user_updated ON conversations (user_id, updated_time);


CREATE TABLE invite_codes (
                              id BIGSERIAL PRIMARY KEY,
                              code VARCHAR(32) NOT NULL,
                              code_hash CHAR(64) NOT NULL,
                              creator_user_id BIGINT,
                              max_uses INT NOT NULL DEFAULT 1,
                              used_count INT NOT NULL DEFAULT 0,
                              status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
                              expire_at TIMESTAMP,
                              remark VARCHAR(255),
                              created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_code ON invite_codes (code);
CREATE UNIQUE INDEX uk_code_hash ON invite_codes (code_hash);
CREATE INDEX idx_creator ON invite_codes (creator_user_id);
CREATE INDEX idx_status_expire ON invite_codes (status, expire_at);


CREATE TABLE invite_usage (
                              id BIGSERIAL PRIMARY KEY,
                              invite_code_id BIGINT NOT NULL,
                              code_hash CHAR(64) NOT NULL,
                              inviter_user_id BIGINT,
                              invitee_user_id BIGINT NOT NULL,
                              client_ip VARCHAR(64),
                              user_agent VARCHAR(255),
                              used_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_invitee ON invite_usage (invitee_user_id);
CREATE INDEX idx_code ON invite_usage (invite_code_id);
CREATE INDEX idx_hash_time ON invite_usage (code_hash, used_at);
CREATE INDEX idx_ip_time ON invite_usage (client_ip, used_at);

CREATE TABLE ai_characters (
                               id BIGSERIAL PRIMARY KEY,
                               name VARCHAR(128) NOT NULL,
                               image_url VARCHAR(512),
                               author VARCHAR(128),
                               created_by_user_id BIGINT,
                               visibility VARCHAR(16) NOT NULL DEFAULT 'PUBLIC',
                               status BOOLEAN NOT NULL DEFAULT TRUE,
                               is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                               created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               associated_user_id BIGINT
);

CREATE INDEX idx_visibility_status ON ai_characters (visibility, status);
CREATE INDEX idx_creator ON ai_characters (created_by_user_id);


CREATE TABLE ai_character_settings (
                                       id BIGSERIAL PRIMARY KEY,
                                       user_id BIGINT,
                                       character_id BIGINT NOT NULL,
                                       name VARCHAR(128),
                                       avatar_url VARCHAR(512),
                                       memorial_day DATE,
                                       relationship VARCHAR(64),
                                       background VARCHAR(1024),
                                       language VARCHAR(32),
                                       custom_params VARCHAR(2000),
                                       is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                                       created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_character
    ON ai_character_settings (character_id);

-- Compatibility migration for existing databases:
-- default character setting uses user_id IS NULL
ALTER TABLE ai_character_settings
    ALTER COLUMN user_id DROP NOT NULL;

ALTER TABLE ai_character_settings
    DROP CONSTRAINT IF EXISTS ai_character_settings_user_id_character_id_key;

DROP INDEX IF EXISTS uk_ai_character_setting_user_character;
DROP INDEX IF EXISTS uk_ai_character_setting_default;

CREATE UNIQUE INDEX uk_ai_character_setting_user_character
    ON ai_character_settings (user_id, character_id)
    WHERE user_id IS NOT NULL;

CREATE UNIQUE INDEX uk_ai_character_setting_default
    ON ai_character_settings (character_id)
    WHERE user_id IS NULL;


CREATE TABLE recharge_order (
                                id BIGSERIAL PRIMARY KEY,
                                order_no VARCHAR(64) NOT NULL,
                                user_id BIGINT NOT NULL,
                                amount_cents BIGINT NOT NULL,
                                currency VARCHAR(8) NOT NULL DEFAULT 'CNY',
                                points BIGINT NOT NULL DEFAULT 0,
                                channel VARCHAR(32) NOT NULL,
                                channel_order_no VARCHAR(128),
                                status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
                                client_req_id VARCHAR(64) NOT NULL,
                                extra VARCHAR(1024),
                                created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                paid_time TIMESTAMP,
                                UNIQUE (order_no),
                                UNIQUE (user_id, client_req_id)
);
CREATE INDEX idx_user_created
    ON recharge_order (user_id, created_time);


CREATE TABLE wallet_account (
                                   id BIGSERIAL PRIMARY KEY,
                                   user_id BIGINT NOT NULL,
                                   balance_cents BIGINT NOT NULL DEFAULT 0,
                                   freeze_cents BIGINT NOT NULL DEFAULT 0,
                                   version BIGINT NOT NULL DEFAULT 0,
                                   created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   CONSTRAINT uk_user UNIQUE (user_id)
);

CREATE TABLE wallet_tx (
                              id BIGSERIAL PRIMARY KEY,
                              user_id BIGINT NOT NULL,
                              order_no VARCHAR(64) NULL,
                              type VARCHAR(16) NOT NULL,
                              amount_cents BIGINT NOT NULL,
                              balance_after BIGINT NOT NULL,
                              trace_id VARCHAR(64) NULL,
                              remark VARCHAR(255) NULL,
                              created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_user_time ON wallet_tx (user_id, created_time);
CREATE INDEX idx_order_no ON wallet_tx (order_no);
CREATE UNIQUE INDEX uk_order_type ON wallet_tx (order_no, type);

CREATE TABLE idempotent_record (
                                      id BIGSERIAL PRIMARY KEY,
                                      biz_type VARCHAR(32) NOT NULL,
                                      idem_key VARCHAR(128) NOT NULL,
                                      biz_id VARCHAR(128) NULL,
                                      status VARCHAR(16) NOT NULL DEFAULT 'CREATED',
                                      result_hash VARCHAR(64) NULL,
                                      created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_biz_key ON idempotent_record (biz_type, idem_key);
CREATE INDEX idx_biz ON idempotent_record (biz_type, biz_id);

CREATE TABLE risk_check_log (
                                   id BIGSERIAL PRIMARY KEY,
                                   user_id BIGINT NOT NULL,
                                   order_no VARCHAR(64) NULL,
                                   amount_cents BIGINT NOT NULL,
                                   risk_code VARCHAR(32) NOT NULL,
                                   pass BOOLEAN NOT NULL DEFAULT TRUE,
                                   detail VARCHAR(512) NULL,
                                   created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_user_time ON risk_check_log (user_id, created_time);

CREATE TABLE payment_notify_log (
                                       id BIGSERIAL PRIMARY KEY,
                                       order_no VARCHAR(64) NOT NULL,
                                       channel VARCHAR(32) NOT NULL,
                                       status VARCHAR(16) NOT NULL,
                                       notify_body TEXT NULL,
                                       created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_order_channel ON payment_notify_log (order_no, channel);

CREATE TABLE user_profiles (
                                  id BIGSERIAL PRIMARY KEY,
                                  user_id BIGINT NOT NULL,
                                  gender VARCHAR(16) NULL,
                                  birthday DATE NULL,
                                  height_cm INT NULL,
                                  mbti VARCHAR(16) NULL,
                                  occupation VARCHAR(128) NULL,
                                  interests VARCHAR(2048) NULL,
                                  photos VARCHAR(4096) NULL,
                                  desc VARCHAR(2048) NULL,
                                  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  CONSTRAINT uk_user_id UNIQUE (user_id)
);

CREATE TABLE feedback (
                             id BIGSERIAL PRIMARY KEY,
                             type VARCHAR(32) NOT NULL,
                             content VARCHAR(4096) NOT NULL,
                             contact VARCHAR(255) NULL,
                             images VARCHAR(4096) NULL,
                             app_version VARCHAR(64) NULL,
                             os_version VARCHAR(64) NULL,
                             device_model VARCHAR(128) NULL,
                             network_type VARCHAR(16) NULL,
                             page_route VARCHAR(255) NULL,
                             user_id BIGINT NULL,
                             extra_data VARCHAR(4000) NULL,
                             status VARCHAR(16) NOT NULL DEFAULT 'NEW',
                             handler_user_id BIGINT NULL,
                             handler_remark VARCHAR(1024) NULL,
                             created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_status_time ON feedback (status, created_time);
CREATE INDEX idx_type_time ON feedback (type, created_time);
CREATE INDEX idx_user_time ON feedback (user_id, created_time);

CREATE TABLE voice_assets (
                                 id BIGSERIAL PRIMARY KEY,
                                 user_id BIGINT NOT NULL,
                                 message_id BIGINT NULL,
                                 file_key VARCHAR(512) NOT NULL,
                                 content_type VARCHAR(64) NULL,
                                 size_bytes BIGINT NULL,
                                 duration_ms INT NULL,
                                 format VARCHAR(16) NULL,
                                 is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                                 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 CONSTRAINT uk_file_key UNIQUE (file_key)
);
CREATE INDEX idx_user_time ON voice_assets (user_id, id);
CREATE INDEX idx_message ON voice_assets (message_id);

CREATE TABLE robot_task (
                             id BIGSERIAL PRIMARY KEY,
                             user_id BIGINT NOT NULL,
                             robot_id BIGINT NULL,
                             task_type VARCHAR(50) NOT NULL,
                             action_type VARCHAR(50) NOT NULL,
                             action_payload TEXT NOT NULL,
                             scheduled_at TIMESTAMP NOT NULL,
                             status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                             locked_by VARCHAR(255) DEFAULT NULL,
                             retry_count INT NOT NULL DEFAULT 0,
                             max_retry_count INT NOT NULL DEFAULT 3,
                             started_at TIMESTAMP NULL,
                             completed_at TIMESTAMP NULL,
                             heartbeat_at TIMESTAMP NULL,
                             error_message TEXT NULL,
                             created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_status_scheduled ON robot_task (status, scheduled_at);
CREATE INDEX idx_user_id ON robot_task (user_id);
CREATE INDEX idx_robot_id ON robot_task (robot_id);
CREATE INDEX idx_timeout_check ON robot_task (status, heartbeat_at);
CREATE INDEX idx_cleanup ON robot_task (status, completed_at);

CREATE TABLE robot_task_execution_log (
                                           id BIGSERIAL PRIMARY KEY,
                                           task_id BIGINT NOT NULL,
                                           execution_attempt INT NOT NULL,
                                           status VARCHAR(20) NOT NULL,
                                           started_at TIMESTAMP NOT NULL,
                                           completed_at TIMESTAMP NOT NULL,
                                           execution_duration_ms BIGINT NOT NULL,
                                           delay_from_scheduled_ms BIGINT NOT NULL,
                                           error_message TEXT NULL,
                                           instance_id VARCHAR(100) NOT NULL,
                                           created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_task_id ON robot_task_execution_log (task_id);
CREATE INDEX idx_started_at ON robot_task_execution_log (started_at);
CREATE INDEX idx_instance_id ON robot_task_execution_log (instance_id);


CREATE TABLE ai_model (
                          id BIGSERIAL PRIMARY KEY,
                          name VARCHAR(100) NOT NULL,
                          version VARCHAR(50) NOT NULL,
                          provider VARCHAR(50) NOT NULL,
                          model_type VARCHAR(20) NOT NULL,

                          api_endpoint VARCHAR(500) NOT NULL,
                          api_key TEXT NOT NULL,

                          context_length INT,
                          parameter_count VARCHAR(20),

                          tags JSON,
                          weight INT DEFAULT 1,

                          enabled BOOLEAN DEFAULT TRUE,

                          created_by BIGINT,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          updated_by BIGINT,
                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                          CONSTRAINT uk_name_version UNIQUE (name, version)
);
CREATE INDEX idx_provider ON ai_model (provider);
CREATE INDEX idx_model_type ON ai_model (model_type);
CREATE INDEX idx_enabled ON ai_model (enabled);

CREATE TABLE routing_strategy (
                                  id BIGSERIAL PRIMARY KEY,
                                  name VARCHAR(100) NOT NULL,
                                  description VARCHAR(500),

                                  strategy_type VARCHAR(30) NOT NULL,
                                  config JSON NOT NULL,

                                  is_default BOOLEAN DEFAULT FALSE,
                                  enabled BOOLEAN DEFAULT TRUE,

                                  created_by BIGINT,
                                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                  updated_by BIGINT,
                                  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                  CONSTRAINT uk_name UNIQUE (name)
);
CREATE INDEX idx_strategy_type ON routing_strategy (strategy_type);
CREATE INDEX idx_enabled ON routing_strategy (enabled);

CREATE TABLE strategy_model_relation (
                                         id BIGSERIAL PRIMARY KEY,
                                         strategy_id BIGINT NOT NULL,
                                         model_id BIGINT NOT NULL,
                                         priority INT DEFAULT 0,
                                         weight INT DEFAULT 1,

                                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                         CONSTRAINT uk_strategy_model UNIQUE (strategy_id, model_id)
);
CREATE INDEX idx_strategy_id ON strategy_model_relation (strategy_id);
CREATE INDEX idx_model_id ON strategy_model_relation (model_id);

CREATE TABLE model_health_status (
                                     id BIGSERIAL PRIMARY KEY,
                                     model_id BIGINT NOT NULL,

                                     status VARCHAR(20) NOT NULL,
                                     consecutive_failures INT DEFAULT 0,
                                     total_checks INT DEFAULT 0,
                                     successful_checks INT DEFAULT 0,

                                     last_check_time TIMESTAMP,
                                     last_success_time TIMESTAMP,
                                     last_error TEXT,

                                     last_response_time INT,
                                     response_time_ms INT,
                                     uptime_percentage DECIMAL(5,2),

                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                     CONSTRAINT uk_model_id UNIQUE (model_id)
);
CREATE INDEX idx_status ON model_health_status (status);
CREATE INDEX idx_last_check ON model_health_status (last_check_time);

CREATE TABLE model_request_log (
                                   id BIGSERIAL PRIMARY KEY,
                                   request_id VARCHAR(64) NOT NULL,

                                   model_id BIGINT NOT NULL,
                                   model_name VARCHAR(100),

                                   request_type VARCHAR(20) NOT NULL,
                                   prompt_tokens INT,
                                   completion_tokens INT,
                                   total_tokens INT,

                                   response_status VARCHAR(20) NOT NULL,
                                   response_time_ms INT,
                                   error_message TEXT,

                                   user_id BIGINT,
                                   source VARCHAR(100),

                                   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_model_id ON model_request_log (model_id);
CREATE INDEX idx_request_id ON model_request_log (request_id);
CREATE INDEX idx_created_at ON model_request_log (created_at);
CREATE INDEX idx_response_status ON model_request_log (response_status);
CREATE INDEX idx_user_id ON model_request_log (user_id);

-- 插入默认路由策略
INSERT INTO routing_strategy (name, description, strategy_type, config, is_default, enabled)
VALUES ('默认轮询策略', '按顺序依次选择可用模型', 'ROUND_ROBIN', '{}', 1, 1);


CREATE TABLE prompt_template (
                                   id BIGSERIAL PRIMARY KEY,
                                   char_id BIGINT NOT NULL,
                                   description VARCHAR(255) DEFAULT NULL,

                                   model_code VARCHAR(64) DEFAULT NULL,
                                   lang VARCHAR(16) DEFAULT 'zh-CN',

                                   content TEXT NOT NULL,
                                   param_schema JSON DEFAULT NULL,

                                   version INT NOT NULL DEFAULT 1,
                                   is_latest BOOLEAN NOT NULL DEFAULT TRUE,
                                   status SMALLINT NOT NULL DEFAULT 0,


                                   gray_strategy SMALLINT NOT NULL DEFAULT 0,
                                   gray_ratio INT DEFAULT NULL,
                                   gray_user_list JSON DEFAULT NULL,
                                   priority INT NOT NULL DEFAULT 0,
                                   tags JSON DEFAULT NULL,

                                   post_process_pipeline_id BIGINT DEFAULT NULL,
                                   post_process_config JSON DEFAULT NULL,

                                   created_by VARCHAR(64) DEFAULT NULL,
                                   updated_by VARCHAR(64) DEFAULT NULL,

                                   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   is_deleted BOOLEAN NOT NULL DEFAULT FALSE,

                                   CONSTRAINT uk_char_id_version UNIQUE (char_id, version)
);
CREATE INDEX idx_char_id_latest ON prompt_template (char_id, is_latest, status);
CREATE INDEX idx_gray ON prompt_template (gray_strategy, status);
CREATE INDEX idx_prompt_post_process_pipeline ON prompt_template (post_process_pipeline_id);

CREATE TABLE post_process_pipeline (
                                      id BIGSERIAL PRIMARY KEY,
                                      name VARCHAR(128) NOT NULL,
                                      description VARCHAR(255),

                                      lang VARCHAR(16) DEFAULT 'zh-CN',
                                      model_code VARCHAR(64) DEFAULT NULL,

                                      version INT NOT NULL DEFAULT 1,
                                      is_latest BOOLEAN NOT NULL DEFAULT TRUE,
                                      status SMALLINT NOT NULL DEFAULT 0,

                                      gray_strategy SMALLINT NOT NULL DEFAULT 0,
                                      gray_ratio INT DEFAULT NULL,
                                      gray_user_list JSON DEFAULT NULL,

                                      tags JSON DEFAULT NULL,

                                      created_by VARCHAR(64) DEFAULT NULL,
                                      updated_by VARCHAR(64) DEFAULT NULL,
                                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      is_deleted BOOLEAN NOT NULL DEFAULT FALSE,

                                      CONSTRAINT uk_post_process_pipeline_name_ver UNIQUE (name, version)
);
CREATE INDEX idx_post_process_pipeline_status ON post_process_pipeline (status, is_latest);

CREATE TABLE post_process_step (
                                  id BIGSERIAL PRIMARY KEY,
                                  pipeline_id BIGINT NOT NULL REFERENCES post_process_pipeline(id),
                                  step_order INT NOT NULL,
                                  step_type VARCHAR(32) NOT NULL,
                                  enabled BOOLEAN NOT NULL DEFAULT TRUE,

                                  config JSON NOT NULL,
                                  on_fail SMALLINT NOT NULL DEFAULT 0,
                                  priority INT NOT NULL DEFAULT 0,

                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                  CONSTRAINT uk_pipeline_order UNIQUE (pipeline_id, step_order)
);
CREATE INDEX idx_post_process_step_pipeline ON post_process_step (pipeline_id, enabled, step_order);

CREATE TABLE monthly_plans (
                                  id BIGSERIAL PRIMARY KEY,
                                  character_id BIGINT NOT NULL,
                                  day_rule VARCHAR(64) NOT NULL,
                                  start_time TIME NOT NULL,
                                  duration_min INT NOT NULL,
                                  location VARCHAR(255) NULL,
                                  action VARCHAR(512) NOT NULL,
                                  participants JSON NULL,
                                  extra JSON NULL,
                                  is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                                  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_character_id ON monthly_plans (character_id);
CREATE INDEX idx_character_deleted ON monthly_plans (character_id, is_deleted);

CREATE TABLE conversation_summary (
                                      id BIGSERIAL PRIMARY KEY,
                                      user_id BIGINT NOT NULL,
                                      ai_character_id BIGINT NOT NULL,
                                      session_id VARCHAR(64),
                                      summary_json JSONB NOT NULL,
                                      covered_until_message_id BIGINT NOT NULL,
                                      message_count INT NOT NULL CHECK (message_count > 0),
                                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 复合索引：按用户 + AI 角色 + 时间查询
CREATE INDEX idx_cs_user_char_created_at
    ON conversation_summary(user_id, ai_character_id, created_at DESC);

CREATE INDEX idx_cs_covered_message_id
    ON conversation_summary(covered_until_message_id);

CREATE EXTENSION IF NOT EXISTS vectors;

CREATE TABLE long_term_memory (
                                  id BIGSERIAL PRIMARY KEY,
                                  user_id BIGINT NOT NULL,
                                  ai_character_id BIGINT NOT NULL,
                                  text TEXT NOT NULL,
                                  embedding vector(1536) NOT NULL,
                                  memory_type VARCHAR(32) NOT NULL
                                      CHECK (memory_type IN ('event', 'preference', 'relationship', 'emotion', 'fact')),
                                  importance FLOAT NOT NULL
                                      CHECK (importance >= 0.0 AND importance <= 1.0),
                                  source_message_id BIGINT,
                                  last_accessed_at TIMESTAMP,
                                  access_count INT DEFAULT 0,
                                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 复合索引：用户 + AI 角色（向量检索前置过滤）
CREATE INDEX idx_ltm_user_char
    ON long_term_memory(user_id, ai_character_id);

CREATE INDEX idx_ltm_importance
    ON long_term_memory(importance DESC);

CREATE INDEX idx_ltm_type
    ON long_term_memory(memory_type);

-- 向量索引（IVFFlat）
CREATE INDEX idx_ltm_embedding
    ON long_term_memory
        USING vectors (embedding vector_cos_ops)
    WITH (options = $$
        [indexing.ivf]
        nlist = 100
    $$);
