package com.importio.api.clientlite.data;

import com.importio.api.clientlite.MessageCallback;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * This class wraps up a request for a query to happen, so we can queue them
 *
 * @author dev@import.io
 * @see https://github.com/import-io/importio-client-libs/tree/master/java
 */
@Data
@AllArgsConstructor
@FieldDefaults(level=AccessLevel.PRIVATE) 
public class QueryRequest {

	/**
	 * The query the user issued
	 */
	Query query;

	/**
	 * The callback they wanted for messages about the query
	 */
	MessageCallback callback;
	
}
