package com.data;
import java.util.List;
import java.util.Set;

import lombok.Data;

@Data
public class FetchStoryQueryMessage {
	List<Story> stories;
	Set<Integer> unseenStories;
}
