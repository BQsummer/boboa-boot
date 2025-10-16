package com.bqsummer.controller;

import com.bqsummer.common.dto.auth.User;
import com.bqsummer.common.vo.Response;
import com.bqsummer.service.im.FriendService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 好友管理相关接口
 */
@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
@Validated
public class FriendController {

    private final FriendService friendService;

    /**
     * 添加好友
     * @param friendId 好友ID
     * @return 添加结果
     */
    @PostMapping("/{friendId}")
    public ResponseEntity<Response<Void>> addFriend(@PathVariable @NotNull Long friendId) {
        Long uid = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        friendService.addFriend(uid, friendId);
        return ResponseEntity.ok(Response.success());
    }

    /**
     * 删除好友
     * @param friendId 好友ID
     * @return 删除结果
     */
    @DeleteMapping("/{friendId}")
    public ResponseEntity<Response<Void>> removeFriend(@PathVariable @NotNull Long friendId) {
        Long uid = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        friendService.removeFriend(uid, friendId);
        return ResponseEntity.ok(Response.success());
    }

    /**
     * 获取好友列表
     * @return 好友列表
     */
    @GetMapping
    public ResponseEntity<List<User>> listFriends() {
        Long uid = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        List<User> friends = friendService.listFriends(uid);
        return ResponseEntity.ok(friends);
    }

    /**
     * 检查是否为好友关系
     * @param friendId 好友ID
     * @return 是否为好友
     */
    @GetMapping("/{friendId}/status")
    public ResponseEntity<Boolean> isFriend(@PathVariable @NotNull Long friendId) {
        Long uid = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        boolean isFriend = friendService.isFriend(uid, friendId);
        return ResponseEntity.ok(isFriend);
    }

    /**
     * 搜索用户（用于添加好友）
     * @param keyword 搜索关键词（用户名或昵称）
     * @return 用户列表
     */
    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(@RequestParam String keyword) {
        Long uid = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        List<User> users = friendService.searchUsers(keyword, uid);
        return ResponseEntity.ok(users);
    }
}
