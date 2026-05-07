package com.boonya.business.trip.rag.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.boonya.business.trip.common.entity.Prompts;
import com.boonya.business.trip.rag.mapper.PromptMapper;
import com.boonya.business.trip.rag.service.PromptService;
import org.springframework.stereotype.Service;

@Service
public class PromptServiceImpl extends ServiceImpl<PromptMapper, Prompts> implements PromptService {

}
