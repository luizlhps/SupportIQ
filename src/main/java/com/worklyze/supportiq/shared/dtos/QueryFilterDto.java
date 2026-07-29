package com.worklyze.supportiq.shared.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class QueryFilterDto {
    @Min(0)
    private int page = 0;

    @Min(1)
    @Schema(example = "10")
    private int size = 10;

    @Schema(description = "Campo para ordenação", example = "createdAt")
    private String sortBy = "createdAt";

    private boolean ascending = true;
}
