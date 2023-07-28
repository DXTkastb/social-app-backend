package com.storyservices;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.data.Story;
import com.datalayer.RDBDaoLayer;
import com.datalayer.RedisLayer;

import reactor.core.publisher.Mono;

@Service
public class StoryFetchService {

	@Autowired
	RedisLayer redislayer;

	@Autowired
	RDBDaoLayer daolayer;

	private static PriorityQueue<Story> getQueue() {
		return new PriorityQueue<Story>(

				(a, b) -> {
					long diff = b.getStime().getEpochSecond() - a.getStime().getEpochSecond();
					if (diff > 0l)
						return 1;
					if (diff == 0)
						return 0;
					return -1;
				}
		);
	}

	public Mono<List<Map<String, List<Story>>>> getStoriesForUser(String accountname, Instant instant) {

		Map<String, PriorityQueue<Story>> unseen_stories = new LinkedHashMap<>();
		Map<String, PriorityQueue<Story>> seen_stories = new LinkedHashMap<>();

		Long milliseconds = instant.toEpochMilli();

		return redislayer.getStoriesForUser(accountname + ":unseen-stories", milliseconds).collectList()
				.zipWith(redislayer.getStoriesForUser(accountname + ":seen-stories", milliseconds).collectList())
				.flatMap((value) -> {

					return

				daolayer.getStoriesFlux(value.getT1()).collectList()
						.zipWith(daolayer.getStoriesFlux(value.getT2()).collectList()).map((ss) -> {

							Map<String, PriorityQueue<Story>> unseen_stories2 = new LinkedHashMap<>();
							Map<String, PriorityQueue<Story>> seen_stories2 = new LinkedHashMap<>();

							List<Story> usn = ss.getT1();
							List<Story> sn = ss.getT2();

							usn.forEach((story) -> {
								if (!(unseen_stories2.containsKey(story.getAccountname()))) {

									unseen_stories2.put(story.getAccountname(), getQueue());
								}

								unseen_stories2.get(story.getAccountname()).add(story);

							});

							sn.forEach((story) -> {
								if ((unseen_stories2.containsKey(story.getAccountname()))) {

									unseen_stories2.get(story.getAccountname()).add(story);
									return;

								} else if (!(seen_stories.containsKey(story.getAccountname()))) {

									seen_stories2.put(story.getAccountname(), getQueue());

								}

								seen_stories2.get(story.getAccountname()).add(story);
							});

							List<Map<String, List<Story>>> result = (processAllStories(unseen_stories2, seen_stories2));

							return result;
						});

				});

	}

	private static List<Map<String, List<Story>>> processAllStories(Map<String, PriorityQueue<Story>> stories1,
			Map<String, PriorityQueue<Story>> stories2) {

		Map<String, List<Story>> result1 = new LinkedHashMap<>();
		Map<String, List<Story>> result2 = new LinkedHashMap<>();

		List<Map<String, List<Story>>> stories_with_timelines = new ArrayList<>();
		stories_with_timelines.add(result1);
		stories_with_timelines.add(result2);

		stories1.forEach((k, v) -> {	
			result1.put(k, fromPriorityQueue(v));	
		});
		stories2.forEach((k, v) -> {
			result2.put(k, fromPriorityQueue(v));
		});

		return stories_with_timelines;

	}
	
	private static ArrayList<Story> fromPriorityQueue(PriorityQueue<Story> queue){
		ArrayList<Story> story_array = new ArrayList<>();
		while (queue.size() != 0) 
			story_array.add(queue.poll());
		return story_array;
	}

	public Mono<Map<String, List<Story>>> getNewStories(String accountname, Long instant,
			Set<Integer> latestStoriesID) {
		Map<String, List<Story>> unseen_stories = new LinkedHashMap<>();
		return redislayer.getNewStoriesIds(accountname, instant).filter(t -> !latestStoriesID.contains(t)).collectList()
				.flatMap(value -> {

					return daolayer.getStoriesFlux(value).doOnNext((s) -> {

						if (!(unseen_stories.containsKey(s.getAccountname()))) {
							unseen_stories.put(s.getAccountname(), new LinkedList<Story>());
						}

						unseen_stories.get(s.getAccountname()).add(s);

					}).then(Mono.just(unseen_stories));
				});
	}

//	public Mono<List<String>> getStoryViews(Story story) {
//		return daolayer.getFollowers(story.getAccountname()).flatMap((value) -> {
//
//			return redislayer.checkStoryView(value, story).map((s) -> {
//				return (s) ? value : null;
//			});
//
//		}).filter(x -> (x != null)).collectList();
//	}
}
