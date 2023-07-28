package com.controllers;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import com.data.Userdetails;
import com.data.UserdetailsMessage;
import com.datalayer.RDBDaoLayer;
import com.datalayer.RedisLayer;
import com.services.TokenService;
import com.headers.CustomHeaders;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@CrossOrigin(value = "*")
@RestController
public class LoginSignupController {

	@Autowired
	RDBDaoLayer daolayer;

	@Autowired
	RedisLayer redislayer;

	@Autowired
	TokenService tokenService;

	/*
	 * USER LOGIN CONTROLLER AUTHORISED : RETURN JWT TOKEN ELSE : RETURN INVALID
	 * USERNAME PASSWORD
	 */

	@PostMapping(value = "login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public Mono<ResponseEntity<UserdetailsMessage>> login(ServerWebExchange serverWebExchange) {

		Mono<MultiValueMap<String, String>> formData = serverWebExchange.getFormData();
		return

		formData.flatMap((form) -> {
			return daolayer.getUserDetails(form.getFirst("accountname")).flatMap((user) -> {

				if (user.getPassword().compareTo(form.getFirst("password")) != 0) {
					return Mono.just(new ResponseEntity<UserdetailsMessage>(null,
							CustomHeaders.INCORRECT_PASSWORD_ERROR, HttpStatusCode.valueOf(401)));
				}
				;
				return daolayer.updateFkey(form.getFirst("accountname"), form.getFirst("fkey"))
						.zipWith(redislayer.getUserDetailsFromRedisCache(user.getAccountname())).map((val) -> {
							String jwt_token = tokenService.generateToken(user.getAccountname(), user.getUsername());
							HttpHeaders headers = new HttpHeaders();
							headers.add("auth-token", jwt_token);
							user.setPassword("");
							return new ResponseEntity<UserdetailsMessage>(val.getT2(), headers,
									HttpStatusCode.valueOf(200));
						});

			}).switchIfEmpty(Mono.just(new ResponseEntity<UserdetailsMessage>(null,
					CustomHeaders.USER_DOES_NOT_EXIST_ERROR, HttpStatusCode.valueOf(401))))
					.onErrorReturn(new ResponseEntity<UserdetailsMessage>(null, CustomHeaders.LOGIN_ERROR,
							HttpStatusCode.valueOf(500)));
		});

	}

	/*
	 * SIGNUP : USRNAME, ACCOUNTNAME, PASSWORD, IMAGE_FILE[OPTIONAL]
	 */

	@PostMapping("signup")
	public Mono<ResponseEntity<UserdetailsMessage>> signup(@RequestPart(name = "accountname") String accountname,
			@RequestPart(name = "username") String username, @RequestPart(name = "password") String password,
			@RequestPart(name = "fkey") String fkey, @RequestPart(name = "imageAvailable") String imageAvailable,
			@RequestPart(name = "image", required = false) Mono<FilePart> imagefile) {

		Mono<Integer> uploadImageToFiles;
		String image_file_name;

		if (imageAvailable.equalsIgnoreCase("true")) {
			image_file_name = accountname + Instant.now().getEpochSecond() + ".jpg";
			uploadImageToFiles = daolayer.uploadProfileImage(imagefile, image_file_name);
		} else {
			image_file_name = "_NULL_";
			uploadImageToFiles = Mono.just(200);
		}

		return daolayer.signup(accountname, password, image_file_name, username, fkey).zipWhen((user) -> {
			return redislayer.createUserCache(user).map((created) -> {
				return UserdetailsMessage.fromUserdetails(user);
			}).zipWith(uploadImageToFiles);
		}).map((val) -> {
			String jwt_token = tokenService.generateToken(val.getT1().getAccountname(), val.getT1().getUsername());
			HttpHeaders headers = new HttpHeaders();
			headers.add("auth-token", jwt_token);
			return new ResponseEntity<UserdetailsMessage>(val.getT2().getT1(), headers, HttpStatusCode.valueOf(200));
		}).doOnError(e -> {
			e.printStackTrace();
		}).onErrorReturn(DataAccessException.class,
				new ResponseEntity<>(null, CustomHeaders.USER_TAKEN_ERROR, HttpStatusCode.valueOf(409)))
				.onErrorReturn(new ResponseEntity<>(null, CustomHeaders.SIGNUP_ERROR, HttpStatusCode.valueOf(500)));

	}

	@PostMapping("updateProfileImage")
	public Mono<String> updateProfileImage(@RequestHeader(value = "auth-token") String jwt_token,@RequestPart(name = "accountname") String accountname,
			@RequestPart(name = "image", required = true) Mono<FilePart> imagefile) {
		String image_file_name = accountname + Instant.now().getEpochSecond() + ".jpg";
		return Mono.just(tokenService.verifyToken(jwt_token)).flatMap(val->{
			if(val.isEmpty()) return Mono.just("");
			return daolayer.updateProfileUrl(accountname, image_file_name).zipWhen((x) -> {
				return redislayer.updateUserProfileUrlCache(accountname, image_file_name);
			}).then(daolayer.uploadProfileImage(imagefile, image_file_name).map(v -> image_file_name)).onErrorReturn("");
		});
		

	}

	@PostMapping("auth")
	public Mono<ResponseEntity<UserdetailsMessage>> authenticateUser(
			@RequestHeader(value = "auth-token") String jwt_token) {
		return Mono.just(tokenService.verifyToken(jwt_token)).flatMap((acname) -> {
			if (acname.isEmpty())
				return Mono.empty();
			return redislayer.getUserDetailsFromRedisCache(acname);
		}).map((user) -> {
			return new ResponseEntity<UserdetailsMessage>(user, null, HttpStatusCode.valueOf(200));
		}).switchIfEmpty(Mono.just(new ResponseEntity<UserdetailsMessage>(null, null, HttpStatusCode.valueOf(401))))
				.doOnError((e) -> {
					e.printStackTrace();
				}).onErrorReturn(new ResponseEntity<UserdetailsMessage>(null, null, HttpStatusCode.valueOf(500)));
	}

	@GetMapping("gtoken")
	public Mono<String> getTT() {
		return Mono.just(tokenService.generateToken("dxtk", "KAUSTUBH"));
	}

	@PostMapping("updateUserDetails")
	public Mono<ResponseEntity<Userdetails>> updateUserDetails(@RequestBody Map map,
			@RequestHeader(value = "auth-token") String jwt_token) {
		Userdetails userdetails = Userdetails.fromMap(map);
		return Mono.just(tokenService.verifyToken(jwt_token)).flatMap(v -> {
			if (v.isEmpty())
				return Mono.just(new ResponseEntity<Userdetails>(null, null, HttpStatusCode.valueOf(403)));
			return daolayer.updateUserDetail(userdetails).zipWhen((u) -> {
				return redislayer.updateUserCache(userdetails);
			}).map(x -> new ResponseEntity<Userdetails>(userdetails, null, HttpStatusCode.valueOf(200)));
		}).onErrorReturn(new ResponseEntity<Userdetails>(userdetails, null, HttpStatusCode.valueOf(500)));
	}

	@PostMapping("updatePassword")
	public Mono<Integer> updateUserPassword(@RequestBody Map map,
			@RequestHeader(value = "auth-token") String jwt_token) {
		return Mono.just(tokenService.verifyToken(jwt_token)).flatMap((x) -> {
			if (x.isEmpty())
				return Mono.just(500);
			else
				return daolayer.changePassword((String) map.get("accountname"), (String) map.get("password"))
						.map(v -> 200).onErrorReturn(500);
		});

	}
}
