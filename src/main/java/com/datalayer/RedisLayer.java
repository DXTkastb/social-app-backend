package com.datalayer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Range;
import org.springframework.data.domain.Range.Bound;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;

import com.data.CommentActivityQuery;
import com.data.FollowerStory;
import com.data.Followers;
import com.data.LatestNotificationViewQuery;
import com.data.LikeActivityQuery;
import com.data.NotificationMessage;
import com.data.PostFetchQuery;
import com.data.PostFetchQuery;
import com.data.Story;
import com.data.Userdetails;
import com.data.UserdetailsMessage;
import com.redis.lettucemod.api.reactive.RedisModulesReactiveCommands;
import com.redis.lettucemod.search.Limit;
import com.redis.lettucemod.search.SearchOptions;
import com.redis.lettucemod.search.SearchResults;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;

@Service
public class RedisLayer {
	static HashMap<String, Long> staticLastMemoryId = new HashMap<>();
	static {
		staticLastMemoryId.put("memoryId", 0l);
		staticLastMemoryId.put("last-memory-second-epoch", 0l);
	}

	static final List<Object> userDetailsCacheKeys = List.of("acname", "uname", "pi-url", "about", "delegation", "link",
			"postscount", "followerscount", "followingcount");

	static final List<Object> userMemoryCacheKeys = List.of("memoryid", "last-memory-second-epoch");

	@Value("classpath:scripts/storyUploadAndViews.lua")
	private Resource storyUploadAndViewsScript;

	@Value("classpath:scripts/checkUnseenStoriesCount.lua")
	private Resource checkUnseenStoriesCountScript;

	@Value("classpath:scripts/alterfollowCount.lua")
	private Resource updateFollowingCount;

	@Value("classpath:scripts/getUnseenStories.lua")
	private Resource getUnseenStories;

	@Autowired
	@Qualifier(value = "redis-template")
	ReactiveStringRedisTemplate redisTemplate;

	@Autowired
	RedisModulesReactiveCommands<String, String> redisModuleCommands;

	public Flux<Integer> getNewStoriesIds(String accoutname, Long instant) {
		return redisTemplate.opsForZSet()
				.rangeByScoreWithScores(accoutname + ":unseen-stories",
						Range.closed((double) instant, Double.MAX_VALUE))
				.map((val) -> Integer.parseInt(val.getValue()));
	}

//	public Mono<Void> createPostTimelineList(Userdetails userdetails) {
//
//		return redisTemplate.hasKey(userdetails.getAccountname()).flatMap((value) -> {
//			if (value) {
//				return Mono.empty().then();
//			}
//			return redisTemplate.opsForList().rightPush(userdetails.getAccountname() + ":timeline", "NO-MORE_POSTS");
//		}).zipWith(redisTemplate.hasKey(userdetails.getAccountname() + ":unseen-stories").flatMap((value) -> {
//			if (value) {
//				return Mono.empty().then();
//			}
//			return redisTemplate.opsForZSet().add(userdetails.getAccountname() + ":unseen-stories", "0", -1).then();
//		}))
//				 .zipWith(	 
//						 redisTemplate.hasKey(userdetails.getAccountname()+":seen-stories")
//						 .flatMap((value)->{
//							 if(value) {
//									return Mono.empty().then();
//								}
//								return redisTemplate
//										.opsForZSet()
//										.add(userdetails.getAccountname()+":stories","0", -1)
//										.then();}))
//				);
//				.then().subscribeOn(Schedulers.boundedElastic());
//	}

	public Flux<Integer> getStoriesForUser(String accountname_key, Long instantTime) {

		double currentTime = instantTime;
		double past24hour = currentTime - 86400000;

		return redisTemplate.opsForZSet().rangeByScoreWithScores(accountname_key, Range.closed(past24hour, currentTime))
				.map((tuple) -> {
					return Integer.parseInt(tuple.getValue());
				});

	}

	public Mono<Void> pushDataToTimeline(String accountname, Integer postid) {
		String postID = "" + postid;
		return redisTemplate.opsForList().rightPush(accountname + ":timeline", postID).then();
	}

