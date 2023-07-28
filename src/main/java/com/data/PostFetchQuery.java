package com.data;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class PostFetchQuery {
	String accountname;
	List<Integer> postlist;
	Long maxIndex;
	Long minIndex;
	
	private static final PostFetchQuery  pid = new PostFetchQuery();
	
	static {
		pid.setAccountname("default");pid.setMaxIndex(-2l);pid.setMinIndex(-2l);
		 pid.setPostlist(new ArrayList<>());
	}
	
	
	public static PostFetchQuery getDefault() {
		return pid;
	}
	
}
