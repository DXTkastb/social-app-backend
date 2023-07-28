package com.data;

import java.time.Instant;
import org.springframework.data.annotation.Id;

import lombok.Data;

@Data
public class Post {
	@Id
	Integer postid;

	String caption;

	String accountname;

	String imgurl;

	Instant ctime;

	private final static Post ERROR_POST;
	static {
		ERROR_POST = new Post();
		ERROR_POST.setPostid(-1);
		ERROR_POST.setAccountname("NILL");
		ERROR_POST.setCaption("");
		ERROR_POST.setImgurl("NILL");
	}

	public static Post errorPost() {

		return ERROR_POST;
	}

}
