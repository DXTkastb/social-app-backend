package com.data;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class StoryFetchMessage {
	List<Map<String, List<Story>>> stories;
	Map<String, List<Story>> newStories;
}
