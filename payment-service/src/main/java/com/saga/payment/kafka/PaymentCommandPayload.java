package com.saga.payment.kafka;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentCommandPayload(UUID sagaId, UUID orderId, String commandType, BigDecimal totalAmount){}
