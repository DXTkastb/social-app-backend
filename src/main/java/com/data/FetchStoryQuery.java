package com.data;

import lombok.Data;

@Data
public class FetchStoryQuery {
	String accountName;
	Long instant1;
	Long instant2;
	String viewerName;
}
