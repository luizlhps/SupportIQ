package com.worklyze.supportiq.shared.page.interfaces;

import com.worklyze.worklyze.shared.page.PageListImpl;

public interface QueryParams {
    PageListImpl getPagination();
    String getSort(); // Ex: "field1:asc,field2:desc"
}
