package com.data;

import org.springframework.data.annotation.Id;

import lombok.Data;
import lombok.Setter;

@Data
public class Likes {
	@Id
	 Integer id;

	 Integer onpostid;

	 String accountname;
}
