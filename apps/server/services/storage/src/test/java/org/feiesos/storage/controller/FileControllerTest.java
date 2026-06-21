package org.feiesos.storage.controller;

import org.feiesos.common.exception.BusinessException;
import org.feiesos.storage.backend.StorageFacade;
import org.feiesos.storage.recycle.service.RecycleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock
    private StorageFacade storageFacade;

    @Mock
    private RecycleService recycleService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FileController(storageFacade, recycleService)).build();
    }

    @Test
    void delete_shouldMoveFileToRecycleBin() throws Exception {
        doNothing().when(recycleService).moveToRecycleBin(10L, 1L);

        mockMvc.perform(delete("/api/v1/files/10")
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("已移入回收站"));

        verify(recycleService).moveToRecycleBin(10L, 1L);
    }

    @Test
    void delete_shouldMapBusinessExceptionToFailResponse() throws Exception {
        doThrow(new BusinessException(403, "无权操作该文件"))
                .when(recycleService).moveToRecycleBin(10L, 1L);

        mockMvc.perform(delete("/api/v1/files/10")
                        .requestAttr("currentUserId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.msg").value("无权操作该文件"));
    }
}
