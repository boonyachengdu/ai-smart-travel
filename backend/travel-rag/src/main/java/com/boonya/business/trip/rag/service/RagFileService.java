package com.boonya.business.trip.rag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.boonya.business.trip.common.constant.Scene;
import com.boonya.business.trip.common.entity.RagFile;
import org.springframework.web.multipart.MultipartFile;

public interface RagFileService extends IService<RagFile> {

    boolean uploadOss(Scene scene, Long companyId, MultipartFile file);
}
