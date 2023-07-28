package com.data;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import lombok.*;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class NotificationMessage {
	String notificationCreatorAccountName;
	/*
	 * type => 0: liked 1: commented 2: followed
	 */
	Integer type;
	Integer postid;
	Long instant;
	
	public static NotificationMessage getNotification(String acname,Integer postid,Long instant,Integer type) {
		NotificationMessage nm = new NotificationMessage();
		nm.setNotificationCreatorAccountName(acname);
		nm.setPostid(postid);
		nm.setInstant(instant);
		nm.setType(type);
		return nm;
	}
	
	public static NotificationMessage defaultNotificationMessage() {
		return getNotification("Mark",256546,1678172345l,1);
	}
	
}
