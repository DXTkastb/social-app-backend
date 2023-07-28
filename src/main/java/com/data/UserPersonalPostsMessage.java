package com.data;

import java.util.List;

import lombok.Data;

@Data
public class UserPersonalPostsMessage {

	List<Post> userPersonalPosts;
	public UserPersonalPostsMessage(List<Post> posts){
		userPersonalPosts = posts;
	}
}
