package org.feiesos.storage.recycle.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.feiesos.common.exception.BusinessException;
import org.feiesos.storage.backend.StorageBackend;
import org.feiesos.storage.backend.StorageRouter;
import org.feiesos.storage.config.StorageProperties;
import org.feiesos.storage.entity.FileNode;
import org.feiesos.storage.mapper.FileNodeMapper;
import org.feiesos.storage.recycle.dto.RecycleItemDTO;
import org.feiesos.storage.recycle.service.RecycleService;
import org.feiesos.storage.service.AuthzService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RecycleServiceImpl implements RecycleService {

    private final AuthzService authzService;
    private final StorageRouter storageRouter;
    private final FileNodeMapper fileNodeMapper;
    private final StorageProperties storageProperties;

    public RecycleServiceImpl(AuthzService authzService,
                              StorageRouter storageRouter,
                              FileNodeMapper fileNodeMapper,
                              StorageProperties storageProperties) {
        this.authzService = authzService;
        this.storageRouter = storageRouter;
        this.fileNodeMapper = fileNodeMapper;
        this.storageProperties = storageProperties;
    }

    @Override
    @Transactional
    public void moveToRecycleBin(Long fileId, Long userId) {
        authzService.checkPermission(userId, "file:delete");

        FileNode root = fileNodeMapper.selectById(fileId);
        if (root == null) {
            throw new BusinessException("文件已不存在或已在回收站中");
        }
        assertOwnership(root, userId);

        List<FileNode> subtree = loadSubtree(root);
        fileNodeMapper.markDeleted(userId, extractIds(subtree), LocalDateTime.now());
    }

    @Override
    public List<RecycleItemDTO> listRecycleItems(Long userId) {
        authzService.checkPermission(userId, "file:read");

        List<FileNode> deletedNodes = fileNodeMapper.selectDeletedByOwnerId(userId);
        Set<Long> deletedIds = deletedNodes.stream()
                .map(FileNode::getId)
                .collect(Collectors.toSet());

        return deletedNodes.stream()
                .filter(node -> isRecycleRoot(node, deletedIds))
                .map(this::toRecycleItem)
                .toList();
    }

    @Override
    @Transactional
    public void restore(Long fileId, Long userId) {
        authzService.checkPermission(userId, "file:delete");

        FileNode root = requireDeletedNode(fileId, userId);
        assertRestorable(root, userId);

        List<FileNode> subtree = loadSubtree(root);
        fileNodeMapper.restoreDeleted(userId, extractIds(subtree));
    }

    @Override
    @Transactional
    public void purge(Long fileId, Long userId) {
        authzService.checkPermission(userId, "file:delete");

        FileNode root = requireDeletedNode(fileId, userId);
        List<FileNode> subtree = loadSubtree(root);

        // Child files are deleted before parent directories so local backends can clean up safely.
        subtree.stream()
                .filter(node -> Boolean.FALSE.equals(node.getIsDir()))
                .filter(node -> node.getStoragePath() != null && !node.getStoragePath().isBlank())
                .sorted(Comparator.comparing(FileNode::getId))
                .forEach(this::deletePhysicalObjectQuietly);

        List<Long> ids = extractIds(subtree);
        if (!ids.isEmpty()) {
            fileNodeMapper.hardDeleteByIds(ids);
        }
    }

    @Override
    @Transactional
    public void purgeExpired(int retentionDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        List<FileNode> expiredRoots = fileNodeMapper.selectExpiredDeletedRoots(cutoff);
        for (FileNode root : expiredRoots) {
            try {
                List<FileNode> subtree = loadSubtree(root);
                subtree.stream()
                        .filter(node -> Boolean.FALSE.equals(node.getIsDir()))
                        .filter(node -> node.getStoragePath() != null && !node.getStoragePath().isBlank())
                        .forEach(this::deletePhysicalObjectQuietly);
                List<Long> ids = extractIds(subtree);
                if (!ids.isEmpty()) {
                    fileNodeMapper.hardDeleteByIds(ids);
                }
            } catch (Exception e) {
                log.error("定时清理回收站节点失败, id: {}", root.getId(), e);
            }
        }
        if (!expiredRoots.isEmpty()) {
            log.info("定时清理完成, 共清理 {} 个回收站根节点", expiredRoots.size());
        }
    }

    @Scheduled(cron = "${minecloud.storage.recycle.cleanup-cron:0 0 3 * * ?}")
    public void scheduledCleanup() {
        int retentionDays = storageProperties.getRecycle().getRetentionDays();
        log.info("开始定时清理回收站 (保留天数: {}): ", retentionDays);
        purgeExpired(retentionDays);
    }

    private RecycleItemDTO toRecycleItem(FileNode node) {
        return RecycleItemDTO.builder()
                .id(node.getId())
                .name(node.getName())
                .parentId(node.getParentId())
                .isDir(node.getIsDir())
                .size(node.getSize())
                .createTime(node.getCreateTime())
                .deleteTime(node.getDeleteTime())
                .build();
    }

    private boolean isRecycleRoot(FileNode node, Set<Long> deletedIds) {
        Long parentId = node.getParentId();
        return parentId == null || parentId == 0L || !deletedIds.contains(parentId);
    }

    private FileNode requireDeletedNode(Long fileId, Long userId) {
        FileNode node = fileNodeMapper.selectByIdIncludingDeleted(fileId);
        if (node == null) {
            throw new BusinessException("文件不存在");
        }
        assertOwnership(node, userId);
        if (!Boolean.TRUE.equals(node.getIsDeleted())) {
            throw new BusinessException("目标文件不在回收站中");
        }
        return node;
    }

    private void assertRestorable(FileNode root, Long userId) {
        Long parentId = root.getParentId();
        if (parentId != null && parentId != 0L) {
            FileNode parent = fileNodeMapper.selectByIdIncludingDeleted(parentId);
            if (parent == null) {
                throw new BusinessException("原始父目录不存在，无法恢复");
            }
            if (!userId.equals(parent.getOwnerId())) {
                throw new BusinessException(403, "无权恢复到该父目录");
            }
            if (Boolean.TRUE.equals(parent.getIsDeleted())) {
                throw new BusinessException("请先恢复父目录，再恢复当前文件");
            }
        }

        Long siblingCount = fileNodeMapper.selectCount(new LambdaQueryWrapper<FileNode>()
                .eq(FileNode::getOwnerId, userId)
                .eq(FileNode::getParentId, root.getParentId())
                .eq(FileNode::getName, root.getName())
                .ne(FileNode::getId, root.getId()));
        if (siblingCount > 0) {
            throw new BusinessException("恢复失败：原目录下已存在同名文件或文件夹");
        }
    }

    private List<FileNode> loadSubtree(FileNode root) {
        Map<Long, FileNode> nodes = new LinkedHashMap<>();
        Deque<Long> pendingParentIds = new ArrayDeque<>();

        nodes.put(root.getId(), root);
        pendingParentIds.add(root.getId());

        while (!pendingParentIds.isEmpty()) {
            List<Long> batchParentIds = new ArrayList<>();
            while (!pendingParentIds.isEmpty()) {
                batchParentIds.add(pendingParentIds.poll());
            }
            List<FileNode> children = fileNodeMapper.selectByParentIdsIncludingDeleted(batchParentIds);
            for (FileNode child : children) {
                if (nodes.putIfAbsent(child.getId(), child) == null) {
                    pendingParentIds.add(child.getId());
                }
            }
        }

        return new ArrayList<>(nodes.values());
    }

    private List<Long> extractIds(List<FileNode> nodes) {
        return nodes.stream()
                .map(FileNode::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
    }

    private void assertOwnership(FileNode node, Long userId) {
        if (!userId.equals(node.getOwnerId())) {
            throw new BusinessException(403, "无权操作该文件");
        }
    }

    private void deletePhysicalObjectQuietly(FileNode node) {
        StorageBackend backend = storageRouter.route(node);
        try {
            backend.delete(node.getStoragePath());
        } catch (RuntimeException ex) {
            log.error("清理物理文件失败, id: {}, storagePath: {}", node.getId(), node.getStoragePath(), ex);
            throw new BusinessException("彻底删除失败，物理文件清理异常");
        }
    }
}
