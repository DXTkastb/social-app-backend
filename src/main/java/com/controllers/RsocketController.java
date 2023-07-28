package com.controllers;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Controller;

import com.data.*;
import com.datalayer.RDBDaoLayer;
import com.datalayer.RedisLayer;
import com.feedservice.FeedFetchService;
import com.feedservice.FeedGenerationService;
import com.headers.CustomHeaders;
import com.notificationservice.NotificationService;
import com.redis.lettucemod.search.SearchResults;
import com.storyservices.StoryFetchNewService;
import com.storyservices.StoryFetchService;
import com.storyservices.StoryGenerationService;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Controller
public class RsocketController {

	@Autowired
	RDBDaoLayer daolayer;

	@Autowired
	RedisLayer redislayer;

	@Autowired
	FeedGenerationService feddgenerationservice;

	@Autowired
	FeedFetchService feedfetchservice;

	@Autowired
	NotificationService notificationService;

	@Autowired
	StoryGenerationService storygenerationservice;

	@Autowired
	StoryFetchService storyfetchservice;

	@Autowired
	StoryFetchNewService storyFetchNewService;


	@Value("classpath:input/apple.jpg")
	private Resource resource;

	/*
	 * Improvement : Use Multipart(FORM) for addStory
	 */

	@MessageMapping(value = "rsocket.upload.story.newapi")
	public Mono<Story> addStoryNewApi(@Headers Map<String, String> headers, @Payload Mono<DataBuffer> storyFile)
			throws IOException {

		String fileName = headers.get(CustomHeaders.FILE_NAME_KEY);
		String fileExtn = headers.get(CustomHeaders.FILE_EXTN_KEY);
		String filePathName = fileName + "." + fileExtn;

		String accountName = headers.get(CustomHeaders.HEADER_POST_ACCOUTNAME_KEY);

		boolean isMemory = headers.get(CustomHeaders.HEADER_STORY_ISMEMORY_KEY).equalsIgnoreCase("true");

		Instant currentInstant = Instant.now();

		Story story = new Story();
		story.setAccountname(accountName);
		story.setImgurl(filePathName);
		story.setStime(currentInstant);

		Mono<String> uploadImage = daolayer.uploadStoryImage(storyFile, filePathName);

		return uploadImage.flatMap(x -> {
			return daolayer.postStory(story);
		});
	}

	/*
	 * Improvement : Use Multipart(FORM) for addPost
	 */

	@MessageMapping(value = "rsocket.upload.post")
	public Mono<Post> addPost(@Headers Map<String, String> headers, @Payload Mono<DataBuffer> postFile)
			throws IOException {

		String fileName = headers.get(CustomHeaders.FILE_NAME_KEY);
		String fileExtn = headers.get(CustomHeaders.FILE_EXTN_KEY);
		String accountName = headers.get(CustomHeaders.HEADER_POST_ACCOUTNAME_KEY);
		String caption = headers.get(CustomHeaders.HEADER_POST_CAPTION_KEY);

		String filePathName = fileName + "." + fileExtn;

		Post post = new Post();
		post.setAccountname(accountName);
		post.setImgurl(filePathName);
		post.setCaption(caption);
		post.setCtime(Instant.now());

		return daolayer.uploadPostImage(postFile, filePathName)
				.then(daolayer.createPost(post).doOnNext((generated_post) -> {
					try {

						/*
						 * Calling FeedService asynchronously
						 */

						feddgenerationservice.generateFeeds(generated_post).onErrorComplete()
								.subscribeOn(Schedulers.boundedElastic()).subscribe();
					} catch (Exception exception) {
					}
				}).zipWhen(p -> {

					return redislayer.incrementPostCount(accountName, true);

				})).map(p -> p.getT1()).onErrorReturn(Post.errorPost());
	}

	@MessageMapping(value = "rsocket.fetch.user.personal.posts")
	public Mono<UserPersonalPostsMessage> fetchUserPersonalPost(@Payload String accountName) {
		return daolayer.getUserPersonalPostsList(accountName).map((val) -> new UserPersonalPostsMessage(val));
	}

