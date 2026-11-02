package com.aetherai.security;

import com.aetherai.user.entity.UserEntity;
import com.aetherai.user.repository.UserRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class UserJwtAuthenticationConverter implements Converter<Jwt, AbstractOAuth2TokenAuthenticationToken<Jwt>> {

	private final UserRepository userRepository;

	public UserJwtAuthenticationConverter(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public AbstractOAuth2TokenAuthenticationToken<Jwt> convert(Jwt jwt) {
		UUID userId;
		try {
			userId = UUID.fromString(jwt.getSubject());
		} catch (IllegalArgumentException exception) {
			throw new BadCredentialsException("Invalid access token subject.", exception);
		}

		UserEntity user = userRepository.findById(userId)
				.filter(UserEntity::isEnabled)
				.orElseThrow(() -> new BadCredentialsException("Account is unavailable."));
		Number tokenSessionVersion = jwt.getClaim("sessionVersion");
		if (tokenSessionVersion == null || tokenSessionVersion.intValue() != user.getSessionVersion()) {
			throw new BadCredentialsException("Access token has been revoked.");
		}
		List<GrantedAuthority> authorities = user.getRoles().stream()
				.map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
				.map(GrantedAuthority.class::cast)
				.toList();
		return new JwtAuthenticationToken(jwt, authorities, user.getId().toString());
	}
}
