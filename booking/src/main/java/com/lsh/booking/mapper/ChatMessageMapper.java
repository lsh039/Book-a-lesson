package com.lsh.booking.mapper;

import com.lsh.booking.entity.ChatMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author lsh
 * @since 2026-04-12
 */
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /*
    * 获取未读消息
    * */
        @Select("""
    SELECT *
    FROM chat_message
    WHERE session_id IN (
        SELECT session_id
        FROM chat_message
        WHERE from_user_id = #{userId} OR to_user_id = #{userId}
        GROUP BY session_id
    )
    ORDER BY create_time DESC
    """)
    List<ChatMessage> getUnread(Long userId);


        /*
        *
        * */
    @Update("UPDATE chat_message \n" +
            "SET status = 1 \n" +
            "WHERE to_user_id = #{userId} AND status = 0")
    void markRead(Long userId);

    /*
    * 获取最近的聊天记录
    * */
    @Select("""
    SELECT t.*
    FROM chat_message t
    INNER JOIN (
        SELECT session_id, MAX(create_time) AS max_time
        FROM chat_message
        WHERE from_user_id = #{userId} OR to_user_id = #{userId}
        GROUP BY session_id
    ) tmp
    ON t.session_id = tmp.session_id AND t.create_time = tmp.max_time
    ORDER BY t.create_time DESC
    """)
    List<ChatMessage> getRecentChats(Long userId);

    // 获取未读消息统计
    @Select("SELECT session_id as sessionId, COUNT(*) as unreadCount " +
            "FROM chat_message " +
            "WHERE to_user_id = #{userId} AND status = 0 " +
            "GROUP BY session_id")
    List<Map<String, Object>> getUnreadCountList(@Param("userId") Long userId);

    /*
    * 获取未读消息统计
    * */
    @Select("""
    SELECT session_id, COUNT(*) as unreadCount
    FROM chat_message
    WHERE to_user_id = #{userId} AND status = 0
    GROUP BY session_id
    """)
    List<Map<String, Object>> getUnreadCount(Long userId);
}
