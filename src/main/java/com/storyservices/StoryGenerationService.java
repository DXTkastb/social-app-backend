package com.storyservices;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.data.Story;
import com.datalayer.RDBDaoLayer;
import com.datalayer.RedisLayer;

import reactor.core.publisher.Mono;

@Service
public class StoryGenerationService {
	@Autowired
	RDBDaoLayer daolayer;

	@Autowired
	RedisLayer redislayer;

	public Mono<Void> generateStories(Story story) {
		return daolayer.getFollowers(story.getAccountname()).concatWithValues(story.getAccountname())
				.flatMap((follower) -> {
					return redislayer.pushStoryToTimeline(follower, story.getStoryid(),
							story.getStime().toEpochMilli());
				}).then();
	}

	public Mono<Void> generateStoriesNewApi(Story story) {
		return daolayer.getFollowers(story.getAccountname()).flatMap((followerAccountName) -> {
			return redislayer.fanOutStoryToFollower(followerAccountName, story);
		}).then();
	}
}
