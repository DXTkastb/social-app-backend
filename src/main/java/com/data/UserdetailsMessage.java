package com.data;
import lombok.Data;
import lombok.NonNull;

@Data
public class UserdetailsMessage {	

	String username;
    
    String accountname;
    
    String profileurl= "";
    
    String delegation = "";
    
    String about = "";
    
    String link = "";
    
    Integer postsCount = 0;
    
    Integer followerCount = 0;
    
    Integer followingCount = 0;
    
    Boolean follow = false;
    
    
    public static UserdetailsMessage fromUserdetails(Userdetails userdetails) {
    	UserdetailsMessage udm = new UserdetailsMessage();
    	udm.setAbout(userdetails.about);
    	udm.setAccountname(userdetails.accountname);
    	udm.setLink(userdetails.link);
    	udm.setDelegation(userdetails.delegation);
    	udm.setFollowerCount(0);
    	udm.setFollowingCount(0);
    	udm.setPostsCount(0);
    	udm.setProfileurl(userdetails.profileurl);
    	udm.setUsername(userdetails.username);
    	
    	return udm;
    }
    

}
