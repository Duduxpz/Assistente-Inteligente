package com.aetherai.billing;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WebhookSignatureVerifier {

	private static final Pattern SIGNATURE_PART = Pattern.compile("(?:^|,)\\s*(ts|v1)=([^,]+)");

	public boolean isValid(String signatureHeader, String requestId, String dataId, String secret) {
		if (signatureHeader == null || requestId == null || dataId == null || secret == null || secret.isBlank()) {
			return false;
		}

		String timestamp = null;
		String receivedSignature = null;
		Matcher matcher = SIGNATURE_PART.matcher(signatureHeader);
		while (matcher.find()) {
			if ("ts".equals(matcher.group(1))) {
				timestamp = matcher.group(2).trim();
			} else if ("v1".equals(matcher.group(1))) {
				receivedSignature = matcher.group(2).trim();
			}
		}
		if (timestamp == null || receivedSignature == null || !receivedSignature.matches("[0-9a-fA-F]{64}")) {
			return false;
		}

		String manifest = "id:" + dataId.toLowerCase(Locale.ROOT) + ";request-id:" + requestId + ";ts:" + timestamp + ";";
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			byte[] expected = mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));
			byte[] actual = HexFormat.of().parseHex(receivedSignature);
			return MessageDigest.isEqual(expected, actual);
		} catch (java.security.GeneralSecurityException exception) {
			throw new IllegalStateException("HMAC-SHA256 is unavailable.", exception);
		}
	}
}
