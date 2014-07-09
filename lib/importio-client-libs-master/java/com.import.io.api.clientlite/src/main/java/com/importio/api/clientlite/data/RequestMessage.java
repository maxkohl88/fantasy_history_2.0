package com.importio.api.clientlite.data;

import java.util.List;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

/**
 * A RequestMessage is a message that is used to send protocol-level (CometD) data to the import.io platform
 * 
 * @author dev@import.io
 * @see https://github.com/import-io/importio-client-libs/tree/master/java
 */
@Data
@Accessors(chain=true)
@FieldDefaults(level=AccessLevel.PRIVATE)
public class RequestMessage {
	
	/**
	 * Which CometD channel this message is intended for
	 */
	String channel;
	
	/**
	 * Which CometD connection type we are using, always long-polling for now
	 */
	String connectionType = "long-polling";
	
	/**
	 * Which version of CometD protocol we are using
	 */
	String version;
	
	/**
	 * Minimum CometD version we can use
	 */
	String minimumVersion;
	
	/**
	 * The subscription we are using
	 */
	String subscription;
	
	/**
	 * List of supported CometD connection types
	 */
	List<String> supportedConnectionTypes;
	
	/**
	 * The unique ID for this message
	 */
	int id;
	
	/**
	 * An identifying client ID for the client sending the message
	 */
	String clientId;
	
	/**
	 * The data payload for the CometD message.
	 * @see Query
	 */
	Query data;
	
}
