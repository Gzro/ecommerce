package fr.guezrou.ecommerce.order.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String ORDER_QUEUE = "order.queue";
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";

    public static final String STOCK_EXCHANGE = "stock.exchange";

    public static final String STOCK_RESERVED_QUEUE = "stock.reserved.order.queue";

    public static final String STOCK_RESERVED_ROUTING_KEY = "stock.reserved";

    public static final String STOCK_RESERVATION_FAILED_QUEUE = "stock.reservation.failed.order.queue";

    public static final String STOCK_RESERVATION_FAILED_ROUTING_KEY = "stock.reservation.failed";

    public static final String PAYMENT_EXCHANGE = "payment.exchange";

    public static final String PAYMENT_COMPLETED_QUEUE = "payment.completed.order.queue";

    public static final String PAYMENT_COMPLETED_ROUTING_KEY = "payment.completed";

    public static final String PAYMENT_FAILED_QUEUE = "payment.failed.order.queue";

    public static final String PAYMENT_FAILED_ROUTING_KEY = "payment.failed";

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    @Bean
    public Queue orderQueue() {
        return new Queue(ORDER_QUEUE);
    }

    @Bean
    public Binding orderBinding(@Qualifier("orderQueue") Queue orderQueue,
            @Qualifier("orderExchange") TopicExchange orderExchange) {

        return BindingBuilder.bind(orderQueue).to(orderExchange).with(ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    public TopicExchange stockExchange() {
        return new TopicExchange(STOCK_EXCHANGE);
    }

    // =========================
    // STOCK RESERVED
    // =========================

    @Bean
    public Queue stockReservedQueue() {
        return new Queue(STOCK_RESERVED_QUEUE);
    }

    @Bean
    public Binding stockReservedBinding(@Qualifier("stockReservedQueue") Queue stockReservedQueue,
            @Qualifier("stockExchange") TopicExchange stockExchange) {

        return BindingBuilder.bind(stockReservedQueue).to(stockExchange).with(STOCK_RESERVED_ROUTING_KEY);
    }

    // =========================
    // STOCK RESERVATION FAILED
    // =========================

    @Bean
    public Queue stockReservationFailedQueue() {
        return new Queue(STOCK_RESERVATION_FAILED_QUEUE);
    }

    @Bean
    public Binding stockReservationFailedBinding(
            @Qualifier("stockReservationFailedQueue") Queue stockReservationFailedQueue,

            @Qualifier("stockExchange") TopicExchange stockExchange) {

        return BindingBuilder.bind(stockReservationFailedQueue).to(stockExchange)
                .with(STOCK_RESERVATION_FAILED_ROUTING_KEY);
    }

    // =========================
    // RABBIT TEMPLATE
    // =========================

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {

        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);

        rabbitTemplate.setMessageConverter(messageConverter);

        return rabbitTemplate;
    }

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE);
    }

    @Bean
    public Queue paymentCompletedQueue() {
        return new Queue(PAYMENT_COMPLETED_QUEUE);
    }

    @Bean
    public Binding paymentCompletedBinding(@Qualifier("paymentCompletedQueue") Queue paymentCompletedQueue,

            @Qualifier("paymentExchange") TopicExchange paymentExchange) {

        return BindingBuilder.bind(paymentCompletedQueue).to(paymentExchange).with(PAYMENT_COMPLETED_ROUTING_KEY);
    }

    @Bean
    public Queue paymentFailedQueue() {
        return new Queue(PAYMENT_FAILED_QUEUE);
    }

    @Bean
    public Binding paymentFailedBinding(@Qualifier("paymentFailedQueue") Queue paymentFailedQueue,

            @Qualifier("paymentExchange") TopicExchange paymentExchange) {

        return BindingBuilder.bind(paymentFailedQueue).to(paymentExchange).with(PAYMENT_FAILED_ROUTING_KEY);
    }
}