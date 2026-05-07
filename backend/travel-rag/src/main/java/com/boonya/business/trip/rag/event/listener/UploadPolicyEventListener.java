package com.boonya.business.trip.rag.event.listener;

import com.boonya.business.trip.rag.event.UploadPolicyEvent;
import com.boonya.business.trip.rag.service.PolicyDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * 政策上传事件监听器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UploadPolicyEventListener implements ApplicationListener<UploadPolicyEvent> {

    private final PolicyDocumentService policyDocumentService;

    @Override
    public void onApplicationEvent(UploadPolicyEvent event) {
        UploadPolicyEvent.UploadPolicyEventSource source = (UploadPolicyEvent.UploadPolicyEventSource) event.getSource();
        log.info("上传政策事件监听器开始执行...");
        policyDocumentService.upload(source.getFile(), source.getCompanyId(), source.getDeptId(), source.getScene());
        log.info("上传政策事件监听器执行完毕...");
    }
}
