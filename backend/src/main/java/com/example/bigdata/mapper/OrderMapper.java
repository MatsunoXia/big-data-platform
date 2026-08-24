package com.example.bigdata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bigdata.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 查询表总行数（不走MyBatis Plus的count，直接SQL更快）
     */
    @Select("SELECT COUNT(*) FROM t_order")
    long selectTotalCount();

    /**
     * 流式查询 - 按ID范围分段查询（用于导出）
     */
    @Select("SELECT * FROM t_order WHERE id >= #{startId} AND id < #{endId} ORDER BY id")
    List<Order> selectByIdRange(@Param("startId") Long startId, @Param("endId") Long endId);

    /**
     * 获取表中最大ID
     */
    @Select("SELECT MAX(id) FROM t_order")
    Long selectMaxId();

    /**
     * 获取表中最小ID
     */
    @Select("SELECT MIN(id) FROM t_order")
    Long selectMinId();

    /**
     * 传统 OFFSET 分页查询（定义在 XML 中）
     */
    List<Order> selectByOffset(@Param("orderNo") String orderNo,
                               @Param("status") String status,
                               @Param("category") String category,
                               @Param("userId") Long userId,
                               @Param("province") String province,
                               @Param("offset") int offset,
                               @Param("pageSize") int pageSize);

    /**
     * 按条件统计总数（定义在 XML 中）
     */
    long selectCountByCondition(@Param("orderNo") String orderNo,
                                @Param("status") String status,
                                @Param("category") String category,
                                @Param("userId") Long userId,
                                @Param("province") String province);

    /**
     * 游标分页查询（定义在 XML 中）
     */
    List<Order> selectByCursor(@Param("orderNo") String orderNo,
                               @Param("status") String status,
                               @Param("category") String category,
                               @Param("userId") Long userId,
                               @Param("province") String province,
                               @Param("cursorId") Long cursorId,
                               @Param("pageSize") int pageSize);
}
