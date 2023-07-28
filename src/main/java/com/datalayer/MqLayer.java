package com.datalayer;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.data.Userdetails;
import com.rabbitmq.client.AMQP.Queue.DeclareOk;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.rabbitmq.QueueSpecification;
import reactor.rabbitmq.Sender;

@Service
public class MqLayer {
		
	@Autowired
	Sender sender;
	
	public Mono<Void> createUserNotificationStream(Userdetails userdetails) {
		return sender.declare(getNewUserQueueSpec(userdetails.getAccountname()+"_notif")).then();
	}
	
	private Mono<DeclareOk> checkQueueInfo(String qname){
		return sender.declare(getNewUserQueueSpec(qname));
	}
	
	public Mono<Integer> checkQueueLength(String qname){
		return checkQueueInfo(qname).map((value)->value.getMessageCount());
	}
	
	private static QueueSpecification getNewUserQueueSpec(String queuename) {
		
		Map<String,Object> arguments =
				new HashMap<>();
		
		arguments.put("x-max-age", "90D");
		arguments.put("x-queue-type", "stream");
		arguments.put("x-max-length-bytes", Integer.valueOf(2500000));
		arguments.put("x-stream-max-segment-size-bytes", Integer.valueOf(500000));
		
		return new QueueSpecification()
				.autoDelete(false)
				.durable(true)
				.exclusive(false)
				.name(queuename)
				.arguments(arguments);
	}
	
		
	////////////////////////////////////////////////////////////////////////////////////////////////////////
	public Mono<Void> createUserNotificationStreams(String userdetails) {
		return sender.declare(getNewUserQueueSpec(userdetails)).then();
	}
	
	
}