	public Mono<Void> pushStoryToTimeline(String accountname, Integer storyid, long score) {
		return redisTemplate.opsForZSet().add(accountname + ":unseen-stories", "" + storyid, (double) score).then();

	}

	public Mono<Void> removeFromUnseenStory(String accountname, Integer storyid, long score) {
		return redisTemplate.opsForZSet().remove(accountname, storyid)
				.then(redisTemplate.opsForZSet().add(accountname + ":seen-stories", "" + storyid, score)).then();
	}

	public Mono<Long> getListSize(String accountname) {
		return redisTemplate.opsForList().size(accountname);
	}

	public Mono<PostFetchQuery> fetchFreshPosts(String accountname) {

		return getListSize(accountname).flatMap((size) -> {

			final PostFetchQuery posts = new PostFetchQuery();
			posts.setPostlist(new ArrayList<Integer>());
			posts.setMaxIndex(size - 1);
			posts.setMinIndex((size - 35) > 0 ? (size - 35) : 0);
			return Mono.just(posts)
					.zipWith(redisTemplate.opsForList().range(accountname, posts.getMinIndex(), posts.getMaxIndex())
							.map(s -> Integer.parseInt(s)).collectList())
					.map((tuple) -> {
						PostFetchQuery posts2 = new PostFetchQuery();
						posts2.setPostlist(tuple.getT2());
						posts2.setMaxIndex(tuple.getT1().getMaxIndex());
						posts2.setMinIndex(tuple.getT1().getMinIndex());
						return posts2;
					});

//			return redisTemplate.opsForList().range(accountname, posts.getMinIndex(), posts.getMaxIndex())
//					.map(s -> Integer.parseInt(s)).doOnNext((v) -> {
//						posts.getPostlist().add(v);
//					}).then(Mono.just(posts));
		});
	}

	public Mono<PostFetchQuery> fetchUserLatestPostIds(String accountname, Long maxIndex, Long minIndex) {

		if (maxIndex == -1l && minIndex == -1l)
			return fetchFreshPosts(accountname);

		return getListSize(accountname).flatMap((size) -> {

			if ((minIndex == -1 && maxIndex == size - 1) || (minIndex == 0 && maxIndex == -1)) {
				PostFetchQuery posts = new PostFetchQuery();
				posts.setMinIndex(minIndex);
				posts.setMaxIndex(maxIndex);
				return Mono.just(posts);
			}

			if (minIndex == -1 && maxIndex < size - 1) {
				return redisTemplate.opsForList().range(accountname, maxIndex + 1, size - 1)
						.map((s) -> Integer.parseInt(s)).collectList().map(list -> {
							PostFetchQuery posts = new PostFetchQuery();
							posts.setPostlist(list);
							posts.setMaxIndex(size - 1);
							posts.setMinIndex(-1l);
							return posts;
						});
			}

			if (minIndex > 0 && maxIndex == -1) {

				return redisTemplate.opsForList()
						.range(accountname, ((minIndex >= 35) ? (minIndex - 35) : 0), minIndex - 1)
						.map((s) -> Integer.parseInt(s)).doOnNext(t -> {

						}).collectList().map(list -> {
							PostFetchQuery posts = new PostFetchQuery();
							posts.setMinIndex((minIndex >= 35) ? (minIndex - 35) : 0);
							posts.setMaxIndex(-1l);
							posts.setPostlist(list);
							return posts;
						});
			}
			return Mono.just(new PostFetchQuery());
		});
	}

	public Mono<Integer> pushNotificationToUserList(String accountname, String notification) {
		return redisTemplate.opsForList().rightPush(accountname + ":notifications", notification).then(Mono.just(200))
				.onErrorReturn(500);
	}

	public Mono<List<String>> getOldNotifications(String accountname, Integer indexOffset) {
		return redisTemplate.opsForList().range(accountname + ":notifications", indexOffset - 200, indexOffset)
				.collectList();
	}

	public Mono<Boolean> createUserCache(Userdetails userdetails) {
		Map<String, Object> map = new HashMap<>();

		map.put("acname", userdetails.getAccountname());
		map.put("uname", userdetails.getUsername());
		map.put("about", "");
		map.put("delegation", "");
		map.put("link", "");
		map.put("postscount", "0");
		map.put("followerscount", "0");
		map.put("followingcount", "0");
		map.put("latestNotificationID", "0-0");
		map.put("pi-url", userdetails.getProfileurl());

		return redisTemplate.opsForHash().putAll("search:" + userdetails.getAccountname(), map)
				.subscribeOn(Schedulers.boundedElastic()).doOnError((e) -> {
					e.printStackTrace();
				});
	}

