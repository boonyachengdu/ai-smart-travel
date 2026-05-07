package com.boonya.business.trip.rag.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.boonya.business.trip.common.constant.Scene;
import com.boonya.business.trip.common.context.UserHolder;
import com.boonya.business.trip.common.entity.RagFile;
import com.boonya.business.trip.rag.event.UploadOssEvent;
import com.boonya.business.trip.rag.mapper.RagFileMapper;
import com.boonya.business.trip.rag.service.RagFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RagFileServiceImpl extends ServiceImpl<RagFileMapper, RagFile> implements RagFileService {

    private final ApplicationEventPublisher publisher;

    @Override
    public boolean uploadOss(Scene scene, Long companyId, MultipartFile file) {
        UserHolder userHolder = UserHolder.get();
        // 实际项目中：上传到 OSS/MinIO，返回 filePath
        String filePath = "oss://bucket/" + file.getOriginalFilename();
        String description = file.getOriginalFilename();
        RagFile ragFile = new RagFile();
        ragFile.setScene(scene);
        ragFile.setFileName(file.getOriginalFilename());
        ragFile.setFilePath(filePath);
        ragFile.setFileSize(file.getSize());
        ragFile.setMimeType(file.getContentType());
        ragFile.setDescription(description);
        ragFile.setCompanyId(Objects.isNull(companyId) ? userHolder.getCompanyId() : companyId);
        // 当前用户（从 token 获取）
        Long currentUserId = userHolder.getUserId();
        ragFile.setUserId(currentUserId);
        ragFile.setCreateTime(LocalDateTime.now());
        ragFile.setUpdateTime(LocalDateTime.now());

        boolean result = save(ragFile);
        if (result) {
            // TODO 触发上传 OSS 事件
            //publisher.publishEvent(new UploadOssEvent(file,ragFile));
        }

        return result;
    }
}
