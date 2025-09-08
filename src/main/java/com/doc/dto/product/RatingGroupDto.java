package com.doc.dto.product;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class RatingGroupDto {
    private Double rating;
    private List<UserProductDto> mappings = new ArrayList<>();
}