	public Mono<Boolean> updateUserCache(Userdetails userdetails) {
		Map<String, Object> map = new HashMap<>();
		map.put("about", userdetails.getAbout());
		map.put("delegation", userdetails.getDelegation());
		map.put("link", userdetails.getLink());
		map.put("uname", userdetails.getUsername());
		return redisTemplate.opsForHash().putAll("search:" + userdetails.getAccountname(), map);
	}

	public Mono<Boolean> updateUserProfileUrlCache(String accountName, String profileUrl) {
		Map<String, Object> map = new HashMap<>();
		map.put("pi-url", profileUrl);
		return redisTemplate.opsForHash().putAll("search:" + accountName, map);
	}

	public Mono<String> getUserProfileUrlCache(String accountName) {
		return redisTemplate.opsForHash().get("search:" + accountName, "pi-url").map(str -> (String) str);
	}

	public Mono<SearchResults<String, String>> getsearchUsers(String nameQuery) {
		return redisModuleCommands.ftSearch("user:search", nameQuery + "*");
	}

	// check this now

	public Mono<UserdetailsMessage> getUserDetailsFromRedisCache(String accountName) {
		return redisTemplate.opsForHash().multiGet("search:" + accountName, userDetailsCacheKeys).map((u) -> {
	
			UserdetailsMessage user = new UserdetailsMessage();
		
			user.setAccountname((String) u.get(0));

			user.setUsername((String) u.get(1));
	
			user.setProfileurl((String) u.get(2));
		
			user.setAbout((String) u.get(3));
	
			user.setDelegation((String) u.get(4));
	
			user.setLink((String) u.get(5));
	
			user.setPostsCount(Integer.parseInt((String) u.get(6)));

			user.setFollowerCount(Integer.parseInt((String) u.get(7)));
	
			user.setFollowingCount(Integer.parseInt((String) u.get(8)));

			return user;
		});
	}

	public Mono<Long> incrementPostCount(String accountName, boolean increment) {
		int x = -1;
		if (increment)
			x = 1;
		return redisTemplate.opsForHash().increment("search:" + accountName, "postscount", x);
	}

	public Mono<Boolean> incrementFollowingCount(String follower, String followee, String increment) { // increase
		List<String> keys = List.of(follower, followee);

		String arg1 = increment;
		List args = List.of(arg1);
		return redisTemplate.execute(RedisScript.of(updateFollowingCount)).then(Mono.just(true));
	}
	
	public Mono<Boolean> incrementFollowingCount2(String follower, String followee, double increment) { // increase
		return redisTemplate.opsForHash().increment("search:"+follower,"followingcount", increment).zipWith(
				redisTemplate.opsForHash().increment("search:"+followee,"followerscount", increment)).map(x->true);
	}

	public Flux<ObjectRecord<String, NotificationMessage>> getLatestNotificationsFromRedisStream(String accountname) {
		String key = accountname + ":notifications";
		org.springframework.data.redis.connection.Limit lt = new org.springframework.data.redis.connection.Limit();
		lt.count(50);
		return redisTemplate.opsForStream().reverseRange(NotificationMessage.class, key, Range.unbounded(), lt);
	}

	public Mono<String> getUserLastNotificationViewID(String key) {

		return redisTemplate.opsForHash().get("search:" + key, "latestNotificationID").map(x -> {
			String result = (String) x;
			return result;
		}).doOnError((e) -> {
		});
	}

	public Mono<HashMap<String, Long>> getLastestMemoryIdTime(String accountName) {
		return redisTemplate.opsForHash().multiGet(getUserHsetName(accountName), userMemoryCacheKeys).map((value) -> {
			HashMap<String, Long> lastMemoryDetails = new HashMap<>();
			if (value.get(0) == null || value.get(1) == null) {
				return staticLastMemoryId;
			}
			lastMemoryDetails.put("memoryId", Long.parseLong((String) value.get(0)));
			lastMemoryDetails.put("last-memory-second-epoch", Long.parseLong((String) value.get(1)));

			return lastMemoryDetails;
		}).defaultIfEmpty(staticLastMemoryId);
	}

