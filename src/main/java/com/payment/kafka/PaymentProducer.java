package com.payment.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.model.PaymentDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void sendPaymentInitiated(String topic, PaymentDTO.Response payment) {
        try {
            String payload = objectMapper.writeValueAsString(payment);
            kafkaTemplate.send(topic, payment.getId(), payload);
            log.info("Published to [{}] paymentId={}", topic, payment.getId());
        } catch (Exception e) {
            log.error("Failed to publish payment event: {}", e.getMessage());
        }
    }
}
