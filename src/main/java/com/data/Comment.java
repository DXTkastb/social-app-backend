package com.data;

import org.springframework.data.annotation.Id;

import lombok.Data;
import lombok.NonNull;

@Data
public class Comment {
	@Id
	Integer commentid;
	
	Integer onpostid;
	
	String text;
	
	String accountname;
}
