package dev.winestore.payment;

import java.math.BigDecimal;

public interface PaymentService {

    PaymentResult charge(long orderId, BigDecimal amount);
}
