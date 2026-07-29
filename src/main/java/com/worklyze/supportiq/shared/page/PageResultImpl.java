package com.worklyze.supportiq.shared.page;

import com.worklyze.worklyze.shared.page.interfaces.PageResult;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PageResultImpl<T> implements PageResult<T>{
    public List<T> items;
    public Integer pageNumber;
    public Integer pageSize;
    public Integer totalCount;
    public Integer totalPages;
}