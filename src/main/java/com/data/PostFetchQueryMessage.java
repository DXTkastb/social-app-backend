package com.data;

import java.util.List;

import lombok.Data;

@Data
public class PostFetchQueryMessage {
	List<PostMessage> userPosts;
	Long maxIndex;
	Long minIndex;
}
