package com.fcmservice;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.data.*;
import com.datalayer.RDBDaoLayer;
import com.google.auth.oauth2.GoogleCredentials;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class FcmNotificationService {
	private static final String MESSAGING_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
	private static final String[] SCOPES = { MESSAGING_SCOPE };

	@Autowired
	RDBDaoLayer daolayer;

	@Value("classpath:gkeys/socioGkey.json")
	private Resource socio_gkey;

	WebClient webclient = WebClient.builder()
			.baseUrl("https://fcm.googleapis.com/v1/projects/socio-be71a/messages:send").build();

	public void publishPushNotifications(String accountName, String notificationMessage) {
		daolayer.getUserDetails(accountName).flatMap(user -> {
			if (user.getFkey().isBlank()) {
				return Mono.just("");
			}
			FcmMessageQuery fmq = new FcmMessageQuery(user.getFkey(), notificationMessage);
			return webclient.post().contentType(MediaType.APPLICATION_JSON).header("Authorization", getToken())
					.bodyValue(fmq).exchangeToMono(response -> {
						if (response.statusCode().is4xxClientError()) {
							return Mono.just(user.getFkey());
						}
						return Mono.just("");
					});
		}).flatMap(fkey -> {
			if (!(fkey.isBlank())) {
				daolayer.invalidateFkey(accountName, fkey).subscribe();
			}
			return Mono.just(0);
		}).subscribeOn(Schedulers.boundedElastic()).subscribe();

	}

	private String getAccessToken() throws IOException {
		GoogleCredentials googleCredentials = GoogleCredentials.fromStream(new FileInputStream(socio_gkey.getFile()))
				.createScoped(Arrays.asList(SCOPES));
		googleCredentials.refresh();
		googleCredentials.refreshAccessToken();
		return "Bearer " + googleCredentials.getAccessToken().getTokenValue();
	}

	private String getToken() {
		String token = "";
		try {
			token = getAccessToken();
		} catch (IOException e) {
		}
		return token;
	}

}
