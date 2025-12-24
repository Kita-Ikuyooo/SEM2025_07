package com.g07.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.g07.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
    
    /**
     * 【新增】查询所有文件夹，并关联租户表获取租户名称
     * 这里的 c.* 会映射到 Category 的基本字段
     * t.tenant_name 会映射到 Category 的 tenantName 字段 (因为它是同名的驼峰)
     */
    @Select("SELECT c.*, t.tenant_name " +
            "FROM category c " +
            "LEFT JOIN tenant t ON c.tenant_id = t.tenant_id " +
            "ORDER BY c.sort_order ASC, c.create_time DESC")
    List<Category> selectAllWithTenantName();
}