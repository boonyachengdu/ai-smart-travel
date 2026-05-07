package com.boonya.business.trip.feign.clients;

import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.common.config.FeignClientConfig;
import com.boonya.business.trip.common.entity.PolicyDocument;
import com.boonya.business.trip.common.models.dialog.DialogRequest;
import com.boonya.business.trip.common.models.rag.PolicyDocumentQueryRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@FeignClient(
        name = "travel-rag",
        path = "/policy",
        configuration = FeignClientConfig.class
)
public interface PolicyDocumentFeignClient {

    /**
     * 上传政策文档--生成矢量数据
     */
    @PostMapping("/upload")
    Response<Void> uploadPolicy(@RequestParam("scene") String scene, @RequestParam("file") MultipartFile file, @RequestParam("companyId") Long companyId,@RequestParam("deptId") Long deptId);

    /**
     * 搜索政策（智能检索）
     */
    @GetMapping("/search")
    Response<List<PolicyDocument>> searchPolicies(@RequestBody PolicyDocumentQueryRequest request);

    /**
     * 带员工差标校验的问答
     */
    @PostMapping("/check")
    public Response<String> policyCheck(@RequestBody DialogRequest request);
}
