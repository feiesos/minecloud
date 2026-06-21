package org.feiesos.storage.recycle.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.feiesos.common.exception.BusinessException;
import org.feiesos.common.result.R;
import org.feiesos.storage.recycle.dto.RecycleItemDTO;
import org.feiesos.storage.recycle.service.RecycleService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/recycle")
public class RecycleController {

    private final RecycleService recycleService;

    public RecycleController(RecycleService recycleService) {
        this.recycleService = recycleService;
    }

    @GetMapping
    public R<List<RecycleItemDTO>> list(HttpServletRequest request) {
        Long userId = getUserId(request);
        return R.ok(recycleService.listRecycleItems(userId));
    }

    @PostMapping("/{id}/restore")
    public R<String> restore(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = getUserId(request);
            recycleService.restore(id, userId);
            return R.ok("恢复成功");
        } catch (BusinessException e) {
            return R.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("恢复回收站文件失败, id: {}", id, e);
            return R.fail("恢复失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public R<String> purge(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = getUserId(request);
            recycleService.purge(id, userId);
            return R.ok("彻底删除成功");
        } catch (BusinessException e) {
            return R.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("彻底删除回收站文件失败, id: {}", id, e);
            return R.fail("彻底删除失败: " + e.getMessage());
        }
    }

    private Long getUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("currentUserId");
        if (userId == null) {
            throw new BusinessException(401, "未认证");
        }
        return (Long) userId;
    }
}
