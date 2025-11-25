package com.doc.service;


import java.util.List;
import java.util.Map;

public interface MilestoneStatusService {
    List<Map<String, Object>> getAllMilestoneStatuses();
    Map<String, Object> getMilestoneStatusById(Long id);
    Map<String, Object> getMilestoneStatusByName(String name);
}