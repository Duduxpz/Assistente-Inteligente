package com.aetherai.billing.dto;

import jakarta.validation.constraints.NotBlank;

public record CheckoutRequest(@NotBlank String planKey) {
}
