package com.controllers;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.codec.cbor.Jackson2CborEncoder;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.data.LikeActivityQuery;
import com.data.Likes;
import com.data.Post;
import com.data.PostFetchQuery;
import com.data.Story;
import com.data.UiNotificationMessages;
import com.datalayer.MqLayer;
import com.datalayer.RDBDaoLayer;
import com.datalayer.RedisLayer;
import com.notificationservice.NotificationService;
import com.rabbitmq.client.AMQP.BasicProperties;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.ConsumeOptions;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.Sender;

@RestController
@Deprecated
public class TestController {

//	public static int x =1;
//	
//	@Value("classpath:input/apple.jpg")
//	private Resource rs;
//	
//	@Autowired
//	RDBDaoLayer daolayer;
//	
//	@Autowired
//	RedisLayer redisLayer;
//
//	@Autowired
//	@Qualifier(value = "redis-template")
//	ReactiveStringRedisTemplate redisTemplate;
//	
////	@Autowired
////	FeedGenerationService feddgenerationservice;
////	
////	@Autowired
////	FeedFetchService feedfetchservice;
////	
//	
//	@Autowired
//	NotificationService notificationService;
//	
//	
//@Autowired
//Sender sender;
//	
//	@Autowired
//	MqLayer mqlayer;
//	
//	@Autowired
//	Receiver r;
//
//	
//	@GetMapping(value ="fss")
//	@ResponseBody
//	public Mono<List<String>> getlistdd(@RequestBody String accountname) {
//		return daolayer.getFollowersForStories(accountname);
//	}
//	@GetMapping(value = "llll")
//	@ResponseBody
//	public String postLike(@RequestParam String name, @RequestParam String to) {
//
//		Likes like = new Likes();
//		like.setAccountname(name);
//		like.setOnpostid(35);
//		LikeActivityQuery laq = new LikeActivityQuery();
//		laq.setAccountname(name);;
//		laq.setOnpostid(35);;
//		laq.setPostCreatorName(to);
//		daolayer.postLike(like).doOnNext((l) -> {
//			notificationService.createNotificationActivity(laq);
//		}).subscribe();
//		return "SSSS";
//	}
//	
//	@GetMapping("getnn")
//	@ResponseBody
//	public Mono<UiNotificationMessages> getUserNotifications() {
//		return notificationService.fetchUserLatestNotifications("undertaker").map(value -> {
//			UiNotificationMessages uinm = new UiNotificationMessages();
//			System.out.println(value.getT1());
//			System.out.println(value.getT2());
//			uinm.setUiMessages(value.getT1());
//			uinm.setNewNotificationCount(value.getT2());
//			return uinm;
//		});
//	}
//	
//	@GetMapping("tnn")
//	@ResponseBody
//	public Mono<String> getet() {
//		return notificationService.getHashDATA();
//	}
//	
//	@GetMapping("redisp")
//	@ResponseBody
//	public Mono<PostFetchQuery> fetchFreshPosts() {
//		return redisLayer.getListSize("dxtk:timeline").flatMap((size) -> {
//			PostFetchQuery posts = new PostFetchQuery();
//			posts.setPostlist(new ArrayList<Integer>());
//			posts.setMaxIndex(size - 1);
//			posts.setMinIndex((size - 35) > 0 ? (size - 35) : 0);
//			return redisTemplate.opsForList().range("dxtk:timeline", posts.getMinIndex(), posts.getMaxIndex())
//					.map(s -> Integer.parseInt(s)).doOnNext((v) -> {
//						posts.getPostlist().add(v);
//					})
//					.then(Mono.just(posts))
//					// .subscribeOn(Schedulers.boundedElastic())
//					;
//		});
//	}
//	
//	
//	
//	
//	
//	@GetMapping("gpp")
//	@ResponseBody
//	public Mono<List<Post>> getp(){
//		return daolayer.getposts();
//	}
//
//	@GetMapping("ql")
//	@ResponseBody
//	public Mono<Integer> qlength(){
//		return mqlayer.checkQueueLength("abc");
//	}
//	
////	
////	@GetMapping("posting")
////	@ResponseBody
////	public Mono<Post> cc() {
////	
////		return daolayer.createPost("", "", "");
////	}
//	
//	@GetMapping("bnbn")
//	public Mono<List<Story>> getStoriesSet2() {
//		return daolayer.getUserLatestStories("amber");
//	}
//	@GetMapping("lik")
//	@ResponseBody
//	public Mono cc5() {
//		return daolayer.getUserLatestStories("amber");
//	}
//	
//	
//
//
//	@MessageMapping
//	public Mono<String> ff(@Headers Map<String, Object> headers) {
//		System.out.println("55555555555555552222222222222222222222222222222222222");
//		headers.forEach((s, o) -> {
//			System.out.print(s + "_________________");
//			System.out.println(o);
//
//		});
//		System.out.println("55555555555555552222222222222222222222222222222222222");
//		return Mono.just("kjvhfkhjvgf");
//	}
//
//	@MessageMapping("")
//	public Mono<String> fsf() {
//		return Mono.just("kjghfhf");
//	}
//
//	@MessageMapping("dart")
//	public Mono<String> fsf23(@Headers Map<String, Object> headers) {
//		System.out.println("55555555555555552222222222222222222222222222222222222");
//		headers.forEach((s, o) -> {
//			System.out.print(s + "_________________");
//			System.out.println(o);
//
//		});
//		System.out.println("55555555555555552222222222222222222222222222222222222");
//		return Mono.just("kjf");
//	}
//
////	@GetMapping("postup")
////	@ResponseBody
////	public String up() throws IOException {
////		// Resource re = new Resource();
////
////		Flux<DataBuffer> readFlux = DataBufferUtils.read(resource, new DefaultDataBufferFactory(), 4096)
////				.doOnNext(s -> System.out.println("Sent"));
////		Mono<DataBuffer> data = DataBufferUtils.join(readFlux);
////
////		daolayer.uploadPhoto(data, "apple.jpg").then(daolayer.createPost("dxtk", "apple.jpg", "this is apple"))
////				.subscribe();
////
////		return "D";
////	}
//
//	@GetMapping("m")
//	@ResponseBody
//	public String s() {
//
//		////////////////////////////
//
////		RSocketStrategies strategies = RSocketStrategies.builder()
////				.encoders(encoders -> encoders.add(new Jackson2CborEncoder()))
////				// .metadataExtractorRegistry(registry -> registry.metadataToExtract(
////				// MimeTypeUtils.parseMimeType(WellKnownMimeType.MESSAGE_RSOCKET_MIMETYPE.getString()),
////				// String.class, "xload"))
////				// .encoders(encoders -> encoders.add(new Jackson2JsonEncoder()))
////				.build();
//
//		//////////////////////////////////////////////////////////////////////
//
//	
//
//		//////////////////////////////////////////////////////////////////////
////
////		req.route("")
////				// .metadata(MimeTypeUtils.APPLICATION_JSON_VALUE,
////				// MimeTypeUtils.parseMimeType(WellKnownMimeType.MESSAGE_RSOCKET_MIMETYPE.getString()))
////				.data("dummy")
////
////				.retrieveMono(String.class).subscribe((value) -> {
////					System.out.println(value);
////				});
//
//		return "s";
//	}
//	
//	@GetMapping("qqq")
//	@ResponseBody
//	public String rRE() {
////		req.route("kill")
////		.retrieveMono(String.class).subscribe();
//		
//		return "EWRWER";
//	}
//	
//	
//	
//	// **********************************************************************************************
//	// kkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkk
//	// **********************************************************************************************
//	// kkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkk
//	
//	@GetMapping("pushn")
//	public Mono<String> vv2()
//	{
//		return 	notificationService.dummyCreateNotif().then(Mono.just("sdfsdf"));
//	}
//	
//	@GetMapping("cnn")
//	public Mono<String> cnn()
//	{
//		return 			mqlayer.createUserNotificationStreams("dxtk_notif").then(Mono.just("done!"));
//	}
//	
//	
//	
//	
//	// **********************************************************************************************
//	// kkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkk
//	// **********************************************************************************************
//	// kkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkk
//	
//	
//	
//	@GetMapping("mq2")
//	public Mono<String> vvd2()
//	{
//		 System.out.println(x);
//		
//		return notificationService.createNotification2().then(Mono.just("54df"));
//	}
//	
//	@GetMapping("rrr")
//	public Flux<String> value(){
//		Map<String, Object> arguments = new HashMap<>();
//		arguments.put("x-stream-offset", "first");
//
//		 return r.consumeManualAck("abc", (new ConsumeOptions()).qos(200).arguments(arguments))
//				 .map((del)->{
//					 del.ack();
//					 System.out.println(x);
//					 x++;
//					 return new String(del.getBody());
//				 });
//	
//	}
//	
//	@GetMapping("rrr2")
//	public Flux<String> value2(){
//		Map<String, Object> arguments = new HashMap<>();
//		arguments.put("x-stream-offset", "first");
//
//		 return r.consumeManualAck("abc", (new ConsumeOptions()).qos(200).arguments(arguments))
//				 .map((del)->{
//					 
//					 
//					 BasicProperties bp = del.getProperties();
//					 System.out.println("apd-id		:"+bp.getAppId());
//					 System.out.println("msg-id		:"+bp.getMessageId());
//					 System.out.println("timestamp	:"+bp.getTimestamp());
//					 
//					 
//					 bp.getHeaders().forEach((k,v)->{
//						
//						 System.out.print(k + ":");
//						 System.out.println(v);
//					 });
//					 
//					 String m = new String(del.getBody());System.out.println(m);
//					 System.out.println("___________________________________");
//					 return m;
//				 });
//	
//	}
	
}
