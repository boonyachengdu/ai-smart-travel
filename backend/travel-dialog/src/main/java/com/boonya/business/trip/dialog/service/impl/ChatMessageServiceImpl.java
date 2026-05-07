package com.boonya.business.trip.dialog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.boonya.business.trip.common.entity.ChatMessage;
import com.boonya.business.trip.dialog.mapper.ChatMessageMapper;
import com.boonya.business.trip.dialog.service.ChatMessageService;
import org.springframework.stereotype.Service;

@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements ChatMessageService {
}
