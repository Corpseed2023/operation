//package com.doc.entity.project.activity;
//
//import com.doc.entity.project.Project;
//import com.doc.entity.project.ProjectActivity;
//import jakarta.persistence.*;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "project_call_log")
//@Data
//@NoArgsConstructor
//public class ProjectCallLog {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @OneToOne(fetch = FetchType.LAZY, optional = false)
//    @JoinColumn(name = "activity_id", nullable = false, unique = true)
//    private ProjectActivity activity;
//
//    @ManyToOne(fetch = FetchType.LAZY, optional = false)
//    @JoinColumn(name = "project_id", nullable = false)
//    private Project project;
//
//    @Column(name = "contact_name")
//    private String contactName;
//
//    @Column(name = "phone_number")
//    private String phoneNumber;
//
//    @Column(name = "call_direction", length = 20)
//    private String callDirection; // INCOMING / OUTGOING
//
//    @Column(name = "call_status", length = 20)
//    private String callStatus; // ANSWERED / MISSED / REJECTED
//
//    @Column(name = "duration_seconds")
//    private Integer durationSeconds;
//
//    @Column(name = "notes", columnDefinition = "TEXT")
//    private String notes;
//
//    @Column(name = "call_time", nullable = false)
//    private LocalDateTime callTime;
//
//
//}
