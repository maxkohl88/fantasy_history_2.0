package com.importio.api.clientlite.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
 * Test 7
 * 
 * Tests querying a working source with username and password
 * 
 * @author dev@import.io
 * @see https://github.com/import-io/importio-client-libs/tree/master/java
 */
public class IO7Test extends TestHelper {

	// This records the data returned
	private List<String> namesReturned = new ArrayList<String>();
	
	@Test
	public void test() {
		ImportIO client = new ImportIO();
		client.setHost(host);
		try {
			client.login(username, password);
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
					List<Map<String, String>> results = (List<Map<String, String>>) resultMessage.get("results");
					for (Map<String, String> result : results) {
						if (!result.containsKey("name")) {
							fail("Entry does not contain a 'name'");
						} else {
							namesReturned.add(result.get("name"));
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
		query.setConnectorGuids(Arrays.asList(UUID.fromString("1ac5de1d-cf28-4e8a-b56f-3c42a24b1ef2")));
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
		
		assertArrayEquals(expectedNames.toArray(), namesReturned.toArray());
	}

}
