package com.boonya.business.trip.common.aop;

import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.common.component.cache.CacheService;
import com.boonya.business.trip.common.entity.BaseEntity;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.util.*;

@Slf4j
@Aspect
@Component
public class EntityNameFillAspect {

    @Autowired
    private CacheService cacheService;

    @Around("execution(* com..controller.*Controller.*(..))")
    public Object fillEntityNames(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();

        if (result == null) {
            return result;
        }

        try {
            if (result instanceof Response) {
                handleResponse((Response<?>) result);
            } else if (result instanceof BaseEntity) {
                fillSingleEntity((BaseEntity) result);
            } else if (result instanceof Collection) {
                fillBatchEntities((Collection<?>) result);
            } else if (isPageObject(result)) {
                handlePageResult(result);
            }
        } catch (Exception e) {
            log.error("填充实体名称失败", e);
        }

        return result;
    }

    private void handleResponse(Response<?> response) {
        Object data = response.getData();
        if (data == null) {
            return;
        }

        if (data instanceof BaseEntity) {
            fillSingleEntity((BaseEntity) data);
        } else if (data instanceof Collection) {
            fillBatchEntities((Collection<?>) data);
        } else if (isPageObject(data)) {
            handlePageResult(data);
        }
    }

    private boolean isPageObject(Object obj) {
        return obj.getClass().getName().equals("com.baomidou.mybatisplus.extension.plugins.pagination.Page");
    }

    private void handlePageResult(Object pageResult) {
        try {
            Field recordsField = pageResult.getClass().getDeclaredField("records");
            recordsField.setAccessible(true);
            Object records = recordsField.get(pageResult);
            if (records instanceof Collection) {
                fillBatchEntities((Collection<?>) records);
            }
        } catch (Exception e) {
            log.error("处理分页结果失败", e);
        }
    }

    private void fillSingleEntity(BaseEntity entity) {
        try {
            Class<?> clazz = entity.getClass();

            Field userIdField = getField(clazz, "userId");
            Field companyIdField = getField(clazz, "companyId");
            Field departmentIdField = getField(clazz, "departmentId");
            Field applicantIdField = getField(clazz, "applicantId");
            Field approverIdField = getField(clazz, "approverId");

            if (userIdField != null) {
                userIdField.setAccessible(true);
                Long userId = (Long) userIdField.get(entity);
                if (userId != null) {
                    String username = cacheService.getUsernameById(userId);
                    entity.setUsername(username);
                }
            }

            if (companyIdField != null) {
                companyIdField.setAccessible(true);
                Long companyId = (Long) companyIdField.get(entity);
                if (companyId != null) {
                    String companyName = cacheService.getCompanyNameById(companyId);
                    entity.setCompanyName(companyName);
                }
            }

            if (departmentIdField != null) {
                departmentIdField.setAccessible(true);
                Long deptId = (Long) departmentIdField.get(entity);
                if (deptId != null) {
                    String deptName = cacheService.getDepartmentNameById(deptId);
                    entity.setDepartmentName(deptName);
                }
            }

            if (applicantIdField != null){
                applicantIdField.setAccessible(true);
                Long applicantId = (Long) applicantIdField.get(entity);
                if (applicantId != null) {
                    String applicantName = cacheService.getEmployeeNameById(applicantId);
                    entity.setApplicantEmployeeName(applicantName);
                }
            }
            if (approverIdField != null){
                approverIdField.setAccessible(true);
                Long approverId = (Long) approverIdField.get(entity);
                if (approverId != null) {
                    String approverName = cacheService.getEmployeeNameById(approverId);
                    entity.setApproverEmployeeName(approverName);
                }
            }
        } catch (Exception e) {
            log.warn("填充单个实体名称失败：{}", e.getMessage());
        }
    }

