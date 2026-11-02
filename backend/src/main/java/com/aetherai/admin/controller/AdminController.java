package com.aetherai.admin.controller;

import com.aetherai.admin.dto.AdminUserResponse;
import com.aetherai.admin.dto.UserStatusRequest;
import com.aetherai.admin.service.AdminService;
import com.aetherai.usage.dto.UsageTotals;
import com.aetherai.usage.service.UsageService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminController {

	private final AdminService adminService;
	private final UsageService usageService;

	public AdminController(AdminService adminService, UsageService usageService) {
		this.adminService = adminService;
		this.usageService = usageService;
	}

	@GetMapping("/users")
	public Page<AdminUserResponse> listUsers(
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
		return adminService.listUsers(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
	}

	@GetMapping("/usage")
	public UsageTotals getUsage() {
		return usageService.forAllUsers();
	}

	@PutMapping("/users/{userId}/status")
	public AdminUserResponse setUserEnabled(
			@PathVariable UUID userId,
			@Valid @RequestBody UserStatusRequest request) {
		return adminService.setUserEnabled(userId, request.enabled());
	}
}
