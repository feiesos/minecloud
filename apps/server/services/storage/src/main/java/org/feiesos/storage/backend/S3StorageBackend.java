package org.feiesos.storage.backend;

import org.feiesos.storage.dto.StorageObject;
import org.springframework.stereotype.Component;
import java.io.InputStream;
import java.util.List;

@Component
public class S3StorageBackend implements StorageBackend {

    @Override
    public StorageObject read(String storagePath) {
        throw new UnsupportedOperationException("S3 存储后端尚未实现");
    }

    @Override
    public void write(String storagePath, InputStream data, long size) {
        throw new UnsupportedOperationException("S3 存储后端尚未实现");
    }

    @Override
    public void delete(String storagePath) {
        throw new UnsupportedOperationException("S3 存储后端尚未实现");
    }

    @Override
    public void deleteDirectory(String storagePath) {
        throw new UnsupportedOperationException("S3 存储后端尚未实现");
    }

    @Override
    public List<String> list(String storagePath) {
        throw new UnsupportedOperationException("S3 存储后端尚未实现");
    }

    @Override
    public boolean exists(String storagePath) {
        throw new UnsupportedOperationException("S3 存储后端尚未实现");
    }

    @Override
    public StorageType type() {
        return StorageType.S3;
    }
}
