package com.example.bigdata.dto;

import lombok.Data;

import java.util.List;

/**
 * 通用分页结果包装
 *
 * 统一返回格式，前端不用关心是哪种分页方式
 */
@Data
public class PageResult<T> {

    /** 当前页数据 */
    private List<T> list;

    /** 总条数（传统分页有，游标分页可能为空） */
    private Long total;

    /** 当前页码（传统分页） */
    private Integer pageNum;

    /** 每页条数 */
    private Integer pageSize;

    /** 总页数（传统分页） */
    private Integer totalPages;

    /** 游标值（游标分页返回，下一页用） */
    private Long nextCursorId;

    /** 是否有下一页（游标分页） */
    private Boolean hasNext;

    /** 查询耗时（毫秒），由 Service 层填充 */
    private Long queryTimeMs;

    /** 是否命中缓存 */
    private Boolean cacheHit;

    /** 命中的索引名称 */
    private String indexHit;

    /** 扫描行数（如果能获取的话） */
    private Long scanRows;

    /** 分页方式说明 */
    private String pageTypeDesc;

    /**
     * 传统分页的便捷构造
     */
    public static <T> PageResult<T> ofOffset(List<T> list, long total, int pageNum, int pageSize) {
        PageResult<T> result = new PageResult<>();
        result.setList(list);
        result.setTotal(total);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setTotalPages((int) Math.ceil((double) total / pageSize));
        return result;
    }

    /**
     * 游标分页的便捷构造
     */
    public static <T> PageResult<T> ofCursor(List<T> list, int pageSize, Long lastId) {
        PageResult<T> result = new PageResult<>();
        result.setList(list);
        result.setPageSize(pageSize);
        // 如果返回的数据量等于 pageSize，说明可能还有下一页
        boolean hasNext = list.size() == pageSize;
        result.setHasNext(hasNext);
        result.setNextCursorId(hasNext ? lastId : null);
        return result;
    }
}
