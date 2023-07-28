package com.data;

import java.time.Instant;

import org.springframework.data.annotation.Id;

import lombok.Data;

@Data
public class PostMessage {
	@Id
	Long postid;

	String caption;

	String accountname;

	String imgurl;

	Instant ctime;
	
	Long likesCount;
	
	Long liked;
	


}