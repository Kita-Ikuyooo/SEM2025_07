package com.g07.file.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String QUEUE_NAME = "doc.process.queue";
    public static final String EXCHANGE_NAME = "doc.process.exchange";
    public static final String ROUTING_KEY = "doc.upload";

    @Bean
    public Queue docQueue() {
        // 持久化队列
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public DirectExchange docExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Binding binding(Queue queue, DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
    }
}