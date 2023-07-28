package com.controllers;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.annotation.ConnectMapping;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.data.FetchStoryQuery;
import com.data.FetchStoryQueryMessage;
import com.data.NotificationMessage;
import com.data.Post;
import com.data.Story;
import com.data.UiNotificationMessage;
import com.data.UiNotificationMessages;
import com.data.Userdetails;
import com.data.UserdetailsMessage;
import com.datalayer.RDBDaoLayer;
import com.datalayer.RedisLayer;
import com.fcmservice.FcmNotificationService;
import com.feedservice.FeedGenerationService;
import com.headers.CustomHeaders;
import com.notificationservice.NotificationService;
import com.redis.lettucemod.search.SearchResults;
import com.storyservices.StoryFetchNewService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


@RestController
@Deprecated
public class TestController2 {

//	@Autowired
//	RedisLayer redislayer;
//	@Autowired
//	NotificationService nfs;
//	
//	@Autowired
//	FeedGenerationService feddgenerationservice;
//
//	@Autowired
//	StoryFetchNewService storyFetchNewService;
//
//	@Autowired
//	RDBDaoLayer daolayer;
//	@Autowired
//	NotificationService notificationService;
//
//	@Autowired
//	FcmNotificationService fns;
////	static RSocketRequester req;
////	
////	@ConnectMapping
////	public void connect(RSocketRequester requester)
////	{
////		req = requester;
////		System.out.println("CONNECTED RSOCKET");
////	}
////	
////	@GetMapping("zzz")
////	public Mono<String> xxx() {
////		req
////		
////		.metadata("NONE",MimeTypeUtils.TEXT_PLAIN)
////		//.metadata("STORY",MimeTypeUtils.parseMimeType("message/tyty.tyty"))
////		//.metadata("TimeLine",MimeTypeUtils.parseMimeType("message/tyty.tyty2"))
////		.data("DATA")
////		.retrieveMono(String.class)
////		.subscribe();
////		return Mono.just("www");
////	}
//
////	@GetMapping("us")
////	public Mono<Integer> us
////	(){
////		return redisLayer.getsearchUsers("Kaus");
////	}
////	
//
////	@GetMapping("dkn")
////	Mono<List<ObjectRecord<String, NotificationMessage>>>  dkkn(){
////		return redisLayer.getLatestNotificationsFromRedisStreams("dxtk");
////	}
//
////	@GetMapping("tostream")
////	Mono<Integer> dsfsdef(){
////		return redisLayer.addToUserNotificationStream();
////	}
//
//	@GetMapping("ftsearch")
//	public Mono ftsearchFunc() {
//		return redislayer.getsearchUsers("ma").map((results) -> {
//			List<UserdetailsMessage> users = new ArrayList<>();
//			results.forEach((x) -> {
//				UserdetailsMessage udm = new UserdetailsMessage();
//				udm.setAccountname(x.get("acname"));
//				udm.setProfileurl(x.get("pi-url"));
//				users.add(udm);
//			});
//			return users;
//		});
//	}
//
//	@GetMapping("stnf")
//	public Mono<ObjectRecord<String, NotificationMessage>> uinm() {
//		return nfs.subscribeToRedisReactiveStreamNotificationsTest2().next();
//	}
//
////	@GetMapping("stnf2")
////	public Flux<UiNotificationMessage>  uinm2(){
////		return nfs.subscribeToRedisReactiveStreamNotificationsTest();
////	}
//
//	@GetMapping("atos")
//	public Mono<Integer> attso() {
//		return redislayer.addtostreamNf();
//	}
//
//	@GetMapping("pds")
//	public Mono<Boolean> postDummyStory() {
//		Story story = new Story();
//		story.setAccountname("dxtk");
//		story.setImgurl("asasa");
//		story.setStime(Instant.now());
//		story.setIsmemory(56464631l);
//		story.setStoryid(7825782);
//		return redislayer.uploadStoryToUserSet(story);
//	}
//
//	@GetMapping("fsqmm")
//	public Mono<FetchStoryQueryMessage> getStoriesSet() {
//		long instant1 = Instant.now().getEpochSecond();
//		return storyFetchNewService.getSpecificUserStory("dxtk", "dxtk", instant1 - 86400, instant1);
//	}
//
//	@GetMapping("stids")
//	public Mono<List<Integer>> getstoryIDSS() {
//		long instant1 = Instant.now().getEpochSecond();
//		return redislayer.getStoriesOfSpecificUser("dxtk" + ":stories", instant1 - 86400, instant1).doOnNext((c) -> {
//			System.out.println(c);
//		}).collectList();
//	}
//
//	@GetMapping("redisStream")
//	public Flux<UiNotificationMessage> getStream() {
//		return notificationService.subscribeToRedisReactiveStreamNotificationsTest("1678174785883-0");
//	}
//
//	@GetMapping("getuc")
//	public Mono<UserdetailsMessage> getUserCache() {
//		return redislayer.getUserDetailsFromRedisCache("dxtk");
//	}
//
//	@GetMapping("usrdd")
//	public Mono<UiNotificationMessages> getUserNotifications() {
//		return notificationService.fetchUserLatestNotifications("dxtk").map(value -> {
//			UiNotificationMessages uinm = new UiNotificationMessages();
//			uinm.setUiMessages(value.getT1());
//			System.out.println(value.getT2());
//			uinm.setNewNotificationCount(value.getT2());
//			return uinm;
//		});
//	}
//
//	@GetMapping("gln")
//	public Mono getGln() {
//		return redislayer.getUserLastNotificationViewID("dxtk");
//	}
//
//	@GetMapping("ufk")
//	public Mono upkey() {
//		return daolayer.updateFkey("dxtk", "DFDFDFDFDF");
//	}
//
//	@GetMapping("ppfn")
//	public Mono<Integer> ppp() {
//		fns.publishPushNotifications("dxtk", "Amber liked your Post!");
//		return Mono.just(1);
//	}
//
//	@GetMapping("pufk")
//	public Mono pupkey() {
//		return daolayer.pushFcmKey(
//				"cJPv6bWdS4Wj49zuUdt59J:APA91bFyW31cb0kmR-b6QZESUVbXZHxYX7lpgupYr1hTkgOhDkCB5b29prhDjcTrYLfM2irbjBWN6zEr7BbV1raGrvHRiX-UmCDKSGVclKvz01JPvj8t5LrIPRFkIcx_QCUB5W2WJZFA");
//	}
//
//	@GetMapping("uppd")
//	public Mono<Integer> upppds() {
//		Userdetails uss = new Userdetails();
//		uss.setAbout("abbty");
//		Instant x = Instant.now();
//		uss.setLink("" + x.getEpochSecond());
//		uss.setDelegation("delegation");
//		;
//		uss.setAccountname("Kumar");
//		return daolayer.updateUserDetail(uss).then(Mono.just(100));
//
//	}
//
//	@GetMapping("yty")
//	public Mono tyty() {
//		return redislayer.getUnseenStoriesTest("amber", List.of("dxtk1", "rtr"));
//	}
//	
//	@GetMapping("gps")
//	public Mono gPPs() {
//      return daolayer.getUserPostsFromDb("undertaker", List.of(32)).collectList();
//	}
//
//	@GetMapping("fakep")
//	public Mono addPost()
//			throws IOException {
//
//
//	
//		
//		//String filePathName = fileName + "." + fileExtn;
//
//
//		
//		List<Post> postList = new ArrayList<>();
//		
//		int x = 37;
//		
//		while(x>0) {
//
//			Post post = new Post();
//			post.setAccountname("undertaker");
//			post.setImgurl("ggg");
//			post.setCaption("this is caption number:"+x);
//			post.setCtime(Instant.now());
//			
//			postList.add(post);
//			
//			x--;
//		}
//
//		return Flux.fromIterable(postList).flatMap((pv)->{
//			return daolayer.createPost(pv).doOnNext((generated_post) -> {
//				try {
//					
//					System.out.println("generated post caption:"+
//							generated_post.getCaption()
//							);
//					
//					/*
//					 * Calling FeedService asynchronously
//					 */
//
//					feddgenerationservice.generateFeeds(generated_post).onErrorComplete()
//							.subscribeOn(Schedulers.boundedElastic()).subscribe();
//				} catch (Exception exception) {}
//					}).zipWhen(p->redislayer.incrementPostCount("undertaker",true));
//		}).then();
//		
//	}
	
}
