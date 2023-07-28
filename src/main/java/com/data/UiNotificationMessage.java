package com.data;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import lombok.Data;

@Data
public class UiNotificationMessage {
	String accountname;
	/*
	 * type => 0: liked   1: commented    2: followed
	 */
	Integer postid;
	String timeCreated;
	String id;
	String notificationText;
	Integer type;
	public UiNotificationMessage(NotificationMessage notificationMessage, String id) {
		this.id = id;
		this.accountname = notificationMessage.getNotificationCreatorAccountName();
		type = notificationMessage.getType();
		if ( type== 2) {
			notificationText = "started following you!";
		}
		else if (type== 1) {
			notificationText = "commented on your post.";
		}
		else if (type== 0) {
			notificationText = "liked your post.";
		}
		this.postid = notificationMessage.getPostid();
		long diff = Instant.now().getEpochSecond() - notificationMessage.getInstant();

		long days = TimeUnit.SECONDS.toDays(diff);
		if (days >= 356l) {
			this.timeCreated = (days / 356) + "y";
		} else if (days >= 30l) {
			this.timeCreated = (days / 30) + "m";
		} else if (days >= 7l) {
			this.timeCreated = (days / 7) + "w";
		} else if(days>0l)
			this.timeCreated = (days) + "d";
		else  {
			long hours = TimeUnit.MILLISECONDS.toHours(diff);
			if(hours>0l)
			this.timeCreated = (hours) + "hr";
			else this.timeCreated = "few moments ago";
		}

	}
}