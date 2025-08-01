package com.doc.service;


import com.doc.dto.desigantion.DesignationResponseDto;

import java.util.List;

public interface DesignationService {

    DesignationResponseDto createDesignation(String name, Long weightValue, Long departmentId,
                                             Long createdBy);

    DesignationResponseDto getDesignationById(Long id);

    List<DesignationResponseDto> getAllDesignations(int page, int size);

    DesignationResponseDto updateDesignation(Long id, String name, Long weightValue, Long departmentId);

    void deleteDesignation(Long id);

    DesignationResponseDto createMasterDesignation(String name, Long weightValue, Long departmentId);


}