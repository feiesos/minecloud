package org.feiesos.storage.recycle.service.impl;

import org.feiesos.common.exception.BusinessException;
import org.feiesos.storage.backend.StorageBackend;
import org.feiesos.storage.backend.StorageRouter;
import org.feiesos.storage.config.StorageProperties;
import org.feiesos.storage.entity.FileNode;
import org.feiesos.storage.mapper.FileNodeMapper;
import org.feiesos.storage.recycle.dto.RecycleItemDTO;
import org.feiesos.storage.service.AuthzService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecycleServiceImplTest {

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

    private RecycleServiceImpl recycleService;

    @BeforeEach
    void setUp() {
        recycleService = new RecycleServiceImpl(authzService, storageRouter, fileNodeMapper, storageProperties);
    }

    @Test
    void moveToRecycleBin_shouldMarkWholeSubtreeDeleted() {
        FileNode root = node(10L, 0L, true, 1L, false, "root", null);
        FileNode childDir = node(11L, 10L, true, 1L, false, "dir", null);
        FileNode childFile = node(12L, 11L, false, 1L, false, "file.txt", "objects/file.txt");

        when(fileNodeMapper.selectById(10L)).thenReturn(root);
        when(fileNodeMapper.selectByParentIdsIncludingDeleted(argThat(idsMatch(10L)))).thenReturn(List.of(childDir));
        when(fileNodeMapper.selectByParentIdsIncludingDeleted(argThat(idsMatch(11L)))).thenReturn(List.of(childFile));
        when(fileNodeMapper.selectByParentIdsIncludingDeleted(argThat(idsMatch(12L)))).thenReturn(List.of());

        recycleService.moveToRecycleBin(10L, 1L);

        verify(authzService).checkPermission(1L, "file:delete");
        verify(fileNodeMapper).markDeleted(eq(1L), argThat(idsContainExactly(10L, 11L, 12L)), any(LocalDateTime.class));
    }

    @Test
    void listRecycleItems_shouldOnlyReturnRecycleRoots() {
        FileNode root = node(10L, 0L, true, 1L, true, "root", null);
        FileNode child = node(11L, 10L, false, 1L, true, "child.txt", "objects/child.txt");
        FileNode standalone = node(12L, 99L, false, 1L, true, "standalone.txt", "objects/standalone.txt");

        when(fileNodeMapper.selectDeletedByOwnerId(1L)).thenReturn(List.of(root, child, standalone));

        List<RecycleItemDTO> items = recycleService.listRecycleItems(1L);

        verify(authzService).checkPermission(1L, "file:read");
        assertEquals(List.of(10L, 12L), items.stream().map(RecycleItemDTO::getId).toList());
    }

    @Test
    void restore_shouldRestoreWholeSubtreeWhenParentIsActive() {
        FileNode root = node(10L, 5L, true, 1L, true, "root", null);
        FileNode parent = node(5L, 0L, true, 1L, false, "parent", null);
        FileNode child = node(11L, 10L, false, 1L, true, "child.txt", "objects/child.txt");

        when(fileNodeMapper.selectByIdIncludingDeleted(10L)).thenReturn(root);
        when(fileNodeMapper.selectByIdIncludingDeleted(5L)).thenReturn(parent);
        when(fileNodeMapper.selectCount(any())).thenReturn(0L);
        when(fileNodeMapper.selectByParentIdsIncludingDeleted(argThat(idsMatch(10L)))).thenReturn(List.of(child));
        when(fileNodeMapper.selectByParentIdsIncludingDeleted(argThat(idsMatch(11L)))).thenReturn(List.of());

        recycleService.restore(10L, 1L);

        verify(authzService).checkPermission(1L, "file:delete");
        verify(fileNodeMapper).restoreDeleted(1L, List.of(10L, 11L));
    }

    @Test
    void restore_shouldRejectWhenSiblingConflictExists() {
        FileNode root = node(10L, 5L, false, 1L, true, "report.pdf", "objects/report.pdf");
        FileNode parent = node(5L, 0L, true, 1L, false, "parent", null);

        when(fileNodeMapper.selectByIdIncludingDeleted(10L)).thenReturn(root);
        when(fileNodeMapper.selectByIdIncludingDeleted(5L)).thenReturn(parent);
        when(fileNodeMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class, () -> recycleService.restore(10L, 1L));

        assertTrue(exception.getMessage().contains("同名"));
        verify(fileNodeMapper, never()).restoreDeleted(any(), any());
    }

    @Test
    void purge_shouldDeletePhysicalFilesAndHardDeleteMetadata() {
        FileNode root = node(10L, 0L, true, 1L, true, "root", null);
        FileNode childFile = node(11L, 10L, false, 1L, true, "a.txt", "objects/a.txt");
        FileNode childDir = node(12L, 10L, true, 1L, true, "dir", null);
        FileNode grandchildFile = node(13L, 12L, false, 1L, true, "b.txt", "objects/b.txt");

        when(fileNodeMapper.selectByIdIncludingDeleted(10L)).thenReturn(root);
        when(fileNodeMapper.selectByParentIdsIncludingDeleted(argThat(idsMatch(10L)))).thenReturn(List.of(childFile, childDir));
        when(fileNodeMapper.selectByParentIdsIncludingDeleted(argThat(idsMatch(11L, 12L)))).thenReturn(List.of(grandchildFile));
        when(fileNodeMapper.selectByParentIdsIncludingDeleted(argThat(idsMatch(13L)))).thenReturn(List.of());
        when(storageRouter.route(any(FileNode.class))).thenReturn(storageBackend);

        recycleService.purge(10L, 1L);

        InOrder inOrder = inOrder(storageBackend, fileNodeMapper);
        inOrder.verify(storageBackend).delete("objects/a.txt");
        inOrder.verify(storageBackend).delete("objects/b.txt");
        verify(fileNodeMapper).hardDeleteByIds(List.of(10L, 11L, 12L, 13L));
    }

    @Test
    void purge_shouldWrapPhysicalDeletionFailure() {
        FileNode root = node(10L, 0L, false, 1L, true, "a.txt", "objects/a.txt");

        when(fileNodeMapper.selectByIdIncludingDeleted(10L)).thenReturn(root);
        when(fileNodeMapper.selectByParentIdsIncludingDeleted(argThat(idsMatch(10L)))).thenReturn(List.of());
        when(storageRouter.route(root)).thenReturn(storageBackend);
        doThrow(new IllegalStateException("boom")).when(storageBackend).delete("objects/a.txt");

        BusinessException exception = assertThrows(BusinessException.class, () -> recycleService.purge(10L, 1L));

        assertEquals("彻底删除失败，物理文件清理异常", exception.getMessage());
        verify(fileNodeMapper, never()).hardDeleteByIds(any());
    }

    private static FileNode node(Long id,
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
        node.setSize(1L);
        node.setCreateTime(LocalDateTime.now());
        node.setDeleteTime(isDeleted ? LocalDateTime.now() : null);
        return node;
    }

    private static ArgumentMatcher<Collection<Long>> idsMatch(Long... expectedIds) {
        List<Long> expected = List.of(expectedIds);
        return actual -> actual != null && actual.size() == expected.size() && actual.containsAll(expected);
    }

    private static ArgumentMatcher<Collection<Long>> idsContainExactly(Long... expectedIds) {
        return idsMatch(expectedIds);
    }
}