	public Mono<Boolean> setLatestMemoryIdForUser(String accountName, Map<Object, Object> values) {

		// redisTemplate.opsForHash(null);

		RedisSerializationContext<String, Integer> serializationContext = RedisSerializationContext
				.<String, Integer>newSerializationContext(new StringRedisSerializer()).key(new StringRedisSerializer())
				.hashValue(new GenericJackson2JsonRedisSerializer()).build();

		return redisTemplate.opsForHash(serializationContext).putAll(getUserHsetName(accountName), values);
	}

	public static String getUserHsetName(String accountName) {
		return "search:" + accountName;
	}

	public Mono<Boolean> uploadStoryToUserSet(Story story) {

		/*
		 * Upload story to user's sorted set Upload story id as key (TTL 25 hrs) with
		 * value as set of users who viewed the stories.
		 */

		String accountStoriesKey = story.getAccountname() + ":stories";
		String storyKey = story.getAccountname() + ":story:" + story.getStoryid();
		List<String> keys = List.of(accountStoriesKey, storyKey);

		String arg1 = story.getStoryid() + "";
		String arg2 = story.getStime().getEpochSecond() + "";
		String arg3 = story.getAccountname();
		List args = List.of(arg1, arg2, arg3);

		return redisTemplate.execute(RedisScript.of(storyUploadAndViewsScript), keys, args).then(Mono.just(true));
	}

	public Mono<Boolean> fanOutStoryToFollower(String accountName, Story story) {
		return redisTemplate.opsForZSet().add(accountName + ":unseen-stories", story.getAccountname(),
				story.getStime().getEpochSecond());
	}

	public Mono<Tuple2<Map<String, FollowerStory>, Map<String, FollowerStory>>> getUserStorySet(String accountName,
			long instant1) {
		return (getUserNewStorySet(accountName, instant1 - 86400, instant1))
				.zipWith(
						redisTemplate.opsForZSet()
								.rangeByScoreWithScores(accountName + ":seen-stories",
										Range.leftOpen((double) (instant1 - 86400), (double) (instant1)))
								.collectMap((key) -> {
									return key.getValue();
								}, (value) -> {
									FollowerStory fs = new FollowerStory();
									fs.setViewedAll(false);
									fs.setScore(value.getScore());
									return fs;
								}));
	}

	public Mono<Map<String, FollowerStory>> getUserNewStorySet(String accountName, long instant1, long instant2) {
		return redisTemplate.opsForZSet().rangeByScoreWithScores(accountName + ":unseen-stories",
				Range.leftOpen((double) instant1, (double) instant2)).doOnNext((c) -> {
				}).collectMap((key) -> {
					return key.getValue();
				}, (value) -> {
					FollowerStory fs = new FollowerStory();
					fs.setViewedAll(false);
					fs.setScore(value.getScore());
					return fs;
				});
	}

	public Flux<Integer> getStoriesOfSpecificUser(String accountName, long instant1, long instant2) {
		return redisTemplate.opsForZSet()
				.rangeByScore(accountName, Range.leftOpen((double) instant1, (double) instant2))
				.map(v -> Integer.parseInt(v));
	}

	public Mono<Integer> getUnseenCount(String vAccountName, List<Story> stories) {

		List<String> keys = stories.stream().map(k -> {
			return k.getAccountname() + ":story:" + k.getStoryid();
		}).toList();

		List args = List.of(vAccountName, keys.size() + "");

		return (redisTemplate.execute(RedisScript.of(checkUnseenStoriesCountScript), keys, args).single()
				.map(v -> (int) v));
	}

	public Mono<Set<Integer>> getUnseenStories(String vAccountName, List<Story> stories) {

		List<String> keys = stories.stream().map(k -> {
			return k.getAccountname() + ":story:" + k.getStoryid();
		}).toList();

		List args = List.of(vAccountName, keys.size() + "");

		return redisTemplate.execute(RedisScript.of(getUnseenStories), keys, args)
				.map(f -> stories.get(Integer.parseInt((String) f) - 1).getStoryid()).collect(Collectors.toSet());
	}

