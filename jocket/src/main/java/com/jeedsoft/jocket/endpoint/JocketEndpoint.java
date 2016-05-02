package com.jeedsoft.jocket.endpoint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JocketEndpoint
{
	/**
	 * The path. valid values include:
	 * 1. a simple slash (/)
	 * 2. N (N > 0) parts start with a slash and followed by a word or {word}. for example:
	 *    /abc
	 *    /abc/def
	 *    /abc/{def}
	 *    /{abc}/def/{ghi} 
	 */
	String value();
}
