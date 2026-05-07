package com.boonya.business.trip.rag.event;

import com.boonya.business.trip.common.constant.Scene;
import org.springframework.context.ApplicationEvent;
import org.springframework.web.multipart.MultipartFile;

/**
 * 政策上传事件
 */
public class UploadPolicyEvent extends ApplicationEvent {

    public UploadPolicyEvent(Scene scene, Long companyId, Long deptId, MultipartFile file) {
        super(new UploadPolicyEventSource(scene, companyId, deptId, file));
    }

    public static class UploadPolicyEventSource {
        private Scene scene;
        private Long companyId;
        private Long deptId;
        private MultipartFile file;

        public UploadPolicyEventSource(Scene scene, Long companyId, Long deptId, MultipartFile file) {
            this.scene = scene;
            this.companyId = companyId;
            this.deptId = deptId;
            this.file = file;
        }

        public Scene getScene() {
            return scene;
        }

        public void setScene(Scene scene) {
            this.scene = scene;
        }

        public Long getCompanyId() {
            return companyId;
        }

        public void setCompanyId(Long companyId) {
            this.companyId = companyId;
        }

        public Long getDeptId() {
            return deptId;
        }

        public void setDeptId(Long deptId) {
            this.deptId = deptId;
        }

        public MultipartFile getFile() {
            return file;
        }

        public void setFile(MultipartFile file) {
            this.file = file;
        }
    }
}
