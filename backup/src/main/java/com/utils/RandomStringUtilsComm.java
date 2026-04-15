package com.utils;

import org.apache.commons.lang.RandomStringUtils;

public class RandomStringUtilsComm {
	
	public static String getRandomPassword (){
		return RandomStringUtils.randomNumeric(6);
	}
	
	
}
