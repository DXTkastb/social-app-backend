package com.notificationservice;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamReceiver;
import org.springframework.data.redis.stream.StreamReceiver.StreamReceiverOptions;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Service;

import com.data.*;
import com.datalayer.RDBDaoLayer;
import com.datalayer.RedisLayer;
import com.fcmservice.FcmNotificationService;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.rabbitmq.ConsumeOptions;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.Sender;
import reactor.util.function.Tuple2;

@Service
public class NotificationService {

	@Autowired
	Sender sender;

	@Autowired
	RDBDaoLayer daolayer;

//	@Autowired
//	Receiver receiver;

	@Autowired
	RedisLayer redislayer;
	@Autowired
	FcmNotificationService fcmService;
	@Autowired
	@Qualifier(value = "redis-connection-factory")
	ReactiveRedisConnectionFactory redisConnectionFactory;
	
	public void createNotificationActivity(LikeActivityQuery lk) {
		if(lk.getPostCreatorName().equalsIgnoreCase(lk.getAccountname())) return;
		redislayer.addToUserNotificationStream(lk)
		.doOnNext(l->{
			System.out.println("Pushing to user phone!!!");
			fcmService.publishPushNotifications(lk.getPostCreatorName(), lk.getAccountname()+" liked your post.");
		})
		.subscribeOn(Schedulers.boundedElastic()).subscribe();
	}
	public void createNotificationActivity(CommentActivityQuery caq) {
		if(caq.getPostCreatorName().equalsIgnoreCase(caq.getAccountname())) return;
		redislayer.addToUserNotificationStream(caq)
		.doOnNext(l->{
			fcmService.publishPushNotifications(caq.getPostCreatorName(), caq.getAccountname()+" commented on your post.");
		})
		.subscribeOn(Schedulers.boundedElastic()).subscribe();
	}
	public void createNotificationActivity(Map<String, String>  fd) {
		redislayer.addToUserNotificationStream(fd)
		.doOnNext(l->{
			fcmService.publishPushNotifications(fd.get("followee"), fd.get("follower")+" started following you!");
		})
		.subscribeOn(Schedulers.boundedElastic()).subscribe();
	}
	
	public Mono<Tuple2<List<UiNotificationMessage>,Integer>> fetchUserLatestNotifications(String accountName) {
		return redislayer.getLatestNotificationsFromRedisStream(accountName)
				.map((notification)->{
					return new UiNotificationMessage(notification.getValue(),notification.getId().getValue());
				}).collectList()
				.zipWhen(list->{	
					if(list.isEmpty())
						return Mono.just(0);
					return redislayer.getUserLastNotificationViewID(accountName).map(str->{					
						String[] userLastSplits = str.split("-");
						Long l1 = Long.parseLong(userLastSplits[0]);
						Long l2 = Long.parseLong(userLastSplits[1]);
						
						String[] latestIdSplits = list.get(0).getId().split("-");
						Long l3 = Long.parseLong(latestIdSplits[0]);
						Long l4 = Long.parseLong(latestIdSplits[1]);
						if(l3>l1)
							return 1;
						if(l1==l3 && l4>l2)
							return 1;
						return 0;
					}).doOnError((e)->{
						System.out.println(e);
					});
				});
	}
	
	public void subscribeToRedisReactiveStreamNotifications(RSocketRequester requester, String accountName,
			String lastId) {
		StreamReceiver<String, ObjectRecord<String, NotificationMessage>> streamReceiver = StreamReceiver.create(redisConnectionFactory,
				StreamReceiverOptions.builder().pollTimeout(Duration.ofSeconds(2)).batchSize(100).targetType(NotificationMessage.class).build());
		StreamOffset offset;		
		if(lastId.equalsIgnoreCase("0")) {
			offset = StreamOffset.fromStart(accountName+":notifications");	
		}
		else offset = StreamOffset.create((accountName+":notifications"), ReadOffset.from(lastId));
		 
		Flux<ObjectRecord<String, NotificationMessage>> notificationStream = streamReceiver
				.receive(offset);

		Disposable disposable = notificationStream
				.map((notification) -> new UiNotificationMessage(notification.getValue(),notification.getId().getValue()))
				.flatMapSequential((value) -> {
					return requester.route("").data(value).retrieveMono(String.class);
				})
				.onErrorComplete()
				.subscribeOn(Schedulers.boundedElastic()).subscribe();
		
		requester.rsocket().onClose().doOnError((e) -> {
			System.out.println("WARNING : CHANNEL MAY BE CLOSED!");
		}).doFinally((consumer) -> {
			System.out.println("DISPOSING REDIS-STREAM CONSUMER!!");
			disposable.dispose();
		}).subscribe();
	}
	
