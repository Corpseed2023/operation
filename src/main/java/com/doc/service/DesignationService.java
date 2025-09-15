package com.doc.service;

import com.doc.dto.desigantion.DesignationRequestDto;
import com.doc.dto.desigantion.DesignationResponseDto;

import java.util.List;

public interface DesignationService {

    DesignationResponseDto createDesignation(DesignationRequestDto requestDto);

    DesignationResponseDto getDesignationById(Long id);

    List<DesignationResponseDto> getAllDesignations(int page, int size);

    DesignationResponseDto updateDesignation(Long id, DesignationRequestDto requestDto);

    void deleteDesignation(Long id);

    DesignationResponseDto createMasterDesignation(DesignationRequestDto requestDto);
}