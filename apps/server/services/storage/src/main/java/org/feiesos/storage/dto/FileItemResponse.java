package org.feiesos.storage.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.feiesos.storage.entity.FileNode;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileItemResponse {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String name;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    private Boolean isDir;

    private Long size;

    private String fileHash;

    private LocalDateTime createTime;

    private LocalDateTime deleteTime;

    public static FileItemResponse from(FileNode node) {
        return FileItemResponse.builder()
                .id(node.getId())
                .name(node.getName())
                .parentId(node.getParentId())
                .isDir(node.getIsDir())
                .size(node.getSize())
                .fileHash(node.getFileHash())
                .createTime(node.getCreateTime())
                .deleteTime(node.getDeleteTime())
                .build();
    }
}
