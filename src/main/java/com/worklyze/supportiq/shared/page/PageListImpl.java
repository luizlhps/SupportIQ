package com.worklyze.supportiq.shared.page;

import com.worklyze.supportiq.shared.page.interfaces.PageList;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PageListImpl implements PageList {
    private Integer pageNumber = 0;
    private Integer pageSize = 10;
    private Integer totalCount = 0;
    private Boolean allRows = false;

    public Integer getPageNumber() {
        return Math.max(pageNumber, 0);
    }

    public Integer getPageSize() {
        return Math.max(pageSize, 10);
    }

    public int getOffset() {
        return getPageNumber() * getPageSize();
    }
}
