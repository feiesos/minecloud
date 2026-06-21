package org.feiesos.storage.recycle.controller;

import org.feiesos.common.exception.BusinessException;
import org.feiesos.storage.recycle.dto.RecycleItemDTO;
import org.feiesos.storage.recycle.service.RecycleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RecycleControllerTest {

    @Mock
    private RecycleService recycleService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RecycleController(recycleService)).build();
    }

    @Test
    void list_shouldReturnRecycleItems() throws Exception {
        RecycleItemDTO root = RecycleItemDTO.builder()
                .id(10L)
                .name("root")
                .parentId(0L)
                .isDir(true)
                .size(0L)
                .createTime(LocalDateTime.of(2026, 6, 20, 12, 0))
                .deleteTime(LocalDateTime.of(2026, 6, 20, 13, 0))
                .build();

        when(recycleService.listRecycleItems(1L)).thenReturn(List.of(root));

        mockMvc.perform(get("/api/v1/recycle")
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value("10"))
                .andExpect(jsonPath("$.data[0].name").value("root"))
                .andExpect(jsonPath("$.data[0].isDir").value(true));

        verify(recycleService).listRecycleItems(1L);
    }

    @Test
    void restore_shouldReturnSuccessWhenServiceCompletes() throws Exception {
        doNothing().when(recycleService).restore(10L, 1L);

        mockMvc.perform(post("/api/v1/recycle/10/restore")
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("恢复成功"));

        verify(recycleService).restore(10L, 1L);
    }

    @Test
    void restore_shouldMapBusinessExceptionToFailResponse() throws Exception {
        doThrow(new BusinessException(409, "恢复失败：原目录下已存在同名文件或文件夹"))
                .when(recycleService).restore(10L, 1L);

        mockMvc.perform(post("/api/v1/recycle/10/restore")
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.msg").value("恢复失败：原目录下已存在同名文件或文件夹"));
    }

    @Test
    void purge_shouldMapUnexpectedExceptionToGenericFailResponse() throws Exception {
        doThrow(new IllegalStateException("storage backend unavailable"))
                .when(recycleService).purge(10L, 1L);

        mockMvc.perform(delete("/api/v1/recycle/10")
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("彻底删除失败: storage backend unavailable"));
    }
}
