package com.data;

import java.util.Map;

import lombok.Data;

@Data
public class StorySetQueryMessage {
	Map<String, FollowerStory> data;
}
