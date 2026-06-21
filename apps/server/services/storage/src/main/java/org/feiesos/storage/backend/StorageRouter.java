package org.feiesos.storage.backend;

import org.feiesos.storage.config.StorageProperties;
import org.feiesos.storage.entity.FileNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class StorageRouter {

    private static final Logger log = LoggerFactory.getLogger(StorageRouter.class);

    private final Map<StorageType, StorageBackend> backends;
    private final StorageType defaultType;

    public StorageRouter(List<StorageBackend> backendList, StorageProperties properties) {
        this.backends = backendList.stream()
                .collect(Collectors.toMap(StorageBackend::type, Function.identity(), (a, b) -> a));
        this.defaultType = properties.getDefaultType();
        if (!backends.containsKey(defaultType)) {
            throw new IllegalStateException("默认存储后端未注册: " + defaultType);
        }
        log.info("已注册存储后端: {}", backends.keySet());
    }

    public StorageBackend route(FileNode node) {
        if (node != null && node.getStorageType() != null) {
            try {
                StorageType type = StorageType.valueOf(node.getStorageType());
                StorageBackend backend = backends.get(type);
                if (backend != null) {
                    return backend;
                }
                log.warn("存储后端 {} 未注册，回退到默认后端 {}", type, defaultType);
            } catch (IllegalArgumentException e) {
                log.warn("无效的存储类型: {}，回退到默认后端 {}", node.getStorageType(), defaultType);
            }
        }
        return backends.get(defaultType);
    }

    public StorageBackend defaultBackend() {
        return backends.get(defaultType);
    }
}
