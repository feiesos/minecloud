package org.feiesos.storage.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.feiesos.common.exception.BusinessException;
import org.feiesos.storage.config.StorageProperties;
import org.feiesos.storage.dto.StorageObject;
import org.feiesos.storage.entity.FileNode;
import org.feiesos.storage.mapper.FileNodeMapper;
import org.feiesos.storage.service.AuthzService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageFacadeTest {

    @Mock
    private AuthzService authzService;

    @Mock
    private StorageRouter storageRouter;

    @Mock
    private FileNodeMapper fileNodeMapper;

    @Mock
    private StorageBackend storageBackend;

    @Mock
    private StorageProperties storageProperties;

    @Mock
    private ObjectMapper objectMapper;

    private StorageFacade storageFacade;

    @BeforeEach
    void setUp() {
        storageFacade = new StorageFacade(authzService, storageRouter, fileNodeMapper, storageProperties, objectMapper);
    }

    @Test
    void download_shouldRejectWhenAncestorDirectoryIsDeleted() {
        FileNode file = fileNode(10L, 5L, false, 1L, false, "report.pdf", "objects/report.pdf");
        FileNode deletedParent = fileNode(5L, 0L, true, 1L, true, "archive", null);

        doNothing().when(authzService).checkPermission(1L, "file:read");
        when(fileNodeMapper.selectById(10L)).thenReturn(file);
        when(fileNodeMapper.selectByIdIncludingDeleted(5L)).thenReturn(deletedParent);

        BusinessException exception = assertThrows(BusinessException.class, () -> storageFacade.download(10L, 1L));

        assertEquals("文件不存在", exception.getMessage());
        verify(storageRouter, never()).route(file);
        verify(storageBackend, never()).read("objects/report.pdf");
    }

    @Test
    void download_shouldReadFileWhenAncestorsAreAccessible() {
        FileNode file = fileNode(10L, 5L, false, 1L, false, "report.pdf", "objects/report.pdf");
        FileNode parent = fileNode(5L, 0L, true, 1L, false, "docs", null);
        StorageObject storageObject = StorageObject.builder()
                .filename("report.pdf")
                .size(3L)
                .inputStream(new ByteArrayInputStream(new byte[]{1, 2, 3}))
                .build();

        doNothing().when(authzService).checkPermission(1L, "file:read");
        when(fileNodeMapper.selectById(10L)).thenReturn(file);
        when(fileNodeMapper.selectByIdIncludingDeleted(5L)).thenReturn(parent);
        when(storageRouter.route(file)).thenReturn(storageBackend);
        when(storageBackend.read("objects/report.pdf")).thenReturn(storageObject);

        StorageObject result = storageFacade.download(10L, 1L);

        assertNotNull(result);
        assertEquals("report.pdf", result.getFilename());
        verify(storageRouter).route(file);
        verify(storageBackend).read("objects/report.pdf");
    }

    private static FileNode fileNode(Long id,
                                     Long parentId,
                                     boolean isDir,
                                     Long ownerId,
                                     boolean isDeleted,
                                     String name,
                                     String storagePath) {
        FileNode node = new FileNode();
        node.setId(id);
        node.setParentId(parentId);
        node.setIsDir(isDir);
        node.setOwnerId(ownerId);
        node.setIsDeleted(isDeleted);
        node.setName(name);
        node.setStoragePath(storagePath);
        return node;
    }
}
