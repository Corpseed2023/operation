//package com.doc;
//
//import com.doc.entity.department.Department;
//import com.doc.entity.project.ProjectMilestoneAssignment;
//import com.doc.entity.project.UserPerformanceCount;
//import com.doc.entity.user.User;
//import com.doc.entity.user.UserProductMap;
//import com.doc.repository.UserPerformanceCountRepository;
//import com.doc.repository.UserProductMapRepository;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Component
//public class EligibilityChecker {
//    private static final Logger logger = LoggerFactory.getLogger(EligibilityChecker.class);
//
//    @Autowired
//    private UserProductMapRepository userProductMapRepository;
//    @Autowired
//    private UserPerformanceCountRepository userPerformanceCountRepository;
//
//    public boolean isUserEligible(User newUser, ProjectMilestoneAssignment assignment, List<Long> milestoneDepartmentIds, boolean isHolidayMode) {
//        List<Long> userDepartmentIds = newUser.getDepartments().stream()
//                .map(Department::getId)
//                .collect(Collectors.toList());
//
//        // Check department membership
//        boolean isEligible = userDepartmentIds.stream().anyMatch(milestoneDepartmentIds::contains);
//        if (isEligible) {
//            logger.info("User ID {} (Name={}) is eligible: in department IDs {}",
//                    newUser.getId(), newUser.getFullName(), userDepartmentIds);
//            return true;
//        }
//
//        // Check cross-team eligibility if allowed
//        if (teamConfigService.isCrossTeamAssignmentAllowed(assignment.getProject().getId()) || isHolidayMode) {
//            UserProductMap map = userProductMapRepository.findByUserIdAndProductIdAndIsDeletedFalse(
//                    newUser.getId(), assignment.getProject().getProduct().getId());
//            boolean hasExpertise = map != null && map.getRating() >= 4.0;
//            UserPerformanceCount count = userPerformanceCountRepository.findByUserIdAndProductId(
//                    newUser.getId(), assignment.getProject().getProduct().getId());
//            boolean isIdle = count == null || count.getAssignmentCount() < newUser.getBucketSize() * 0.5;
//            isEligible = newUser.isActive() && hasExpertise && isIdle;
//            if (isEligible) {
//                logger.info("Cross-team eligibility granted for user ID {} (Name={}) for milestone '{}': active={}, rating={}, idle={}",
//                        newUser.getId(), newUser.getFullName(), assignment.getProductMilestoneMap().getMilestone().getName(),
//                        newUser.isActive(), map != null ? map.getRating() : 0.0, isIdle);
//            } else {
//                logger.warn("Cross-team eligibility denied for user ID {} (Name={}). Active={}, Rating={}, Idle={}",
//                        newUser.getId(), newUser.getFullName(), newUser.isActive(),
//                        map != null ? map.getRating() : 0.0, isIdle);
//            }
//        }
//
//        return isEligible;
//    }
//}