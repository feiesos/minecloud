package org.feiesos.storage.recycle.service;

import org.feiesos.storage.recycle.dto.RecycleItemDTO;

import java.util.List;

public interface RecycleService {

    void moveToRecycleBin(Long fileId, Long userId);

    List<RecycleItemDTO> listRecycleItems(Long userId);

    void restore(Long fileId, Long userId);

    void purge(Long fileId, Long userId);

    void purgeExpired(int retentionDays);
}
