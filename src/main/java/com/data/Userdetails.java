package com.data;

import java.util.Map;

import lombok.Data;
import lombok.NonNull;

@Data
public class Userdetails {

	String username;
	
    String password;
    
    String accountname;
    
    String profileurl;

    String delegation;
    
    String about;
    
    String link;
    
    String fkey;
    
    public static Userdetails fromMap(Map map) {
    	Userdetails userdetails = new Userdetails();
    	userdetails.setAccountname((String)map.get("accountname"));
    	userdetails.setDelegation((String)map.get("delegation"));
    	userdetails.setAbout((String)map.get("about"));
    	userdetails.setLink((String)map.get("link"));
    	userdetails.setUsername((String)map.get("username"));
    	return userdetails;
    }
        
}