    private void fillBatchEntities(Collection<?> entities) {
        if (CollectionUtils.isEmpty(entities)) {
            return;
        }

        Set<Long> userIds = new HashSet<>();
        Set<Long> companyIds = new HashSet<>();
        Set<Long> deptIds = new HashSet<>();
        Set<Long> applicantIds = new HashSet<>();
        Set<Long> approverIds = new HashSet<>();
        List<BaseEntity> entityList = new ArrayList<>();

        for (Object obj : entities) {
            if (obj instanceof BaseEntity) {
                entityList.add((BaseEntity) obj);
                collectIds((BaseEntity) obj, userIds, companyIds, deptIds, applicantIds, approverIds);
            }
        }

        Map<Long, String> usernameMap = cacheService.getUsernamesByIds(userIds);
        Map<Long, String> companyNameMap = cacheService.getCompanyNamesByIds(companyIds);
        Map<Long, String> deptNameMap = cacheService.getDepartmentNamesByIds(deptIds);
        Map<Long, String> applicantNameMap = cacheService.getEmployeeNamesByIds(applicantIds);
        Map<Long, String> approverNameMap = cacheService.getEmployeeNamesByIds(approverIds);

        for (BaseEntity entity : entityList) {
            try {
                Class<?> clazz = entity.getClass();

                Field userIdField = getField(clazz, "userId");
                if (userIdField != null) {
                    userIdField.setAccessible(true);
                    Long userId = (Long) userIdField.get(entity);
                    if (userId != null && usernameMap.containsKey(userId)) {
                        entity.setUsername(usernameMap.get(userId));
                    }
                }

                Field companyIdField = getField(clazz, "companyId");
                if (companyIdField != null) {
                    companyIdField.setAccessible(true);
                    Long companyId = (Long) companyIdField.get(entity);
                    if (companyId != null && companyNameMap.containsKey(companyId)) {
                        entity.setCompanyName(companyNameMap.get(companyId));
                    }
                }

                Field departmentIdField = getField(clazz, "departmentId");
                if (departmentIdField != null) {
                    departmentIdField.setAccessible(true);
                    Long deptId = (Long) departmentIdField.get(entity);
                    if (deptId != null && deptNameMap.containsKey(deptId)) {
                        entity.setDepartmentName(deptNameMap.get(deptId));
                    }
                }

                Field applicantIdField = getField(clazz, "applicantId");
                if (applicantIdField != null) {
                    applicantIdField.setAccessible(true);
                    Long applicantId = (Long) applicantIdField.get(entity);
                    if (applicantId != null && applicantNameMap.containsKey(applicantId)) {
                        entity.setApplicantEmployeeName(applicantNameMap.get(applicantId));
                    }
                }

                Field approverIdField = getField(clazz, "approverId");
                if (approverIdField != null) {
                    approverIdField.setAccessible(true);
                    Long approverId = (Long) approverIdField.get(entity);
                    if (approverId != null && approverNameMap.containsKey(approverId)) {
                        entity.setApproverEmployeeName(approverNameMap.get(approverId));
                    }
                }
            } catch (Exception e) {
                log.warn("填充批量实体名称失败：{}", e.getMessage());
            }
        }
    }

    private void collectIds(BaseEntity entity, Set<Long> userIds,
                            Set<Long> companyIds, Set<Long> deptIds,
                            Set<Long> applicantIds, Set<Long> approverIds) {
        try {
            Class<?> clazz = entity.getClass();

            Field userIdField = getField(clazz, "userId");
            if (userIdField != null) {
                userIdField.setAccessible(true);
                Long userId = (Long) userIdField.get(entity);
                if (userId != null) {
                    userIds.add(userId);
                }
            }

            Field companyIdField = getField(clazz, "companyId");
            if (companyIdField != null) {
                companyIdField.setAccessible(true);
                Long companyId = (Long) companyIdField.get(entity);
                if (companyId != null) {
                    companyIds.add(companyId);
                }
            }

            Field departmentIdField = getField(clazz, "departmentId");
            if (departmentIdField != null) {
                departmentIdField.setAccessible(true);
                Long deptId = (Long) departmentIdField.get(entity);
                if (deptId != null) {
                    deptIds.add(deptId);
                }
            }

            Field applicantIdField = getField(clazz, "applicantId");
            if (applicantIdField != null) {
                applicantIdField.setAccessible(true);
                Long applicantId = (Long) applicantIdField.get(entity);
                if (applicantId != null) {
                    applicantIds.add(applicantId);
                }
            }

            Field approverIdField = getField(clazz, "approverId");
            if (approverIdField != null) {
                approverIdField.setAccessible(true);
                Long approverId = (Long) approverIdField.get(entity);
                if (approverId != null) {
                    approverIds.add(approverId);
                }
            }
        } catch (Exception e) {
            log.warn("收集 ID 失败：{}", e.getMessage());
        }
    }

    private Field getField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() != null && clazz.getSuperclass() != Object.class) {
                return getField(clazz.getSuperclass(), fieldName);
            }
            return null;
        }
    }
}
