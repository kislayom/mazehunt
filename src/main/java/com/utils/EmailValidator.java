package com.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmailValidator {

	private Pattern pattern;
	private Matcher matcher;

	private static final String EMAIL_PATTERN = 
		"^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
		+ "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

	public EmailValidator() {
		pattern = Pattern.compile(EMAIL_PATTERN);
	}

	/**
	 * Validate hex with regular expression
	 * 
	 * @param hex
	 *            hex for validation
	 * @return true valid hex, false invalid hex
	 */
	public boolean validate(final String hex) {
        if(hex.toUpperCase().contains("@IMPETUS.CO.IN")){
       	 matcher = pattern.matcher(hex);
    		return matcher.matches(); 
        }
		return false;

	}
	
	public static boolean validateEmail (String email) {
		EmailValidator em  = new EmailValidator();
		return em.validate(email);
	}
}