package com.aetherai.user.entity;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserEntity {

	@Id
	private UUID id;

	@Column(nullable = false, unique = true, length = 320)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Column(name = "display_name", nullable = false, length = 120)
	private String displayName;

	@Column(nullable = false)
	private boolean enabled;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(
			name = "user_roles",
			joinColumns = @JoinColumn(name = "user_id"),
			inverseJoinColumns = @JoinColumn(name = "role_id"))
	private Set<RoleEntity> roles = new HashSet<>();

	protected UserEntity() {
	}

	public static UserEntity create(String email, String passwordHash, String displayName, RoleEntity role) {
		UserEntity user = new UserEntity();
		user.id = UUID.randomUUID();
		user.email = email;
		user.passwordHash = passwordHash;
		user.displayName = displayName;
		user.enabled = true;
		user.createdAt = OffsetDateTime.now();
		user.updatedAt = user.createdAt;
		user.roles.add(role);
		return user;
	}

	public UUID getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getDisplayName() {
		return displayName;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public Set<RoleEntity> getRoles() {
		return Set.copyOf(roles);
	}
}
