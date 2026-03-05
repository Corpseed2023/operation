package com.doc.entity.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.util.Date;
import java.util.List;

/**
 * Represents a system-level role (e.g., ADMIN, USER, MANAGER)
 */
@Entity
@Getter
@Setter
public class Role {

	@Id
	@Comment("Primary key: Unique identifier for the role")
	private Long id;

	@Column(name = "name", nullable = false, unique = true)
	@Comment("Role name, must be unique (e.g., ADMIN, ROLE_CRT_USER)")
	private String name;

	@ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
	@Comment("Users assigned to this role")
	private List<User> users;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "created_date", nullable = false, updatable = false)
	@Comment("Date when the role was created")
	private Date createdDate;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "updated_date", nullable = false)
	@Comment("Date when the role was last updated")
	private Date updatedDate;

	@Column(name = "is_deleted", nullable = false)
	@Comment("Soft delete flag")
	private boolean isDeleted = false;

	@PrePersist
	protected void onCreate() {
		this.createdDate = new Date();
		this.updatedDate = new Date();
	}

	@PreUpdate
	protected void onUpdate() {
		this.updatedDate = new Date();
	}
}