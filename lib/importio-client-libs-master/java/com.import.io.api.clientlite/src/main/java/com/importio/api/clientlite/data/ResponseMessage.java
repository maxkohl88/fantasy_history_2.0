package com.importio.api.clientlite.data;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * A ResponseMessage is a message that is received from the API with each CometD request
 * 
 * @author dev@import.io
 * @see https://github.com/import-io/importio-client-libs/tree/master/java
 */
@Data
@FieldDefaults(level=AccessLevel.PRIVATE)
public class ResponseMessage {
	
	/**
	 * Indicates whether or not the request in question was successful
	 */
	Boolean successful;
	
	/**
	 * A unique message ID
	 */
	int id;
	
	/**
	 * Which client this message is intended for
	 */
	String clientId;
	
	/**
	 * Which channel this message relates to
	 */
	String channel;
	
	/**
	 * If there was an error, its code will be here
	 */
	String error;
	
	/**
	 * The data payload
	 * @see QueryMessage
	 */
	QueryMessage data;
}
