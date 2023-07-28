package com.configs;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import com.rabbitmq.client.*;

import reactor.core.publisher.Mono;
import reactor.rabbitmq.*;

@Configuration
public class RabbitConfig {

    @Bean(name = "sender-connection-bean")
    Mono<Connection> senderConnectionMono() {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.useNio();
        connectionFactory.setHost("localhost");
        connectionFactory.setPort(5672);
        connectionFactory.setUsername("dxtkb");
        connectionFactory.setPassword("ubuntu@1898");
        return Mono.fromCallable(() -> connectionFactory.newConnection("reactor-rabbit-sender")).cache();
    }

    @Bean(name = "sender-bean")
    Sender sender(@Qualifier(value = "sender-connection-bean") Mono<Connection> connection) {
        return RabbitFlux.createSender(new SenderOptions().connectionMono(connection));
    }

    @Bean(name = "receiver-connection-bean")
    Mono<Connection> receiverConnectionMono() {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.useNio();
        connectionFactory.setHost("localhost");
        connectionFactory.setPort(5672);
        connectionFactory.setUsername("dxtkb");
        connectionFactory.setPassword("ubuntu@1898");
        return Mono.fromCallable(() -> connectionFactory.newConnection("reactor-rabbit-receiver")).cache();
    }

    @Bean(name = "receiver-bean")
    Receiver receiver(@Qualifier(value = "receiver-connection-bean") Mono<Connection> connection) {
        return RabbitFlux.createReceiver(new ReceiverOptions().connectionMono(connection));
    }


}