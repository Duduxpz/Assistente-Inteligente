package com.aetherai.auth.service;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.aetherai.user.repository.UserRepository;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public DatabaseUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		return userRepository.findByEmail(normalizeEmail(email))
				.map(user -> User.withUsername(user.getEmail())
						.password(user.getPasswordHash())
						.disabled(!user.isEnabled())
						.authorities(user.getRoles().stream()
								.map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
								.toList())
						.build())
				.orElseThrow(() -> new UsernameNotFoundException("User not found."));
	}

	static String normalizeEmail(String email) {
		return email.strip().toLowerCase(java.util.Locale.ROOT);
	}
}
