package org.feiesos.storage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Update;
import org.feiesos.storage.entity.FileNode;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Mapper
public interface FileNodeMapper extends BaseMapper<FileNode> {

    String BASE_COLUMNS = """
            id, name, parent_id, is_dir, size, file_hash, storage_path, storage_type,
            owner_id, create_time, is_deleted, delete_time
            """;

    @Select("""
            SELECT
            """ + BASE_COLUMNS + """
            FROM file_node
            WHERE id = #{id}
            """)
    FileNode selectByIdIncludingDeleted(@Param("id") Long id);

    @Select("""
            <script>
            SELECT
            """ + BASE_COLUMNS + """
            FROM file_node
            WHERE parent_id IN
            <foreach collection='parentIds' item='parentId' open='(' separator=',' close=')'>
                #{parentId}
            </foreach>
            </script>
            """)
    List<FileNode> selectByParentIdsIncludingDeleted(@Param("parentIds") Collection<Long> parentIds);

    @Select("""
            SELECT
            """ + BASE_COLUMNS + """
            FROM file_node
            WHERE owner_id = #{userId}
              AND is_deleted = true
            ORDER BY delete_time DESC, is_dir DESC, create_time DESC
            """)
    List<FileNode> selectDeletedByOwnerId(@Param("userId") Long userId);

    @Update("""
            <script>
            UPDATE file_node
            SET is_deleted = true,
                delete_time = #{deleteTime}
            WHERE owner_id = #{userId}
              AND id IN
            <foreach collection='ids' item='id' open='(' separator=',' close=')'>
                #{id}
            </foreach>
              AND is_deleted = false
            </script>
            """)
    int markDeleted(@Param("userId") Long userId,
                    @Param("ids") Collection<Long> ids,
                    @Param("deleteTime") LocalDateTime deleteTime);

    @Update("""
            <script>
            UPDATE file_node
            SET is_deleted = false,
                delete_time = null
            WHERE owner_id = #{userId}
              AND id IN
            <foreach collection='ids' item='id' open='(' separator=',' close=')'>
                #{id}
            </foreach>
              AND is_deleted = true
            </script>
            """)
    int restoreDeleted(@Param("userId") Long userId, @Param("ids") Collection<Long> ids);

    @Select("""
            <script>
            SELECT
            """ + BASE_COLUMNS + """
            FROM file_node fn
            WHERE fn.is_deleted = true
              AND fn.delete_time &lt; #{cutoff}
              AND (fn.parent_id IS NULL OR fn.parent_id = 0 OR
                   NOT EXISTS (SELECT 1 FROM file_node p WHERE p.id = fn.parent_id AND p.is_deleted = true))
            ORDER BY fn.delete_time ASC
            </script>
            """)
    List<FileNode> selectExpiredDeletedRoots(@Param("cutoff") LocalDateTime cutoff);

    @Delete("""
            <script>
            DELETE FROM file_node
            WHERE id IN
            <foreach collection='ids' item='id' open='(' separator=',' close=')'>
                #{id}
            </foreach>
            </script>
            """)
    int hardDeleteByIds(@Param("ids") Collection<Long> ids);
}
