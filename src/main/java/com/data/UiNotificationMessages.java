package com.data;

import java.util.List;

import lombok.Data;

@Data
public class UiNotificationMessages {
	List<UiNotificationMessage> uiMessages;
	Integer newNotificationCount;
}
