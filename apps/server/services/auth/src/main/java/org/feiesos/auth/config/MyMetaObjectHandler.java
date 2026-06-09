package org.feiesos.auth.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

/**
 * 数据库设置了默认填充，这里不写也行
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        /*OffsetDateTime now = OffsetDateTime.now();

        this.strictInsertFill(
                metaObject,
                "createdAt",
                OffsetDateTime.class,
                now
        );

        this.strictInsertFill(
                metaObject,
                "updatedAt",
                OffsetDateTime.class,
                now
        );*/
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        /*this.strictUpdateFill(
                metaObject,
                "updatedAt",
                OffsetDateTime.class,
                OffsetDateTime.now()
        );*/
    }

}