	@MessageMapping(value = "rsocket.fetch.user.metadata")
	public Mono<UserdetailsMessage> fetchUserMetaData(@Payload String accountName) {
		return redislayer.getUserDetailsFromRedisCache(accountName).doOnNext(x -> {
		});
	}

	@MessageMapping(value = "rsocket.fetch.posts")
	public Mono<PostFetchQueryMessage> fetchPosts(@Payload PostFetchQuery pfq) {
		return feedfetchservice.getPostFeeds(pfq).subscribeOn(Schedulers.boundedElastic());
	}

	@MessageMapping(value = "rsocket.post.delete")
	public Mono<Integer> deletePost(@Payload PostDeleteQuery postDeleteQuery) {
		return daolayer.deletePost(postDeleteQuery.getPostId()).zipWhen(t -> {
			return redislayer.incrementPostCount(postDeleteQuery.getAccountName(), false);
		}).map(x -> 201).onErrorReturn(500);
	}

	@MessageMapping(value = "rsocket.fetch.memories")
	public Mono<List<Story>> getUserMemories(@Payload String accountname) {
		return daolayer.getUserMemoriesList(accountname);
	}

	@Deprecated
	@MessageMapping(value = "rsocket.fetch.user.story.newapi")
	public Mono<FetchStoryQueryMessage> getStoriesSet(@Payload FetchStoryQuery fetchStoryQuery) {
		return storyFetchNewService.getSpecificUserStory(fetchStoryQuery.getViewerName(),
				fetchStoryQuery.getAccountName(), fetchStoryQuery.getInstant1(), fetchStoryQuery.getInstant2());
	}

	@MessageMapping(value = "rsocket.fetch.user.story.exp")
	public Mono<List<Story>> getStoriesSet2(@Payload String accountname) {
		return storyFetchNewService.getSpecificUserStoryNew(accountname);
	}

	@MessageMapping(value = "rsocket.fetch.user.memory.stories")
	public Mono<FetchStoryQueryMessage> getMemoryStories(@Payload Integer memoryId) {
		return daolayer.getStoryFromMemoryId((long) memoryId).map(x -> {
			FetchStoryQueryMessage fsqm = new FetchStoryQueryMessage();
			fsqm.setStories(x);
			return fsqm;
		});
	}

	@Deprecated
	@MessageMapping(value = "rsocket.fetch.user.storyset.newapi")
	public Mono<StorySetQueryMessage> getStoriesSet(@Payload StorySetQuery storySetQuery) {
		return storyFetchNewService.getStoriesSetForUser(storySetQuery.getAccountName(), storySetQuery.getInstant1());
	}

	@Deprecated
	@MessageMapping(value = "rsocket.fetch.user.newstoryset.newapi")
	public Mono<StorySetQueryMessage> getNewStoriesSet(@Payload StorySetQuery storySetQuery) {
		return storyFetchNewService.getNewStoriesSetForUser(storySetQuery.getAccountName(), storySetQuery.getInstant1(),
				storySetQuery.getInstant2());
	}

	@MessageMapping(value = "rsocket.fetch.user.follower.stories")
	public Mono<List<String>> getUserStorySet(@Payload String accountname) {
		return daolayer.getFollowersForStories(accountname);
	}

	@MessageMapping(value = "rsocket.fetch.story.views.newapi")
	public Mono<StoryViewsQueryMessage> getStoryViews(@Payload StoryViewsQuery svq) {
		return storyFetchNewService.getViewers(svq.getAccontName(), svq.getStoryID());
	}

	@MessageMapping(value = "rsocket.search.user")
	public Mono<SearchResults<String, String>> searchUser(@Payload String accountName) {
		return redislayer.getsearchUsers(accountName);
	}

	@MessageMapping(value = "rsocket.get.external.user")
	public Mono<UserdetailsMessage> getExternalUserDetails(@Payload ExternalUserQuery searchQuery) {
		return redislayer.getUserDetailsFromRedisCache(searchQuery.getSearchedAccountName())
				.zipWith(daolayer.checkIfUserFollowed(searchQuery)).map(val -> {
					UserdetailsMessage udm = val.getT1();
					udm.setFollow(val.getT2());
					return udm;
				});
	}

