package org.feiesos.storage.config;

import org.feiesos.storage.backend.StorageType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "minecloud.storage")
public class StorageProperties {

    private String uploadPath = "./data/storage";
    private StorageType defaultType = StorageType.LOCAL;
    private Chunk chunk = new Chunk();
    private Recycle recycle = new Recycle();

    public String getUploadPath() {
        return uploadPath;
    }

    public void setUploadPath(String uploadPath) {
        this.uploadPath = uploadPath;
    }

    public StorageType getDefaultType() {
        return defaultType;
    }

    public void setDefaultType(StorageType defaultType) {
        this.defaultType = defaultType;
    }

    public Chunk getChunk() {
        return chunk;
    }

    public void setChunk(Chunk chunk) {
        this.chunk = chunk;
    }

    public Recycle getRecycle() {
        return recycle;
    }

    public void setRecycle(Recycle recycle) {
        this.recycle = recycle;
    }

    public static class Chunk {
        private String tempDir = "temp";

        public String getTempDir() {
            return tempDir;
        }

        public void setTempDir(String tempDir) {
            this.tempDir = tempDir;
        }
    }

    public static class Recycle {
        private int retentionDays = 30;
        private String cleanupCron = "0 0 3 * * ?";

        public int getRetentionDays() {
            return retentionDays;
        }

        public void setRetentionDays(int retentionDays) {
            this.retentionDays = retentionDays;
        }

        public String getCleanupCron() {
            return cleanupCron;
        }

        public void setCleanupCron(String cleanupCron) {
            this.cleanupCron = cleanupCron;
        }
    }
}
