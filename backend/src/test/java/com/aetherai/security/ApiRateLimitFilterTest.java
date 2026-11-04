package com.aetherai.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiRateLimitFilterTest {

	@Test
	void limitsMatchingPostRequestsAndReturnsTheApiErrorShape() throws Exception {
		ApiRateLimitFilter filter = new ApiRateLimitFilter(
				new ObjectMapper(),
				true,
				Duration.ofMinutes(1),
				1,
				5,
				5,
				30);

		MockHttpServletResponse firstResponse = new MockHttpServletResponse();
		filter.doFilter(request(), firstResponse, new MockFilterChain());
		assertEquals(200, firstResponse.getStatus());

		MockHttpServletResponse limitedResponse = new MockHttpServletResponse();
		filter.doFilter(request(), limitedResponse, new MockFilterChain());

		assertEquals(429, limitedResponse.getStatus());
		assertTrue(limitedResponse.getContentAsString().contains("\"status\":429"));
		assertTrue(limitedResponse.getContentAsString().contains("\"path\":\"/api/v1/auth/login\""));
	}

	@Test
	void appliesChatLimitToStreamingEndpoint() throws Exception {
		ApiRateLimitFilter filter = new ApiRateLimitFilter(
				new ObjectMapper(),
				true,
				Duration.ofMinutes(1),
				10,
				5,
				5,
				1);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setRequestURI("/api/v1/conversations/123/messages/stream");
		request.setRemoteAddr("192.0.2.11");

		filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
		MockHttpServletResponse limitedResponse = new MockHttpServletResponse();
		filter.doFilter(request, limitedResponse, new MockFilterChain());

		assertEquals(429, limitedResponse.getStatus());
		assertTrue(limitedResponse.getContentAsString().contains("\"status\":429"));
	}

	private MockHttpServletRequest request() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setRequestURI("/api/v1/auth/login");
		request.setRemoteAddr("192.0.2.10");
		return request;
	}
}
