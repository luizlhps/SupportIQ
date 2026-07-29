package com.worklyze.supportiq.shared.page.interfaces;

import java.util.List;

public interface PageResult<T> {
    List<T> getItems();
    Integer getPageNumber();
    Integer getPageSize();
    Integer getTotalCount();
    Integer getTotalPages();
}
