package com.ideafly.utils;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ideafly.dto.PageBaseInputDto;

import java.util.List;
import java.util.Objects;

public class PageUtil {
    public static <T> Page<T> build(int pageNum, int pageSize, Long totalCount, List<T> items) {
        Page<T> p = new Page<>(pageNum, pageSize, totalCount);
        p.setRecords(items);
        return p;
    }

    public static <T> Page<T> build(Page<?> page, List<T> items) {
        Page<T> p = new Page<>(page.getPages(), page.getSize(), page.getTotal());
        p.setCurrent(page.getCurrent());
        p.setRecords(items);
        return p;
    }

    public static <T> Page<T> empty(Page<?> page) {
        return new Page<>(page.getPages(), page.getSize(), page.getTotal());
    }

    public static <T> Page<T> empty(int currPage, int pageSize) {
        return new Page<T>(currPage, pageSize, 0);
    }

    public static <T> Page<T> build(PageBaseInputDto req) {
        Page<T> page = new Page<>(req.getPageNum(), req.getPageSize());
        String orderColumn = req.getOrderColumn();
        if (StrUtil.isBlank(orderColumn)) {
            orderColumn = "id";
        }
        if (req.getAsc()) {
            page.addOrder(OrderItem.asc(orderColumn));
            return page;
        }
        page.addOrder(OrderItem.desc(orderColumn));
        return page;
    }
}
