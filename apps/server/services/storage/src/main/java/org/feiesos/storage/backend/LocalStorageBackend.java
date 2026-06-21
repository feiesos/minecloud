package org.feiesos.storage.backend;

import org.feiesos.common.exception.BusinessException;
import org.feiesos.storage.config.StorageProperties;
import org.feiesos.storage.dto.StorageObject;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class LocalStorageBackend implements StorageBackend {

    private final Path basePath;

    public LocalStorageBackend(StorageProperties properties) {
        this.basePath = Path.of(properties.getUploadPath()).normalize();
        try {
            Files.createDirectories(this.basePath);
        } catch (IOException e) {
            throw new RuntimeException("存储目录初始化失败: " + this.basePath, e);
        }
    }

    private Path resolve(String storagePath) {
        Path resolved = basePath.resolve(storagePath).normalize();
        if (!resolved.startsWith(basePath)) {
            throw new BusinessException("非法的存储路径: " + storagePath);
        }
        return resolved;
    }

    @Override
    public StorageObject read(String storagePath) {
        Path file = resolve(storagePath);
        if (!Files.exists(file) || Files.isDirectory(file)) {
            throw new BusinessException("物理文件不存在: " + storagePath);
        }
        try {
            return StorageObject.builder()
                    .filename(file.getFileName().toString())
                    .size(Files.size(file))
                    .inputStream(Files.newInputStream(file))
                    .build();
        } catch (IOException e) {
            throw new UncheckedIOException("读取文件失败: " + storagePath, e);
        }
    }

    @Override
    public void write(String storagePath, InputStream data, long size) {
        Path file = resolve(storagePath);
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (OutputStream os = Files.newOutputStream(file)) {
                data.transferTo(os);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("写入文件失败: " + storagePath, e);
        }
    }

    @Override
    public void delete(String storagePath) {
        Path file = resolve(storagePath);
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new UncheckedIOException("删除文件失败: " + storagePath, e);
        }
    }

    @Override
    public void deleteDirectory(String storagePath) {
        Path dir = resolve(storagePath);
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw new UncheckedIOException("清理目录失败", e);
                        }
                    });
        } catch (IOException e) {
            throw new UncheckedIOException("遍历目录失败: " + storagePath, e);
        }
    }

    @Override
    public List<String> list(String storagePath) {
        Path dir = resolve(storagePath);
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(Files::isRegularFile)
                    .map(f -> storagePath + "/" + f.getFileName().toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException("列出目录失败: " + storagePath, e);
        }
    }

    @Override
    public boolean exists(String storagePath) {
        return Files.exists(resolve(storagePath));
    }

    @Override
    public StorageType type() {
        return StorageType.LOCAL;
    }
}
