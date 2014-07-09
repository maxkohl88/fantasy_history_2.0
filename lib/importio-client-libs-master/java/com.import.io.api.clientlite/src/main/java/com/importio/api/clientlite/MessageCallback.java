package com.importio.api.clientlite;

import com.importio.api.clientlite.data.Progress;
import com.importio.api.clientlite.data.Query;
import com.importio.api.clientlite.data.QueryMessage;

/**
 * An interface for users to implement when they want to receive messages from import.io queries
 * 
 * @author dev@import.io
 * @see https://github.com/import-io/importio-client-libs/tree/master/java
 */
public interface MessageCallback {
	
	/**
	 * Called every time a message is received from the server
	 * relating to the current query
	 * 
	 * @see ExecutingQuery.onMessage
	 * 
	 * @param query
	 * @param message
	 * @param progress
	 */
	void onMessage(Query query, QueryMessage message, Progress progress);
}
