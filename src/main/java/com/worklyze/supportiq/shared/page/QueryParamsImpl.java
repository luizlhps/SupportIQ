package com.worklyze.supportiq.shared.page;

import com.worklyze.worklyze.shared.page.interfaces.QueryParams;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class QueryParamsImpl implements QueryParams {
    public PageListImpl pagination = new PageListImpl();
    public String sort ;
}
