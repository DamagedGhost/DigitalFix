package com.digitalfix.ms_digitalfix_notify.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Define los exchanges, colas, DLQ y bindings de DigitalFix.
 * Acá solo se configura; la lógica de negocio vive en los consumers.
 *
 * Flujo: mensaje -> exchange (direct o topic) -> cola principal.
 * Si el consumer lo rechaza, la cola lo manda al DLX y cae en su DLQ.
 */
@Configuration
@EnableConfigurationProperties(DigitalfixRabbitProperties.class)
public class RabbitTopologyConfig {

    private final DigitalfixRabbitProperties props;

    public RabbitTopologyConfig(DigitalfixRabbitProperties props) {
        this.props = props;
    }

    // ---------- Exchanges ----------

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(props.exchanges().direct(), true, false);
    }

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(props.exchanges().topic(), true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(props.exchanges().dead(), true, false);
    }

    // ---------- Flujo 1: email / push al cliente ----------

    @Bean
    public Queue emailQueue() {
        return colaPrincipal(props.queues().email());
    }

    @Bean
    public Queue emailDlq() {
        return colaDlq(props.queues().email());
    }

    @Bean
    public Binding emailDirectBinding() {
        return BindingBuilder.bind(emailQueue()).to(directExchange())
                .with(props.queues().email().directKey());
    }

    @Bean
    public Binding emailTopicBinding() {
        return BindingBuilder.bind(emailQueue()).to(topicExchange())
                .with(props.queues().email().topicKey());
    }

    @Bean
    public Binding emailDlqBinding() {
        return BindingBuilder.bind(emailDlq()).to(deadLetterExchange())
                .with(props.queues().email().dlqKey());
    }

    // ---------- Flujo 2: ticket de despacho al técnico ----------

    @Bean
    public Queue dispatchQueue() {
        return colaPrincipal(props.queues().dispatch());
    }

    @Bean
    public Queue dispatchDlq() {
        return colaDlq(props.queues().dispatch());
    }

    @Bean
    public Binding dispatchDirectBinding() {
        return BindingBuilder.bind(dispatchQueue()).to(directExchange())
                .with(props.queues().dispatch().directKey());
    }

    @Bean
    public Binding dispatchTopicBinding() {
        return BindingBuilder.bind(dispatchQueue()).to(topicExchange())
                .with(props.queues().dispatch().topicKey());
    }

    @Bean
    public Binding dispatchDlqBinding() {
        return BindingBuilder.bind(dispatchDlq()).to(deadLetterExchange())
                .with(props.queues().dispatch().dlqKey());
    }

    // ---------- Flujo 3: generación de PDF ----------

    @Bean
    public Queue reportQueue() {
        return colaPrincipal(props.queues().report());
    }

    @Bean
    public Queue reportDlq() {
        return colaDlq(props.queues().report());
    }

    @Bean
    public Binding reportDirectBinding() {
        return BindingBuilder.bind(reportQueue()).to(directExchange())
                .with(props.queues().report().directKey());
    }

    @Bean
    public Binding reportTopicBinding() {
        return BindingBuilder.bind(reportQueue()).to(topicExchange())
                .with(props.queues().report().topicKey());
    }

    @Bean
    public Binding reportDlqBinding() {
        return BindingBuilder.bind(reportDlq()).to(deadLetterExchange())
                .with(props.queues().report().dlqKey());
    }

    // ---------- Helpers ----------

    // Cola durable que manda los mensajes rechazados al DLX con la llave de su DLQ
    private Queue colaPrincipal(DigitalfixRabbitProperties.QueueDef def) {
        return QueueBuilder.durable(def.name())
                .deadLetterExchange(props.exchanges().dead())
                .deadLetterRoutingKey(def.dlqKey())
                .build();
    }

    private Queue colaDlq(DigitalfixRabbitProperties.QueueDef def) {
        return QueueBuilder.durable(def.dlq()).build();
    }

    /**
     * Spring solo crea las colas cuando se abre la primera conexión a RabbitMQ.
     * Esto abre una al arrancar, así la topología se crea de inmediato.
     */
    @Bean
    public ApplicationRunner crearTopologiaAlArrancar(ConnectionFactory connectionFactory) {
        return args -> connectionFactory.createConnection().close();
    }
}