package com.boonya.business.trip.rag.event.listener;

import com.alibaba.fastjson.JSONObject;
import com.boonya.business.trip.rag.event.UploadOssEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UploadOssEventListener implements ApplicationListener<UploadOssEvent> {
    @Override
    public void onApplicationEvent(UploadOssEvent event) {
        UploadOssEvent.UploadOssEventSource source = (UploadOssEvent.UploadOssEventSource) event.getSource();
        // TODO 暂时不支持上传到oss
        log.info("上传文件成功：{}", JSONObject.toJSONString(source));
    }
}
