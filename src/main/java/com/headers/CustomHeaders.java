package com.headers;

import org.springframework.http.HttpHeaders;

public class CustomHeaders {
	
	/*
	 * FOR REST API BASED LOGIN-SIGNUP 
	 * */
	
    public static final HttpHeaders SIGNUP_ERROR = new HttpHeaders();
    public static final HttpHeaders USER_TAKEN_ERROR = new HttpHeaders();
    public static final HttpHeaders LOGIN_ERROR = new HttpHeaders();
    public static final HttpHeaders USER_DOES_NOT_EXIST_ERROR = new HttpHeaders();
    public static final HttpHeaders IMAGE_ERROR = new HttpHeaders();
    public static final HttpHeaders INCORRECT_PASSWORD_ERROR = new HttpHeaders();
      
    
    /*
     * FOR RSOCKET POST METDATA HEADERS[ACCOUNTNAME,CAPTION]
     * ! Not the best way to handle POST data, still researching RSocket for Multipart data transfer
     */
    
    public static final String HEADER_POST_ACCOUNTNAME_MIME = "message/post.creator.accountname";
    public static final String HEADER_LAST_STORY_INSTANT = "message/last.story.instant";
    public static final String HEADER_POST_CAPTION_MIME = "message/post.creator.caption";
    public static final String HEADER_STORY_ISMEMORY_MIME = "message/story.ismemory";
    public static final String HEADER_POST_ACCOUTNAME_KEY = "post-account-key";
    public static final String HEADER_POST_CAPTION_KEY = "post-caption-key";
    public static final String HEADER_STORY_ISMEMORY_KEY = "ismemory-key";
    
    /*
     * FOR RSOCKET POST IMAGE FILE PAYLOAD 
     * */
    
    public static final String MIME_FILE_EXTENSION   = "message/x.upload.file.extension";
    public static final String MIME_FILE_NAME        = "message/x.upload.file.name";
    public static final String FILE_NAME_KEY = "file-name";
    public static final String FILE_EXTN_KEY = "file-extn";
    
    
    
    
    
    
    static {
        SIGNUP_ERROR.add("error", "Signup Unsuccessful");
        USER_TAKEN_ERROR.add("error", "Account Already Exists!");
        LOGIN_ERROR.add("error", "Login Unsuccessful. Server Error");
        USER_DOES_NOT_EXIST_ERROR.add("error", "User Does Not Exist!");
        IMAGE_ERROR.add("error", "Unable to load image! please try later.");
        INCORRECT_PASSWORD_ERROR.add("error","Incorrect Password");
    }

}
