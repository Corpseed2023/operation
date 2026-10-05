package com.doc.dto.milestone;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class BulkDeleteProductMilestoneMapResponseDto {

    private int deletedCount;
    private List<Long> deletedIds;
}