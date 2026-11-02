package com.aetherai.admin.service;

import com.aetherai.admin.dto.AdminUserResponse;
import com.aetherai.common.exception.ApiException;
import com.aetherai.user.entity.UserEntity;
import com.aetherai.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AdminService {

	private final UserRepository userRepository;

	public AdminService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public Page<AdminUserResponse> listUsers(Pageable pageable) {
		return userRepository.findAll(pageable).map(AdminUserResponse::from);
	}

	@Transactional
	public AdminUserResponse setUserEnabled(UUID userId, boolean enabled) {
		UserEntity user = userRepository.findById(userId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
		user.setEnabled(enabled);
		return AdminUserResponse.from(userRepository.save(user));
	}
}
