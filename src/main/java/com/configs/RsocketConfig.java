package com.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.cbor.Jackson2CborDecoder;
import org.springframework.http.codec.cbor.Jackson2CborEncoder;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.*;
import org.springframework.util.MimeTypeUtils;

import com.headers.CustomHeaders;

@Configuration
public class RsocketConfig {
	@Bean
	public RSocketStrategies rsocketStrategies() {
		return RSocketStrategies.builder()
				.decoder(new DataBufferDecoder())
				.decoders(decoders -> decoders.add(new Jackson2CborDecoder()))
				.encoders(encoders -> encoders.add(new Jackson2CborEncoder()))
				.metadataExtractorRegistry(registry -> {
					registry.metadataToExtract(MimeTypeUtils.parseMimeType(CustomHeaders.HEADER_POST_ACCOUNTNAME_MIME),
							String.class, CustomHeaders.HEADER_POST_ACCOUTNAME_KEY);
					registry.metadataToExtract(MimeTypeUtils.parseMimeType(CustomHeaders.HEADER_POST_CAPTION_MIME),
							String.class, CustomHeaders.HEADER_POST_CAPTION_KEY);
					registry.metadataToExtract(MimeTypeUtils.parseMimeType(CustomHeaders.MIME_FILE_NAME), String.class,
							CustomHeaders.FILE_NAME_KEY);
					registry.metadataToExtract(MimeTypeUtils.parseMimeType(CustomHeaders.HEADER_STORY_ISMEMORY_MIME),
							String.class, CustomHeaders.HEADER_STORY_ISMEMORY_KEY);
					registry.metadataToExtract(MimeTypeUtils.parseMimeType(CustomHeaders.MIME_FILE_EXTENSION),
							String.class, CustomHeaders.FILE_EXTN_KEY);
				}).build();
	}
}
