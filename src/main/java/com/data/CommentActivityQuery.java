package com.data;

import lombok.Data;

@Data
public class CommentActivityQuery {
	Integer onpostid;
	
	String text;
	
	String accountname;
	
	String postCreatorName;
}