	@MessageMapping(value = "rsocket.get.user.post")
	public Mono<PostMessage> getPostData(@Payload Integer upq) {
		return daolayer.getUserPostsFromDb(upq);
	}

	/*
	 * Post Comment and Like and Follow
	 * 
	 */

	@MessageMapping(value = "rsocket.follow.user")
	public Mono<Integer> followUser(@Payload Map<String, String> relation) {
		Followers data = new Followers();
		data.setFollowee(relation.get("followee"));
		data.setFollower(relation.get("follower"));
		return daolayer.followUser(data).doOnNext((d) -> {
			notificationService.createNotificationActivity(relation);
		}).zipWhen(t -> {
			return redislayer.incrementFollowingCount2(relation.get("follower"), relation.get("followee"), 1);
		}).map((value) -> {
			return 200;
		});
	}

	@MessageMapping(value = "rsocket.unfollow.user")
	public Mono<Integer> unfollowUser(@Payload Map<String, String> relation) {
		Followers data = new Followers();
		data.setFollowee(relation.get("followee"));
		data.setFollower(relation.get("follower"));
		return daolayer.unfollowUser(data).doOnNext((d) -> {
		}).zipWhen(t -> {
			return redislayer.incrementFollowingCount2(relation.get("follower"), relation.get("followee"), -1);
		}).map((value) -> {
			return 200;
		}).onErrorReturn(500);
	}

	@MessageMapping(value = "rsocket.post.comment")
	public Mono<Integer> addComment(@Payload CommentActivityQuery caq) {

		Comment comment = new Comment();
		comment.setAccountname(caq.getAccountname());
		comment.setOnpostid(caq.getOnpostid());
		comment.setText(caq.getText());

		return daolayer.addComment(comment).doOnNext((c) -> {
			notificationService.createNotificationActivity(caq);
		}).then(Mono.just(201)).onErrorReturn(500);
	}

	@MessageMapping(value = "rsocket.post.like")
	public void postLike(@Payload LikeActivityQuery laq) {

		Likes like = new Likes();
		like.setAccountname(laq.getAccountname());
		like.setOnpostid(laq.getOnpostid());

		daolayer.postLike(like).doOnNext((l) -> {
			notificationService.createNotificationActivity(laq);
		}).subscribe();
	}

	@MessageMapping(value = "rsocket.post.dislike")
	public void postDislike(@Payload LikeActivityQuery laq) {
		Likes like = new Likes();
		like.setAccountname(laq.getAccountname());
		like.setOnpostid(laq.getOnpostid());
		daolayer.postDislike(like).subscribe();
	}

	@MessageMapping(value = "rsocket.fetch.comments")
	public Mono<List<Comment>> fetchComments(@Payload FetchLikeCommentQuery fetchQuery) {
		return daolayer.fetchComments(fetchQuery);
	}

	@MessageMapping(value = "rsocket.fetch.likes")
	public Mono<List<Likes>> fetchLikes(@Payload FetchLikeCommentQuery fetchQuery) {
		return daolayer.fetchLikes(fetchQuery);
	}

	@MessageMapping(value = "rsocket.fetch.followers")
	public Mono<List<String>> fetchFollowers(@Payload FetchFollowersQuery ffq) {
		return daolayer.getFollowersAccountNames(ffq.getAccountName(), ffq.getLastAccountName());
	}

	@MessageMapping(value = "rsocket.fetch.following")
	public Mono<List<String>> fetchFollowing(@Payload FetchFollowersQuery ffq) {
		return daolayer.getFolloweeAccountNames(ffq.getAccountName(), ffq.getLastAccountName());
	}

	/*
	 * Notifications
	 */

	@MessageMapping(value = "rsocket.fetch.user.notifications")
	public Mono<UiNotificationMessages> getUserNotifications(@Payload String accountName) {
		return notificationService.fetchUserLatestNotifications(accountName).map(value -> {
			UiNotificationMessages uinm = new UiNotificationMessages();
			uinm.setUiMessages(value.getT1());
			uinm.setNewNotificationCount(value.getT2());
			return uinm;
		});
	}

