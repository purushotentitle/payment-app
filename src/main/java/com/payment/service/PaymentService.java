package com.payment.service;

import com.payment.config.PaymentMetrics;
import com.payment.config.KafkaTopicConfig;
import com.payment.kafka.PaymentProducer;
import com.payment.model.Payment;
import com.payment.model.PaymentDTO;
import com.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentProducer paymentProducer;
    private final PaymentMetrics paymentMetrics;

    // ─────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────
    @Transactional
    public PaymentDTO.Response createPayment(PaymentDTO.Request request) {
        log.info("Creating payment: from={} to={} amount={} {} standard={}",
                request.getFromAccount(), request.getToAccount(),
                request.getAmount(), request.getCurrency(), request.getStandard());

        Payment payment = Payment.builder()
                .fromAccount(request.getFromAccount())
                .toAccount(request.getToAccount())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .standard(request.getStandard())
                .description(request.getDescription())
                .referenceId(generateReferenceId(request.getStandard()))
                .status(Payment.PaymentStatus.PENDING)
                .build();

        payment = paymentRepository.save(payment);
        paymentMetrics.recordPaymentCreated(request.getStandard());
        PaymentDTO.Response response = toResponse(payment, "Payment created successfully");

        // Publish to Kafka
        paymentProducer.sendPaymentInitiated(KafkaTopicConfig.TOPIC_PAYMENT_INITIATED, response);

        return response;
    }

    // ─────────────────────────────────────────────
    // PROCESS (simulate ISO routing)
    // ─────────────────────────────────────────────
    @Transactional
    public PaymentDTO.Response processPayment(String paymentId) {
        Payment payment = findOrThrow(paymentId);

        if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
            throw new IllegalStateException("Payment is not in PENDING state: " + payment.getStatus());
        }

        payment.setStatus(Payment.PaymentStatus.PROCESSING);
        payment = paymentRepository.save(payment);

        // Simulate ISO 8583 vs ISO 20022 processing
        Payment finalPayment = payment;
        String processingNote = paymentMetrics.getProcessingTimer().record(() -> {
            if (finalPayment.getStandard() == Payment.MessageStandard.ISO_8583) {
                return processIso8583(finalPayment);
            } else {
                return processIso20022(finalPayment);
            }
        });

        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setProcessedAt(LocalDateTime.now());
        payment = paymentRepository.save(payment);
        paymentMetrics.recordPaymentCompleted();
        PaymentDTO.Response response = toResponse(payment, processingNote);
        paymentProducer.sendPaymentInitiated(KafkaTopicConfig.TOPIC_PAYMENT_PROCESSED, response);

        return response;
    }

    // ─────────────────────────────────────────────
    // GET
    // ─────────────────────────────────────────────
    public PaymentDTO.Response getPayment(String paymentId) {
        return toResponse(findOrThrow(paymentId), null);
    }

    public List<PaymentDTO.Response> getAllPayments() {
        return paymentRepository.findAll()
                .stream()
                .map(p -> toResponse(p, null))
                .collect(Collectors.toList());
    }

    public List<PaymentDTO.Response> getPaymentsByAccount(String account) {
        return paymentRepository.findByFromAccount(account)
                .stream()
                .map(p -> toResponse(p, null))
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // REVERSE
    // ─────────────────────────────────────────────
    @Transactional
    public PaymentDTO.Response reversePayment(String paymentId) {
        Payment payment = findOrThrow(paymentId);

        if (payment.getStatus() != Payment.PaymentStatus.COMPLETED) {
            throw new IllegalStateException("Only COMPLETED payments can be reversed");
        }

        payment.setStatus(Payment.PaymentStatus.REVERSED);
        payment = paymentRepository.save(payment);
        paymentMetrics.recordPaymentReversed();
        PaymentDTO.Response response = toResponse(payment, "Payment reversed");
        paymentProducer.sendPaymentInitiated(KafkaTopicConfig.TOPIC_PAYMENT_FAILED, response);

        return response;
    }

    // ─────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────

    /**
     * ISO 8583: card / POS transaction processing
     * Simulates authorization, bitmap field parsing, etc.
     */
    private String processIso8583(Payment payment) {
        log.info("[ISO-8583] Processing card transaction: ref={}", payment.getReferenceId());
        // Real impl: build ISO 8583 message, send to card network, parse response
        return "Processed via ISO 8583 (card network). Auth code: " + authCode();
    }

    /**
     * ISO 20022: SEPA / modern bank transfer processing
     * Simulates pacs.008 credit transfer message
     */
    private String processIso20022(Payment payment) {
        log.info("[ISO-20022] Processing SEPA transfer: ref={}", payment.getReferenceId());
        // Real impl: build pacs.008 XML, send to payment gateway, parse response
        return "Processed via ISO 20022 pacs.008 (SEPA transfer). UETR: " + UUID.randomUUID();
    }

    private String generateReferenceId(Payment.MessageStandard standard) {
        String prefix = (standard == Payment.MessageStandard.ISO_8583) ? "POS" : "SEPA";
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String authCode() {
        return String.format("%06d", (int)(Math.random() * 999999));
    }

    private Payment findOrThrow(String id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + id));
    }

    private PaymentDTO.Response toResponse(Payment p, String message) {
        return PaymentDTO.Response.builder()
                .id(p.getId())
                .fromAccount(p.getFromAccount())
                .toAccount(p.getToAccount())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .standard(p.getStandard())
                .status(p.getStatus())
                .description(p.getDescription())
                .referenceId(p.getReferenceId())
                .createdAt(p.getCreatedAt())
                .processedAt(p.getProcessedAt())
                .message(message)
                .build();
    }
}
