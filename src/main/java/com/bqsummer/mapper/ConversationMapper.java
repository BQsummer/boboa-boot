package com.bqsummer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bqsummer.common.dto.im.Conversation;
import com.bqsummer.common.vo.resp.im.ConversationItem;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {

    @Select("SELECT c.user_id, c.peer_id, c.unread_count, c.last_message_time, " +
            "m.type AS last_message_type, m.content AS last_message_content, " +
            "u.username AS peer_username, u.nick_name AS peer_nick_name, u.avatar AS peer_avatar " +
            "FROM conversations c " +
            "LEFT JOIN `message` m ON c.last_message_id = m.id " +
            "JOIN users u ON c.peer_id = u.id " +
            "WHERE c.user_id = #{userId} AND c.is_deleted = 0 " +
            "ORDER BY c.updated_time DESC")
    @Results({
            @Result(property = "peerId", column = "peer_id"),
            @Result(property = "unreadCount", column = "unread_count"),
            @Result(property = "lastMessageTime", column = "last_message_time"),
            @Result(property = "lastMessageType", column = "last_message_type"),
            @Result(property = "lastMessageContent", column = "last_message_content"),
            @Result(property = "peerUsername", column = "peer_username"),
            @Result(property = "peerNickName", column = "peer_nick_name"),
            @Result(property = "peerAvatar", column = "peer_avatar")
    })
    List<ConversationItem> listConversations(Long userId);

    @Insert("INSERT INTO conversations (user_id, peer_id, last_message_id, last_message_time, unread_count, is_deleted, created_time, updated_time) " +
            "VALUES (#{userId}, #{peerId}, #{messageId}, #{messageTime}, 0, 0, NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE last_message_id=VALUES(last_message_id), last_message_time=VALUES(last_message_time), updated_time=NOW(), is_deleted=0")
    int upsertSender(@Param("userId") Long userId,
                     @Param("peerId") Long peerId,
                     @Param("messageId") Long messageId,
                     @Param("messageTime") LocalDateTime messageTime);

    @Insert("INSERT INTO conversations (user_id, peer_id, last_message_id, last_message_time, unread_count, is_deleted, created_time, updated_time) " +
            "VALUES (#{userId}, #{peerId}, #{messageId}, #{messageTime}, 1, 0, NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE last_message_id=VALUES(last_message_id), last_message_time=VALUES(last_message_time), unread_count = unread_count + 1, updated_time=NOW(), is_deleted=0")
    int upsertReceiver(@Param("userId") Long userId,
                       @Param("peerId") Long peerId,
                       @Param("messageId") Long messageId,
                       @Param("messageTime") LocalDateTime messageTime);

    @Insert("INSERT INTO conversations (user_id, peer_id, unread_count, is_deleted, created_time, updated_time) " +
            "VALUES (#{userId}, #{peerId}, 0, 0, NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE is_deleted=0, updated_time=NOW()")
    int insertOrRestore(@Param("userId") Long userId, @Param("peerId") Long peerId);

    @Update("UPDATE conversations SET is_deleted=1, unread_count=0, updated_time=NOW() WHERE user_id=#{userId} AND peer_id=#{peerId}")
    int softDelete(@Param("userId") Long userId, @Param("peerId") Long peerId);
}