	@MessageMapping(value = "rsocket.user.subscribe.notifications")
	public void subscribeToNotifications(@Payload StreamQuery streamQuery, RSocketRequester requester) {
		notificationService.subscribeToRedisReactiveStreamNotifications(requester, streamQuery.getAccountName(),
				streamQuery.getOffset());
	}

	@MessageMapping(value = "rsocket.user.latest.notification.mark")
	public void updateNotificationViewFetchMark(@Payload LatestNotificationViewQuery latestNotificationViewQuery) {
		redislayer.updateUserLatestViewedNotification(latestNotificationViewQuery);
	}

	@MessageMapping(value = "rsocket.fetch.user.old.notifications")
	public Mono<List<UiNotificationMessage>> fetchUserOldNotifications(
			@Payload OldNotificationsQuery oldNotificationsQuery) {
		return notificationService.oldUiNotificationsForUser(oldNotificationsQuery.getAccountName(),
				oldNotificationsQuery.getLastNotificationID());
	}

	/*
	 * _____________________________________________________ D
	 * _____________________________________________________ E
	 * _____________________________________________________ P
	 * _____________________________________________________ R
	 * _____________________________________________________ E
	 * _____________________________________________________ C
	 * _____________________________________________________ A
	 * _____________________________________________________ T
	 * _____________________________________________________ E
	 * _____________________________________________________ D
	 */

	@Deprecated
	@MessageMapping(value = "rsocket.story.views")
	public Mono<List<String>> getViews(@Payload Story story) {
		// return storyfetchservice.getStoryViews(story);
		return Mono.just(List.of());
	}

	@Deprecated
	@MessageMapping(value = "rsocket.upload.story")
	public Mono<Story> addStory(@Headers Map<String, String> headers, @Payload Mono<DataBuffer> storyFile)
			throws IOException {

		String fileName = headers.get(CustomHeaders.FILE_NAME_KEY);
		String fileExtn = headers.get(CustomHeaders.FILE_EXTN_KEY);
		String filePathName = fileName + "." + fileExtn;
		String accountName = headers.get(CustomHeaders.HEADER_POST_ACCOUTNAME_KEY);

		Story story = new Story();
		story.setAccountname(accountName);
		story.setImgurl(filePathName);
		story.setStime(Instant.now());

		return daolayer.uploadStoryImage(storyFile, filePathName)
				.then(daolayer.postStory(story).doOnNext((generated_story) -> {
					try {

						/*
						 * story service
						 */

						storygenerationservice.generateStories(generated_story).onErrorComplete()
								.subscribeOn(Schedulers.boundedElastic()).subscribe();
						;

					} catch (Exception exception) {

					}
				})).onErrorReturn(Story.errorStory());
	}

	@Deprecated
	@MessageMapping(value = "rsocket.fetch.stories")
	public Mono<StoryFetchMessage> getStories(@Payload String accountname) {
		Instant now = Instant.now();
		return storyfetchservice.getStoriesForUser(accountname, now).map((value) -> {
			StoryFetchMessage storyfetchmessage = new StoryFetchMessage();
			storyfetchmessage.setStories(value);
			return storyfetchmessage;
		}).subscribeOn(Schedulers.boundedElastic());
	}

	@Deprecated
	@MessageMapping(value = "rsocket.fetch.new.stories")
	public Mono<StoryFetchMessage> getNewStories(@Payload Set<Integer> latestStoriesID,
			@Header(value = CustomHeaders.HEADER_LAST_STORY_INSTANT) String instant,
			@Header(value = CustomHeaders.HEADER_POST_ACCOUTNAME_KEY) String accountname) {
		Long instantTime = Long.parseLong(instant);
		return storyfetchservice.getNewStories(accountname, instantTime, latestStoriesID).map((value) -> {
			StoryFetchMessage storyfetchmessage = new StoryFetchMessage();
			storyfetchmessage.setNewStories(value);
			return storyfetchmessage;
		});
	}

