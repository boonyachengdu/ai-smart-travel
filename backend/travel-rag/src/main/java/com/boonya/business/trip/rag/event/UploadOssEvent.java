package com.boonya.business.trip.rag.event;

import com.boonya.business.trip.common.entity.RagFile;
import org.springframework.context.ApplicationEvent;
import org.springframework.web.multipart.MultipartFile;

public class UploadOssEvent extends ApplicationEvent {
    public UploadOssEvent(MultipartFile file, RagFile ragFile) {
        super(new UploadOssEventSource(file, ragFile));
    }

    public static class UploadOssEventSource {
        private MultipartFile file;
        RagFile ragFile;

        public UploadOssEventSource(MultipartFile file, RagFile ragFile) {
            this.file = file;
            this.ragFile = ragFile;
        }

        public MultipartFile getFile() {
            return file;
        }

        public void setFile(MultipartFile file) {
            this.file = file;
        }

        public RagFile getRagFile() {
            return ragFile;
        }

        public void setRagFile(RagFile ragFile) {
            this.ragFile = ragFile;
        }
    }
}
