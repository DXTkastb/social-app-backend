package com.feedservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.data.Post;
import com.datalayer.RDBDaoLayer;
import com.datalayer.RedisLayer;

import reactor.core.publisher.Mono;

/*
* Improvement : Create a microservice which is fed post via rabbitmq over rsocket
* 				 Microservice will generate posts for users and store it on Redis
* 				 Microservice will also have an endpoints for posts consumptions
*/

@Service
public class FeedGenerationService {
	
	@Autowired
	RDBDaoLayer daolayer;
	
	@Autowired
	RedisLayer redislayer;
	
	public Mono<Void> generateFeeds(Post post){
		return daolayer.getFollowers(post.getAccountname()).concatWithValues(post.getAccountname()).flatMap((follower)->{
			return redislayer.pushDataToTimeline(follower,post.getPostid());
		}).then();
	}
}