	public Mono<List<String>> getStoryViewersAccounts(String key) {
		return redisTemplate.opsForSet().members(key).collectList();
	}

	public Mono<Integer> addToUserNotificationStream(CommentActivityQuery caq) {
		NotificationMessage nm = NotificationMessage.getNotification(caq.getAccountname(), caq.getOnpostid(),
				Instant.now().getEpochSecond(), 1);
		ObjectRecord<String, NotificationMessage> record = StreamRecords.newRecord()
				.in(caq.getPostCreatorName() + ":notifications").ofObject(nm);
		return redisTemplate.opsForStream().add(record).map((v) -> 5);
	}

	public Mono<Integer> addToUserNotificationStream(LikeActivityQuery laq) {
		NotificationMessage nm = NotificationMessage.getNotification(laq.getAccountname(), laq.getOnpostid(),
				Instant.now().getEpochSecond(), 0);
		ObjectRecord<String, NotificationMessage> record = StreamRecords.newRecord()
				.in(laq.getPostCreatorName() + ":notifications").ofObject(nm);
		return redisTemplate.opsForStream().add(record).map((v) -> {
			return 5;
		});

	}

	public Mono<Integer> addToUserNotificationStream(Map<String, String> fd) {
		NotificationMessage nm = NotificationMessage.getNotification(fd.get("follower"), null,
				Instant.now().getEpochSecond(), 2);
		ObjectRecord<String, NotificationMessage> record = StreamRecords.newRecord()
				.in(fd.get("followee") + ":notifications").ofObject(nm);

		return redisTemplate.opsForStream().add(record).map((v) -> 5);

	}

	public void updateUserLatestViewedNotification(LatestNotificationViewQuery lnvq) {
		redisTemplate.opsForHash()
				.put("search:" + lnvq.getAccountName(), "latestNotificationID", lnvq.getNotificationId())
				.subscribeOn(Schedulers.boundedElastic()).subscribe();
	}

	public Flux<ObjectRecord<String, NotificationMessage>> fetchOldNotificationsForUser(String accountName,
			String lastFetchID) {
		return redisTemplate.opsForStream().reverseRange(NotificationMessage.class, accountName + ":notifications",
				Range.rightUnbounded(Bound.exclusive(lastFetchID)));
	}

	////////////////////////////////// Deprecated

	@Deprecated
	public Mono<Boolean> checkStoryViewe(String accountname, Story story) {
		return redisTemplate.opsForZSet().score(accountname + ":seen-stories", story.getStoryid()).map(value -> {
			if (value != null)
				return true;
			return false;
		});
	}

	////////////////////////////////// Testing

	public Mono<HashSet<Integer>> getUnseenStoriesTest(String vAccountName, List<String> keys) {

		List args = List.of(vAccountName, keys.size() + "");

		return redisTemplate.execute(RedisScript.of(getUnseenStories), keys, args)
				.map(f -> Integer.parseInt((String) f)).collectList().map(list -> {
					HashSet<Integer> set = new HashSet<>();
					set.addAll(list);
					return set;
				}).subscribeOn(Schedulers.boundedElastic());
	}

	public Mono<Integer> addtostreamNf() {
		NotificationMessage nm = NotificationMessage.defaultNotificationMessage();
		ObjectRecord<String, NotificationMessage> record = StreamRecords.newRecord().in("dxtk:notifications")
				.ofObject(nm);
		return redisTemplate.opsForStream().add(record).map((v) -> {

			return 10;
		});
	}

	@Deprecated
	public Mono<UserdetailsMessage> getUserMetaDatas(String accountName) {

		return redisTemplate.opsForHash().multiGet("search:" + accountName, userDetailsCacheKeys).map((u) -> {
			UserdetailsMessage user = new UserdetailsMessage();
			user.setAccountname((String) u.get(0));
			user.setPostsCount(Integer.parseInt((String) u.get(6)));
			user.setFollowerCount(Integer.parseInt((String) u.get(7)));
			user.setFollowingCount(Integer.parseInt((String) u.get(8)));
			return user;
		});
	}
}