	@Deprecated
	@MessageMapping(value = "rsocket.create.services")
	public Mono<Integer> createUserServices(@Payload Userdetails userdetails) {

		// redisLayer.createPostTimelineList(userdetails).zipWith
//		(mqlayer.createUserNotificationStream(userdetails)).then(Mono.just(200)).onErrorReturn(500)
//				.subscribeOn(Schedulers.boundedElastic());;
		return Mono.just(10);
	}

	@Deprecated
	@MessageMapping("rsocket.start.notification.services")
	public Mono<Integer> startServices(@Payload Userdetails userdetails,
			@Header(value = "lastOffest") Integer lastOffset, RSocketRequester requester) {

//		if (lastOffset != -1) {
//			Mono.just(1).doOnNext((x) -> {
//				notificationService.pushNotificationsToApp(requester, userdetails, lastOffset + 1);
//			}).subscribeOn(Schedulers.boundedElastic());
//			return Mono.just(-1);
//		}
//
//		return mqlayer.checkQueueLength(userdetails.getAccountname() + "_notif").map(value -> {
//
//			Object offset = null;
//
//			if (value > 200) {
//				offset = Integer.valueOf(value - 200);
//			} else {
//				offset = "first";
//			}
//
//			notificationService.pushNotificationsToApp(requester, userdetails, offset);
//
//			return value;
//		}).subscribeOn(Schedulers.boundedElastic());

		return Mono.just(45);

	}

	@Deprecated
	@MessageMapping(value = "rsocket.upload.story.newapi0")
	public Mono<Story> addStoryNewApi0(@Headers Map<String, String> headers, @Payload Mono<DataBuffer> storyFile)
			throws IOException {

		String fileName = headers.get(CustomHeaders.FILE_NAME_KEY);
		String fileExtn = headers.get(CustomHeaders.FILE_EXTN_KEY);
		String filePathName = fileName + "." + fileExtn;

		String accountName = headers.get(CustomHeaders.HEADER_POST_ACCOUTNAME_KEY);

		boolean isMemory = headers.get(CustomHeaders.HEADER_STORY_ISMEMORY_KEY).equalsIgnoreCase("true");

		Instant currentInstant = Instant.now();

		Story story = new Story();
		story.setAccountname(accountName);
		story.setImgurl(filePathName);
		story.setStime(currentInstant);

		Mono<String> uploadImage = daolayer.uploadStoryImage(storyFile, filePathName);

		Mono<Story> pushStory = daolayer.postStory(story).zipWhen((x) -> {
			return redislayer.uploadStoryToUserSet(x);
		}).doOnNext((generated_story) -> {
			try {
				// This is story service
				storygenerationservice.generateStoriesNewApi(generated_story.getT1()).onErrorComplete()
						.subscribeOn(Schedulers.boundedElastic()).subscribe();
			} catch (Exception exception) {
			}
		}).map((val) -> val.getT1());

		if (isMemory) {
			return uploadImage.then(redislayer.getLastestMemoryIdTime(accountName).flatMap((value) -> {
				Long memoryId = value.get("memoryId");
				Long lastMemoryInstant = value.get("last-memory-second-epoch");

				if ((currentInstant.getEpochSecond() - (long) lastMemoryInstant) <= 86400l) {

					story.setIsmemory(memoryId);
					return pushStory.zipWhen((st) -> {
						Map<Object, Object> map = new HashMap<Object, Object>();
						map.put("last-memory-second-epoch", st.getStime().getEpochSecond());
						return redislayer.setLatestMemoryIdForUser(accountName, map);
					}).map((val) -> val.getT1());

				} else {
					story.setIsmemory(currentInstant.getEpochSecond());
					return pushStory.zipWhen((st) -> {
						Map<Object, Object> map = new HashMap<Object, Object>();
						map.put("memoryid", st.getIsmemory());
						map.put("last-memory-second-epoch", st.getIsmemory());
						return redislayer.setLatestMemoryIdForUser(accountName, map);
					}).map((val) -> val.getT1());
				}
			}));
			// .onErrorReturn(Story.errorStory());

		} else {
			return uploadImage.then(pushStory).onErrorReturn(Story.errorStory());
		}
	}

}