	public Mono<List<UiNotificationMessage>> oldUiNotificationsForUser(String accountName,String lastFetchID) {
		return redislayer.fetchOldNotificationsForUser(accountName,lastFetchID)
		.map((notification)->{
			return new UiNotificationMessage(notification.getValue(),notification.getId().getValue());
		}).collectList();
	}

	@Deprecated
	public void createNotification(CommentActivityQuery comment) {
//		String notification = comment.getAccountname() + ".commented on your post." + comment.getOnpostid();
//		redislayer.pushNotificationToUserList(comment.getPostCreatorName(), notification)
//				.then(sender.send(Mono
//						.just(new OutboundMessage(null, comment.getAccountname() + "_notif", notification.getBytes()))))
//				.onErrorComplete().subscribeOn(Schedulers.boundedElastic()).subscribe();
	}
	
	@Deprecated
	
	public void createNotification(LikeActivityQuery like) {
//		String notification = like.getAccountname() + ".liked your post." + like.getOnpostid();
//		redislayer.pushNotificationToUserList(like.getPostCreatorName(), notification)
//				.then(sender.send(Mono.just(
//						new OutboundMessage(null, like.getPostCreatorName() + "_notif", notification.getBytes()))))
//				.onErrorComplete().subscribeOn(Schedulers.boundedElastic()).subscribe();
	}
	
	/*
	 * 
	 * RabbitMQ Streams design is deprecated. Redis Streams are used for fetching notifications.
	 * 
	 */
	
	@Deprecated
	public void pushNotificationsToApp(RSocketRequester requester, Userdetails userdetails, Object offset) {

		//		Map<String, Object> arguments = new HashMap<>();
		//		arguments.put("x-stream-offset", offset);
		//
		//		Disposable disp = receiver
		//				.consumeManualAck(userdetails.getAccountname() + "_notif",
		//						(new ConsumeOptions()).qos(200).arguments(arguments))
		//				.bufferTimeout(200, Duration.ofSeconds(1)).flatMapSequential((delivery) -> {
		//
		//					if (requester.isDisposed()) {
		//						System.out.println("socket closed");
		//						throw new IllegalStateException("Rscoket closed!");
		//					}
		//
		//					int size = delivery.size();
		//					var lastdel = delivery.get(size - 1);
		//
		//					return requester.route("").data(delivery).retrieveMono(String.class).doOnNext((string) -> {
		//						if (string.compareTo("201") == 0) {
		//							// System.out.println("acknowledging message!");
		//							lastdel.ack(true);
		//						}
		//					});
		//
		//				}).onErrorComplete().subscribe();
		//
		//		// when client disconnects, rabbitmq consumption should stop.
		//
		//		requester.rsocket().onClose().doOnError((e) -> {
		//			System.out.println("WARNING : CHANNEL MAY BE CLOSED!");
		//		}).doFinally((consumer) -> {
		//			System.out.println("DISPOSING RABBITMQ CONSUMER!!");
		//			disp.dispose();
		//		}).subscribe();

	}



	


	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// TESTING
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	
//	public Flux<UiNotificationMessage>  subscribeToRedisReactiveStreamNotificationsTest(String offset) {
//		StreamReceiver<String, ObjectRecord<String, NotificationMessage>> streamReceiver = StreamReceiver.create(redisConnectionFactory,
//				StreamReceiverOptions.builder().pollTimeout(Duration.ofSeconds(30)).batchSize(100).targetType(NotificationMessage.class).build());
//
//		Flux<UiNotificationMessage> notificationStream = streamReceiver
//				.receive(StreamOffset.create("dxtk:notifications", ReadOffset.from(offset))).map(n->new UiNotificationMessage(n.getValue(),n.getId().getValue()));
//		return notificationStream;
//
//	}
//	
//	
//	public Flux<ObjectRecord<String,NotificationMessage>> subscribeToRedisReactiveStreamNotificationsTest2() {
//		StreamReceiver<String, ObjectRecord<String, NotificationMessage>> streamReceiver = StreamReceiver.create(redisConnectionFactory,
//				StreamReceiverOptions.builder().batchSize(100).targetType(NotificationMessage.class).build());
//
//		Flux<ObjectRecord<String,NotificationMessage>> notificationStream = streamReceiver
//				.receive(StreamOffset.fromStart("dxtk:notifications"));
//		return notificationStream;
//
//	}
//	
//	
//	public Mono<Void> dummyCreateNotif() {
//		String test = "HAHAHAHAHAHA :";
//		return sender.send(Mono.just(new OutboundMessage("", "dxtk_notif", test.getBytes())));
//	}
//
//	public Mono<Void> createNotification2() {
//		String test = "num";
//		return sender.send(Mono.just(new OutboundMessage("", "abc", test.getBytes())));
//	}
//	
//	public void testNoti() {
//		redislayer.addtostreamNf();
//	}
//	
//	public Mono<String> getHashDATA() {
//		return redislayer.getUserLastNotificationViewID("undertaker");
//	}
}
