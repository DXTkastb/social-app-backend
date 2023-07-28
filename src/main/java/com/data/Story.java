package com.data;

import java.time.Instant;
import org.springframework.data.annotation.Id;

import lombok.Data;

@Data
public class Story {

	@Id
	Integer storyid;

	/*
	 * image file name is fetched from user device app
	 * image file name convention: accountname:instant_in_seconds.extension 
	 */
	
	String imgurl;

	String accountname;

	Instant stime;
	
	Long ismemory;

	private static final Story ERROR_STORY;

	static {
		ERROR_STORY = new Story();
		ERROR_STORY.setStoryid(-1);
		ERROR_STORY.setImgurl("NILL");
		ERROR_STORY.setAccountname("");
		ERROR_STORY.setStime(null);
	}

	public static Story errorStory() {
		return ERROR_STORY;
	}
	public static Story getRandomStory () {
		Story s = new Story();
		s.setAccountname("dxtk");
		s.setStoryid(87567865);
		return s;
	}

}
