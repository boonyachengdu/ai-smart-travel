package com.boonya.business.trip.common.vo;

import lombok.Data;

import java.util.List;

@Data
public class PageVO<T> {
    private List<T> records;
    private long total;

    public static <T> PageVO<T> build(List<T> records, long total) {
        PageVO<T> vo = new PageVO<>();
        vo.setRecords(records);
        vo.setTotal(total);
        return vo;
    }
}
