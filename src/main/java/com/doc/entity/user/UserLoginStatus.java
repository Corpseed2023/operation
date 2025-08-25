package com.doc.entity.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.util.Date;

@Entity
@Table(name = "user_online_status")
@Getter
@Setter
@NoArgsConstructor
public class UserLoginStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Primary key: User online status ID")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @Comment("Associated user")
    private User user;

    @Column(name = "is_online", nullable = false)
    @Comment("Flag indicating if the user is currently online")
    private boolean isOnline = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_online")
    @Comment("Last time the user was online")
    private Date lastOnline;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_date", updatable = false)
    @Comment("Creation date")
    private Date createdDate = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_date")
    @Comment("Update date")
    private Date updatedDate = new Date();

    @Column(name = "is_deleted", nullable = false)
    @Comment("Soft delete flag")
    private boolean isDeleted = false;

}