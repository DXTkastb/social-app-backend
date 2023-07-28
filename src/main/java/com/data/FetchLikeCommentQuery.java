package com.data;

import lombok.Data;

@Data
public class FetchLikeCommentQuery {
	Integer postid;
	Integer lastFetchId;
}
