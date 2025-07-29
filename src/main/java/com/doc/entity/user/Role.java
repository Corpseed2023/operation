package com.doc.entity.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Represents a system-level role (e.g., ADMIN, USER, MANAGER)
 */
@Entity
@Getter
@Setter
public class Role {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "rid")
	private Long id;

	@Column(name = "name", nullable = false, unique = true)
	private String name;


//    @ManyToMany(mappedBy = "userRole", fetch = FetchType.LAZY)
//    private List<User> users;
//

}
