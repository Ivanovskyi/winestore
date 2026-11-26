package dev.winestore.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Profile("!stripe")
public class FakePaymentService implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(FakePaymentService.class);

    @Override
    public PaymentResult charge(long orderId, BigDecimal amount) {
        log.info("[FAKE PAYMENT] order {} amount {} paid", orderId, amount);
        return new PaymentResult(true, "Purchase successful");
    }
}
