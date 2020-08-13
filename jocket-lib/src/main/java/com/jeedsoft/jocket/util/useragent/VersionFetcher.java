package com.jeedsoft.jocket.util.useragent;

/**
 * Interaface that gets string and returns extrancted version 
 * @author alexr
 */
interface VersionFetcher {
	Version version(String str);
}
