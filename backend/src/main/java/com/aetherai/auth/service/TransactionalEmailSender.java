package com.aetherai.auth.service;

public interface TransactionalEmailSender {

	void sendPasswordReset(String recipient, String resetUrl);
}
