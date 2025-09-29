package com.bqsummer.controller;

import com.bqsummer.common.dto.User;
import com.bqsummer.service.im.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @PostMapping("/{friendId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> addFriend(@PathVariable Long friendId) {
        Long uid = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        friendService.addFriend(uid, friendId);
        return ResponseEntity.ok("添加好友成功");
    }

    @DeleteMapping("/{friendId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> removeFriend(@PathVariable Long friendId) {
        Long uid = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        friendService.removeFriend(uid, friendId);
        return ResponseEntity.ok("删除好友成功");
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<User>> listFriends() {
        Long uid = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        List<User> friends = friendService.listFriends(uid);
        return ResponseEntity.ok(friends);
    }
}

