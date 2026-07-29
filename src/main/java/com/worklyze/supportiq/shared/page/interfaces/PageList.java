package com.worklyze.supportiq.shared.page.interfaces;

public interface PageList {
    Integer getPageNumber();

    Integer getPageSize();

    Integer getTotalCount();

    Boolean getAllRows();
}
