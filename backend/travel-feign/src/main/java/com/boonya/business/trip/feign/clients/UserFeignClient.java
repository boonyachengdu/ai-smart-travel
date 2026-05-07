package com.boonya.business.trip.feign.clients;

import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.common.config.FeignClientConfig;
import com.boonya.business.trip.common.entity.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * travel-user 服务的 Feign 声明式客户端
 * 注意：name 值必须与 travel-user 的 spring.application.name 完全一致
 */
@FeignClient(
        name = "travel-user",
        path = "/users",
        configuration = FeignClientConfig.class
)
public interface UserFeignClient {

    /**
     * 根据 ID 查询用户（getById）
     * 对应 travel-user 的 GET /users/{id}
     */
    @GetMapping("/{id}")
    Response<User> getUserById(@PathVariable("id") Long id);

    /**
     * 创建/保存用户（示例）
     * 对应 travel-user 的 POST /users
     */
    @PostMapping
    Response<Boolean> saveUser(@RequestBody User user);

    /**
     * 更新用户（示例）
     */
    @PutMapping("/{id}")
    Response<Boolean> updateUser(@PathVariable("id") Long id, @RequestBody User user);

    /**
     * 删除用户（逻辑删除示例）
     */
    @DeleteMapping("/{id}")
    Response<Boolean> deleteUser(@PathVariable("id") Long id);

    /**
     * 示例：带参数的复杂查询（可选）
     */
    @GetMapping("/search")
    Response<User> searchUser(@RequestParam(value = "username", required = false) String username, @RequestParam(value = "phone", required = false) String phone);

    @GetMapping("/countUsers")
    public Response<Long> countUsers();

    @GetMapping("/count/company/{companyId}")
    public Response<Long> countByCompanyId(@PathVariable("companyId") Long companyId);
}
