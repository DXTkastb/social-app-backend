package com.storyservices;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.data.FetchStoryQueryMessage;
import com.data.FollowerStory;
import com.data.Story;
import com.data.StorySetQueryMessage;
import com.data.StoryViewsQueryMessage;
import com.data.UserStorySet;
import com.datalayer.RDBDaoLayer;
import com.datalayer.RedisLayer;

import reactor.core.publisher.Mono;

@Service
public class StoryFetchNewService {
	@Autowired
	RedisLayer redislayer;

	@Autowired
	RDBDaoLayer daolayer;

	public Mono<StorySetQueryMessage> getStoriesSetForUser(String accountName, long instant1) {
		return redislayer.getUserStorySet(accountName, instant1).map((tuple) -> {

			Map<String, FollowerStory> map1 = tuple.getT1();
			Map<String, FollowerStory> map2 = tuple.getT2();

			for (Map.Entry<String, FollowerStory> entry : map2.entrySet()) {
				if (!(map1.containsKey(entry.getKey())
						&& (entry.getValue().getScore() < map1.get(entry.getKey()).getScore()))) {
					entry.getValue().setViewedAll(false);
					map1.put(entry.getKey(), entry.getValue());
				}
			}

			return map1;
		}).map((v)->{
			StorySetQueryMessage ssqm = new StorySetQueryMessage();
			ssqm.setData(v);
			return ssqm;
		});
	}

	public Mono<StorySetQueryMessage> getNewStoriesSetForUser(String accountName, long instant1, long instant2) {
		return redislayer.getUserNewStorySet(accountName, instant1, instant2).map((v)->{
			StorySetQueryMessage ssqm = new StorySetQueryMessage();
			ssqm.setData(v);
			return ssqm;
		});
	}
	
	
	@Deprecated
	public Mono<FetchStoryQueryMessage> getSpecificUserStory(String viewerAccountName, String storyAccountName, long instant1, long instant2) {
		Mono<List<Integer>> storyIds = redislayer
				.getStoriesOfSpecificUser(storyAccountName + ":stories", instant1, instant2).doOnNext((c)->{}).collectList();

		Mono<List<Story>> dbData = storyIds.flatMap((val) -> {
			return daolayer.getStoriesOf(val);
		});

		if (storyAccountName.compareTo(viewerAccountName)==0) {
			return dbData.map(val -> {
				FetchStoryQueryMessage fsqm = new FetchStoryQueryMessage();
				fsqm.setStories(val);
				fsqm.setUnseenStories(Set.of());
				return fsqm;
			});
		}

		Mono<FetchStoryQueryMessage> storiesData = dbData.flatMap((val) -> {

			int length = val.size();

			if (length == 0) {
				FetchStoryQueryMessage fsqm = new FetchStoryQueryMessage();
				fsqm.setStories(List.of());
				fsqm.setUnseenStories(Set.of());
				Mono.just(fsqm);
			}

			return Mono.just(val).zipWith(redislayer.getUnseenStories(viewerAccountName, val));

		}).map((f) -> {
			FetchStoryQueryMessage fsqm1 = new FetchStoryQueryMessage();
			fsqm1.setStories(f.getT1());
			fsqm1.setUnseenStories(f.getT2());
			return fsqm1;

		});

		return storiesData;
	}
	
	
	public Mono<List<Story>> getSpecificUserStoryNew(String name) {
		return daolayer.getUserLatestStories(name);
	}
	
	public Mono<StoryViewsQueryMessage> getViewers(String accountName,String storyid){
		return redislayer.getStoryViewersAccounts(accountName+":story:"+storyid).map((v)->{
			StoryViewsQueryMessage svqm = new StoryViewsQueryMessage();
			svqm.setData(v);
			return svqm;
		});
	}
	

}
