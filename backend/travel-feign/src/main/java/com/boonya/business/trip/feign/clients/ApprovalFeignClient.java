package com.boonya.business.trip.feign.clients;

import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.common.config.FeignClientConfig;
import com.boonya.business.trip.common.entity.Approval;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(
        name = "travel-approval",
        path = "/approval",
        configuration = FeignClientConfig.class
)
public interface ApprovalFeignClient {

    /**
     * 回调审批操作（通过/拒绝）
     */
    @PutMapping("/callback/{id}/approve")
    public Response<Boolean> approveCallback(@PathVariable("id") Long id, @RequestBody Approval approval);

    @PostMapping("save")
    public Response<Long> save(@RequestBody Approval approval);

    @GetMapping("/pendingApprovals")
    public Response<Long> pendingApprovals();

    @GetMapping("/{id}")
    public Response<Approval> getById(@PathVariable("id") Long id);

    @PostMapping("/unApprovalList")
    public Response<List<Approval>> getUnApprovalList();
}
