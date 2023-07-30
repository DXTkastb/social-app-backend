package com.feedservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.data.PostFetchQueryMessage;
import com.data.PostFetchQuery;
import com.datalayer.RDBDaoLayer;
import com.datalayer.RedisLayer;

import reactor.core.publisher.Mono;

@Service
public class FeedFetchService {

	@Autowired
	RDBDaoLayer daolayer;

	@Autowired
	RedisLayer redislayer;

	public Mono<PostFetchQueryMessage> getPostFeeds(PostFetchQuery pfq) {

		return redislayer
				.fetchUserLatestPostIds(pfq.getAccountname() + ":timeline", pfq.getMaxIndex(), pfq.getMinIndex())
				.flatMap((pids) -> {
						return Mono.just(pids).zipWhen(pd -> daolayer.getUserPostsFromDb(pfq.getAccountname(),pd.getPostlist()).collectList());
				}).map((data) -> {
					PostFetchQueryMessage postsFetchMessage = new PostFetchQueryMessage();
					postsFetchMessage.setMinIndex(data.getT1().getMinIndex());
					postsFetchMessage.setMaxIndex(data.getT1().getMaxIndex());
					postsFetchMessage.setUserPosts(data.getT2());
					return postsFetchMessage;
				}).doOnError((x)->{
					System.out.println("SOME ERROR OCCURRED!!!!!");
				})

		;
	}
}
