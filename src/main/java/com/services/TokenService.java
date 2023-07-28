package com.services;

import java.security.Key;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class TokenService {

	private static final String JWT_SECRET_KEY = "qdsfkjbwfjn323rwefwdef3kewrwerv5236v56d56w1xweec3wdn3i432oi";
	private static final Key key = Keys.hmacShaKeyFor(JWT_SECRET_KEY.getBytes());

	public String verifyToken(String jwt) {
		String accountname = "";	
		try {		
			Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(jwt).getBody();		
			accountname = claims.get("accountname", String.class);
			}
		catch(Exception ex) {}
		
		return accountname;
	}
	
	public boolean verifiyTokenForResource(String jwt) {
		return !(verifyToken(jwt).isEmpty()); 
	}
	

	public String generateToken(String accountname, String username) {
		String token =
		Jwts.builder()
				.claim("username", username)
				.claim("accountname", accountname)
				.signWith(key)			
				.compact();
		return token;
	}
}

// Jwts.parserBuilder().setSigningKey(key).build().isSigned(jwt);
