package com.doc.repository.department;

import com.doc.entity.department.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    Optional<Team> findByIdAndIsDeletedFalse(Long id);

    boolean existsByNameAndDepartmentIdAndIsDeletedFalse(String name, Long departmentId);

    List<Team> findByDepartmentIdAndIsDeletedFalse(Long departmentId);

    // --- Group / sub-team support ---

    Optional<Team> findByIdAndIsGroupTrueAndIsDeletedFalse(Long id);

    List<Team> findByParentTeamIdAndIsDeletedFalseOrderBySequenceAsc(Long parentTeamId);

    boolean existsByParentTeamIdAndSequenceAndIsDeletedFalse(Long parentTeamId, Integer sequence);

    // Excludes the team's own row — needed so updating a sub-team that already
    // owns a given (parentTeamId, sequence) doesn't flag itself as a duplicate
    boolean existsByParentTeamIdAndSequenceAndIdNotAndIsDeletedFalse(Long parentTeamId, Integer sequence, Long excludeId);
}