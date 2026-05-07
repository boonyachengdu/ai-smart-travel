package com.boonya.business.trip.dialog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.common.context.UserHolder;
import com.boonya.business.trip.common.entity.ChatSession;
import com.boonya.business.trip.common.entity.ChatMessage;
import com.boonya.business.trip.common.models.dialog.SessionQueryRequest;
import com.boonya.business.trip.dialog.service.ChatSessionService;
import com.boonya.business.trip.dialog.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/session")
@RequiredArgsConstructor
public class ChatSessionController {

    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;

    /**
     * 分页查询会话列表
     */
    @PostMapping("/page")
    public Response<Page<ChatSession>> page(@RequestBody SessionQueryRequest request) {
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getDeleted, 0);

        // 过滤用户企业
        UserHolder userHolder = UserHolder.get();
        if (!userHolder.isSuperAdmin()){
            wrapper.eq(ChatSession::getCompanyId, userHolder.getCompanyId());
        }

        if (request.getUserId() != null) {
            wrapper.eq(ChatSession::getUserId, request.getUserId());
        }
        if (request.getCompanyId() != null) {
            wrapper.eq(ChatSession::getCompanyId, request.getCompanyId());
        }
        if (StringUtils.hasText(request.getScene())) {
            wrapper.eq(ChatSession::getScene, request.getScene());
        }
        if (StringUtils.hasText(request.getStatus())) {
            wrapper.eq(ChatSession::getStatus, request.getStatus());
        }

        wrapper.orderByDesc(ChatSession::getLastActiveTime);

        Page<ChatSession> page = chatSessionService.page(
                new Page<>(request.getPage(), request.getSize()),
                wrapper
        );

        return Response.ok(page);
    }

    /**
     * 根据ID查询单个会话详情
     */
    @GetMapping("/{id}")
    public Response<ChatSession> getById(@PathVariable("id") Long id) {
        ChatSession session = chatSessionService.getById(id);
        return session != null ? Response.ok(session) : Response.error("会话不存在");
    }

    /**
     * 获取会话的消息列表（按时间升序）
     */
    @GetMapping("/{id}/messages")
    public Response<List<ChatMessage>> getMessages(@PathVariable("id") Long id) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, id)
                .eq(ChatMessage::getDeleted, 0)
                .orderByDesc(ChatMessage::getCreateTime);

        List<ChatMessage> messages = chatMessageService.list(wrapper);
        return Response.ok(messages);
    }

    /**
     * 删除会话（逻辑删除会话及所有消息）
     */
    @DeleteMapping("/{id}")
    public Response<Boolean> delete(@PathVariable("id") Long id) {
        // 删除会话
        boolean sessionSuccess = chatSessionService.removeById(id);

        // 逻辑删除该会话的所有消息
        LambdaUpdateWrapper<ChatMessage> msgWrapper = new LambdaUpdateWrapper<>();
        msgWrapper.eq(ChatMessage::getSessionId, id)
                .set(ChatMessage::getDeleted, 1);
        chatMessageService.update(msgWrapper);

        return sessionSuccess ? Response.ok(true) : Response.error("删除失败");
    }
}