package com.importio.api.clientlite.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.importio.api.clientlite.ImportIO;
import com.importio.api.clientlite.MessageCallback;
import com.importio.api.clientlite.data.Progress;
import com.importio.api.clientlite.data.Query;
import com.importio.api.clientlite.data.QueryMessage;
import com.importio.api.clientlite.data.QueryMessage.MessageType;

/**
 * Test 5
 * 
 * Test that querying a source that returns an error is handled correctly
 * 
 * @author dev@import.io
 * @see https://github.com/import-io/importio-client-libs/tree/master/java
 */
public class IO5Test extends TestHelper {

	// This records whether the test was successful
	private Boolean success = false;
	
	@Test
	public void test() {
		ImportIO client = new ImportIO(UUID.fromString(userGuid), apiKey);
		client.setHost(host);
		try {
			client.connect();
		} catch (IOException e) {
			fail("Should not have thrown an exception");
		}
		
		final CountDownLatch latch = new CountDownLatch(1);
		MessageCallback messageCallback = new MessageCallback() {
			
			@SuppressWarnings("unchecked")
			public void onMessage(Query query, QueryMessage message, Progress progress) {
				if (message.getType() == MessageType.MESSAGE) {
					HashMap<String, Object> resultMessage = (HashMap<String, Object>) message.getData();
					if (resultMessage.containsKey("errorType")) {
						if (resultMessage.get("errorType").equals("UnauthorizedException")) {
							success = true;
						}
					}
				}
				if ( progress.isFinished() ) {
					latch.countDown();
				}
			}
		};
		
		Map<String, Object> queryInput = new HashMap<String,Object>();
		queryInput.put("query", "server");
		// Generate a query object, and specify the connector GUIDs and the input
		Query query = new Query();
		query.setConnectorGuids(Arrays.asList(UUID.fromString("eeba9430-bdf2-46c8-9dab-e1ca3c322339")));
		query.setInput(queryInput);
		
		try {
			client.query(query, messageCallback);
		} catch (IOException e) {
			fail("Should not have thrown an exception");
		}
		
		try {
			latch.await();
		} catch (InterruptedException e) {
			fail("Should not have thrown an exception");
		}
		
		try {
			client.disconnect();
		} catch (IOException e) {
			fail("Should not have thrown an exception");
		}
		
		assertEquals(true, success);
	}

}
