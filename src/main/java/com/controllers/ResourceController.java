package com.controllers;

import java.nio.ByteBuffer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.datalayer.RDBDaoLayer;
import com.datalayer.RedisLayer;
import com.services.TokenService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController("resources")
public class ResourceController {

	@Autowired
	RedisLayer redisLayer;

	@Autowired
	TokenService tokenService;
	
	@Autowired
	RDBDaoLayer daolayer;
	
	
	static final Resource defaultProfileImageResource = new FileSystemResource("/home/kaustubh/Desktop/default_img/default_user.jpg");

	
	@GetMapping(value = "profile-image/{accountname}", produces = MediaType.IMAGE_JPEG_VALUE)
	public Mono<ResponseEntity<DataBuffer>> getStoryImage(@PathVariable String accountname) {
		return redisLayer.getUserProfileUrlCache(accountname).flatMap(str->{
			 return getfiledata("profiles/" + str)
					 .map(value->{
				return new ResponseEntity<>(value, HttpStatusCode.valueOf(200));
			});
		});
	}
	
	@GetMapping(value = "post-image/{imageUrl}", produces = MediaType.IMAGE_JPEG_VALUE)
	public Mono<ResponseEntity<DataBuffer>> getPostImageWithImageUrl(@PathVariable String imageUrl) {
		return getfiledata("posts/" + imageUrl)
				
				.map(value->{
			return new ResponseEntity<>(value, HttpStatusCode.valueOf(200));
		});
	}
	
	@GetMapping(value = "post-image/wip/{withPostId}", produces = MediaType.IMAGE_JPEG_VALUE)
	public Mono<ResponseEntity<DataBuffer>> getPostImageWithPostID(@PathVariable Integer withPostId) {
		return daolayer.getPost(withPostId).map(p->p.getImgurl()).flatMap(url->{
			return getfiledata("posts/" + url)
				
					 .map(value->{
				return new ResponseEntity<>(value, HttpStatusCode.valueOf(200));
			});
		});
	}
	
	@GetMapping(value = "story-image/{imageUrl}", produces = MediaType.IMAGE_JPEG_VALUE)
	public Mono<ResponseEntity<DataBuffer>> getStoryImageWithUrl(@PathVariable String imageUrl) {
		return getfiledata("stories/" + imageUrl)
				.map(value->{
			return new ResponseEntity<>(value, HttpStatusCode.valueOf(200));
		});
	}
	
	@GetMapping(value = "post-image/wis/{withStoryId}", produces = MediaType.IMAGE_JPEG_VALUE)
	public Mono<ResponseEntity<DataBuffer>> getImageWithStoryId(@PathVariable Integer withStoryId) {
		return daolayer.getStory(withStoryId).map(p->p.getImgurl()).flatMap(url->{
			return getfiledata("posts/" + url)
					.map(value->{
				return new ResponseEntity<>(value, HttpStatusCode.valueOf(200));
			});
		});
	}
	
	
	private Mono<DataBuffer> getDefaultProfileImage() {
		return DataBufferUtils.join(DataBufferUtils.read(defaultProfileImageResource, new DefaultDataBufferFactory(), 4096));
	}
	
	private Mono<DataBuffer> getfiledata(String profilePath) {
		String filesPath = "/home/kaustubh/social-file-data/";
		Resource resource = new FileSystemResource(filesPath+profilePath);
		Flux<DataBuffer> readFlux = DataBufferUtils.read(resource, new DefaultDataBufferFactory(), 4096);
		Mono<DataBuffer> data = DataBufferUtils.join(readFlux);
		return data;
	}	

}
