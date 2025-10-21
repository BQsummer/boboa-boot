-- 功能：AI角色与用户账户自动绑定
-- 日期：2025-10-21
-- 描述：为 ai_characters 表添加 associated_user_id 字段，用于关联自动创建的用户账户

-- 添加关联用户ID字段
ALTER TABLE ai_characters 
ADD COLUMN associated_user_id BIGINT NULL 
COMMENT '关联的用户账户ID（AI角色对应的User记录）'
AFTER updated_time;

-- 创建索引以优化反向查询（从User查找对应的AiCharacter）
CREATE INDEX idx_associated_user_id ON ai_characters(associated_user_id);

-- 注意：此字段允许NULL，以便兼容已存在的AI角色记录
-- 新创建的AI角色会自动填充此字段
