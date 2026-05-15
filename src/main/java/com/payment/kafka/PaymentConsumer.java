package com.payment.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.config.KafkaTopicConfig;
import com.payment.model.PaymentDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopicConfig.TOPIC_PAYMENT_INITIATED,
                   groupId = "payment-group")
    public void onPaymentInitiated(String message) {
        try {
            PaymentDTO.Response payment = objectMapper.readValue(message, PaymentDTO.Response.class);
            log.info("[CONSUMER] Payment initiated: id={} amount={} {}",
                    payment.getId(), payment.getAmount(), payment.getCurrency());
            // Add downstream processing logic here
        } catch (Exception e) {
            log.error("[CONSUMER] Error processing message: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = KafkaTopicConfig.TOPIC_PAYMENT_PROCESSED,
                   groupId = "payment-group")
    public void onPaymentProcessed(String message) {
        log.info("[CONSUMER] Payment processed event received: {}", message);
    }

    @KafkaListener(topics = KafkaTopicConfig.TOPIC_PAYMENT_FAILED,
                   groupId = "payment-group")
    public void onPaymentFailed(String message) {
        log.warn("[CONSUMER] Payment failed event received: {}", message);
    }
}
