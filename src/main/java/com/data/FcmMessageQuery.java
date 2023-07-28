package com.data;

import java.util.*;

import lombok.Data;

@Data
public class FcmMessageQuery {
	FcmMessage message = new FcmMessage();

	public FcmMessageQuery() {
	}

	public FcmMessageQuery(String token, String body) {
		FcmNotification fn = new FcmNotification(body);
		this.message = new FcmMessage(token, fn);
	}
}

@Data
class FcmMessage {
	String token;
	FcmNotification notification;
	AndroidConfig android = new AndroidConfig();

	FcmMessage() {
	};

	FcmMessage(String token, FcmNotification fn) {
		this.notification = fn;
		this.token = token;
	}
}

@Data
class FcmNotification {
	String body;
	String title;

	FcmNotification(String body) {
		this.title = "Socio";
		this.body = body;
	}

	FcmNotification() {
	}
}

@Data
class AndroidConfig {
	String priority = "high";
	Map<String,String> data = new HashMap<>();
	AndroidConfig(){
		data.put("type", "notification");
	}
	
}
