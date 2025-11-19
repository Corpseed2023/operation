package com.doc.impl;

import com.doc.entity.project.MilestoneStatus;
import com.doc.repository.MilestoneStatusRepository;
import com.doc.service.MilestoneStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MilestoneStatusServiceImpl implements MilestoneStatusService {

    @Autowired
    private MilestoneStatusRepository milestoneStatusRepository;

    @Override
    public List<Map<String, Object>> getAllMilestoneStatuses() {
        return milestoneStatusRepository.findAll().stream()
                .map(this::toMap)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getMilestoneStatusById(Long id) {
        return milestoneStatusRepository.findById(id)
                .map(this::toMap)
                .orElseThrow(() -> new RuntimeException("Milestone status not found with id: " + id));
    }

    @Override
    public Map<String, Object> getMilestoneStatusByName(String name) {
        return milestoneStatusRepository.findByName(name)
                .map(this::toMap)
                .orElseThrow(() -> new RuntimeException("Milestone status not found with name: " + name));
    }

    private Map<String, Object> toMap(MilestoneStatus status) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", status.getId());
        map.put("name", status.getName());
        map.put("description", status.getDescription());
        return map;
    }
}