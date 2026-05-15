package com.payment.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String TOPIC_PAYMENT_INITIATED  = "payment.initiated";
    public static final String TOPIC_PAYMENT_PROCESSED  = "payment.processed";
    public static final String TOPIC_PAYMENT_FAILED     = "payment.failed";

    @Bean
    public NewTopic paymentInitiatedTopic() {
        return TopicBuilder.name(TOPIC_PAYMENT_INITIATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentProcessedTopic() {
        return TopicBuilder.name(TOPIC_PAYMENT_PROCESSED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentFailedTopic() {
        return TopicBuilder.name(TOPIC_PAYMENT_FAILED)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
