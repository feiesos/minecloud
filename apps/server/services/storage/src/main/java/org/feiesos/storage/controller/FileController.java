package org.feiesos.storage.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.feiesos.common.exception.BusinessException;
import org.feiesos.common.result.R;
import org.feiesos.storage.backend.StorageFacade;
import org.feiesos.storage.dto.FileItemResponse;
import org.feiesos.storage.dto.StorageObject;
import org.feiesos.storage.recycle.service.RecycleService;
import org.feiesos.storage.entity.FileNode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final StorageFacade storageFacade;
    private final RecycleService recycleService;

    public FileController(StorageFacade storageFacade, RecycleService recycleService) {
        this.storageFacade = storageFacade;
        this.recycleService = recycleService;
    }

    @GetMapping("/list")
    public R<List<FileItemResponse>> list(@RequestParam(defaultValue = "0") Long parentId,
                                           HttpServletRequest request) {
        Long userId = getUserId(request);
        List<FileNode> nodes = storageFacade.listByParent(parentId, userId);
        return R.ok(nodes.stream().map(FileItemResponse::from).toList());
    }

    @GetMapping("/browse")
    public R<List<FileItemResponse>> browse(@RequestParam(defaultValue = "/") String path,
                                             HttpServletRequest request) {
        try {
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            Long userId = getUserId(request);
            List<FileNode> nodes = storageFacade.browse(path, userId);
            return R.ok(nodes.stream().map(FileItemResponse::from).toList());
        } catch (BusinessException e) {
            return R.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("浏览目录失败: {}", e.getMessage(), e);
            return R.fail("浏览目录失败: " + e.getMessage());
        }
    }

    @PostMapping("/upload")
    public R<FileItemResponse> upload(@RequestParam("file") MultipartFile file,
                                       @RequestParam(defaultValue = "/") String path,
                                       HttpServletRequest request) {
        if (file.isEmpty()) {
            return R.fail("上传失败：文件内容不能为空");
        }
        try {
            Long userId = getUserId(request);
            Long parentId = storageFacade.resolvePathToId(path, userId);
            log.info("接收上传文件: {}, 大小: {} bytes", file.getOriginalFilename(), file.getSize());
            FileNode node = storageFacade.upload(
                    file.getOriginalFilename(), parentId, userId,
                    file.getInputStream(), file.getSize());
            return R.ok(FileItemResponse.from(node));
        } catch (BusinessException e) {
            return R.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("文件上传异常: ", e);
            return R.fail("系统繁忙: " + e.getMessage());
        }
    }

    @PostMapping("/chunk")
    public R<String> uploadChunk(@RequestParam("file") MultipartFile file,
                                   @RequestParam("md5") String md5,
                                   @RequestParam("index") Integer index,
                                   HttpServletRequest request) {
        try {
            Long userId = getUserId(request);
            storageFacade.uploadChunk(md5, index, userId, file.getInputStream(), file.getSize());
            return R.ok("分片 " + index + " 上传成功");
        } catch (BusinessException e) {
            return R.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("分片上传异常: ", e);
            return R.fail("分片上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/merge")
    public R<FileItemResponse> merge(@RequestParam("md5") String md5,
                                      @RequestParam("fileName") String fileName,
                                      @RequestParam(defaultValue = "/") String path,
                                      HttpServletRequest request) {
        try {
            Long userId = getUserId(request);
            Long parentId = storageFacade.resolvePathToId(path, userId);
            FileNode node = storageFacade.mergeChunks(md5, fileName, parentId, userId);
            return R.ok(FileItemResponse.from(node));
        } catch (BusinessException e) {
            return R.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("合并分片异常: ", e);
            return R.fail("合并失败: " + e.getMessage());
        }
    }

    @PostMapping("/mkdir")
    public R<FileItemResponse> createDirectory(@RequestParam String name,
                                                @RequestParam(defaultValue = "/") String path,
                                                HttpServletRequest request) {
        if (name == null || name.trim().isEmpty()) {
            return R.fail("文件夹名称不能为空");
        }
        try {
            Long userId = getUserId(request);
            Long parentId = storageFacade.resolvePathToId(path, userId);
            FileNode node = storageFacade.createDirectory(name, parentId, userId);
            return R.ok(FileItemResponse.from(node));
        } catch (BusinessException e) {
            return R.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("新建文件夹失败", e);
            return R.fail("新建文件夹失败：" + e.getMessage());
        }
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<StreamingResponseBody> download(@PathVariable Long id,
                                                           HttpServletRequest request) {
        try {
            Long userId = getUserId(request);
            StorageObject storageObj = storageFacade.download(id, userId);

            StreamingResponseBody body = outputStream -> {
                try (storageObj) {
                    storageObj.getInputStream().transferTo(outputStream);
                }
            };

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" +
                                    URLEncoder.encode(storageObj.getFilename(), StandardCharsets.UTF_8) + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(storageObj.getSize())
                    .body(body);
        } catch (BusinessException e) {
            if (e.getCode() == 403) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("下载文件失败, id: {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}")
    public R<FileNode> detail(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = getUserId(request);
            return R.ok(storageFacade.getFileDetail(id, userId));
        } catch (BusinessException e) {
            return R.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("获取文件详情失败, id: {}", id, e);
            return R.fail("文件不存在");
        }
    }

    @PutMapping("/{id}/rename")
    public R<FileNode> rename(@PathVariable Long id,
                               @RequestBody Map<String, String> body,
                               HttpServletRequest request) {
        try {
            String newName = body.get("name");
            if (newName == null || newName.trim().isEmpty()) {
                return R.fail("名称不能为空");
            }
            Long userId = getUserId(request);
            return R.ok(storageFacade.rename(id, newName.trim(), userId));
        } catch (BusinessException e) {
            return R.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("重命名失败, id: {}", id, e);
            return R.fail("重命名失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/move")
    public R<FileNode> move(@PathVariable Long id,
                             @RequestBody Map<String, Long> body,
                             HttpServletRequest request) {
        try {
            Long targetParentId = body.get("targetParentId");
            if (targetParentId == null) {
                return R.fail("目标目录不能为空");
            }
            Long userId = getUserId(request);
            return R.ok(storageFacade.move(id, targetParentId, userId));
        } catch (BusinessException e) {
            return R.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("移动文件失败, id: {}", id, e);
            return R.fail("移动失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/copy")
    public R<FileNode> copy(@PathVariable Long id,
                             @RequestBody Map<String, Long> body,
                             HttpServletRequest request) {
        try {
            Long targetParentId = body.get("targetParentId");
            if (targetParentId == null) {
                return R.fail("目标目录不能为空");
            }
            Long userId = getUserId(request);
            return R.ok(storageFacade.copy(id, targetParentId, userId));
        } catch (BusinessException e) {
            return R.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("复制文件失败, id: {}", id, e);
            return R.fail("复制失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<StreamingResponseBody> preview(@PathVariable Long id,
                                                          HttpServletRequest request) {
        try {
            Long userId = getUserId(request);
            StorageObject storageObj = storageFacade.download(id, userId);

            String mimeType = URLConnection.guessContentTypeFromName(storageObj.getFilename());
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }

            StreamingResponseBody body = outputStream -> {
                try (storageObj) {
                    storageObj.getInputStream().transferTo(outputStream);
                }
            };

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .contentType(MediaType.parseMediaType(mimeType))
                    .contentLength(storageObj.getSize())
                    .body(body);
        } catch (BusinessException e) {
            if (e.getCode() == 403) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("预览文件失败, id: {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = getUserId(request);
            recycleService.moveToRecycleBin(id, userId);
            return R.ok("已移入回收站");
        } catch (BusinessException e) {
            return R.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("删除文件异常, id: {}", id, e);
            return R.fail("删除失败: " + e.getMessage());
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
