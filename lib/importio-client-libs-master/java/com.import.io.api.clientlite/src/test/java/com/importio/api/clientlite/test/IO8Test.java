package com.importio.api.clientlite.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.importio.api.clientlite.MessageCallback;
import com.importio.api.clientlite.data.Progress;
import com.importio.api.clientlite.data.Query;
import com.importio.api.clientlite.data.QueryMessage;
import com.importio.api.clientlite.data.QueryMessage.MessageType;

/**
 * Test 8
 * 
 * Tests querying a working source twice, with a client ID change in the middle
 * 
 * @author dev@import.io
 * @see https://github.com/import-io/importio-client-libs/tree/master/java
 */
public class IO8Test extends TestHelper {

	// This records the data returned
	private List<String> namesReturned = new ArrayList<String>();
	
	// Records the number of disconnect messages received
	private int disconnects = 0;
	
	// Latch for callbacks
	CountDownLatch latch = new CountDownLatch(1);
	
	@Test
	public void test() {
		TestLibrary client = new TestLibrary(UUID.fromString(userGuid), apiKey);
		client.setHost(host);
		try {
			client.connect();
		} catch (IOException e) {
			fail("Should not have thrown an exception");
		}
		
		MessageCallback messageCallback = new MessageCallback() {
			
			@SuppressWarnings("unchecked")
			public void onMessage(Query query, QueryMessage message, Progress progress) {
				if (message.getType() == MessageType.DISCONNECT) {
					disconnects++;
				}
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
		
		// Set the client ID to be something random
		client.setTestClientId("random");
		
		// Do second query, which will fail
		latch = new CountDownLatch(1);
		try {
			client.query(query, messageCallback);
		} catch (IOException e) {
		}
		
		try {
			latch.await();
		} catch (InterruptedException e) {
			fail("Should not have thrown an exception");
		}
		
		// Do final third query, which will succeed
		latch = new CountDownLatch(1);
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
		
		// Finally, clean everything up
		try {
			client.disconnect();
		} catch (IOException e) {
			fail("Should not have thrown an exception");
		}
		
		assertArrayEquals(expectedNames.toArray(), Arrays.copyOfRange(namesReturned.toArray(), 0, expectedNames.size()));
		assertArrayEquals(expectedNames.toArray(), Arrays.copyOfRange(namesReturned.toArray(), expectedNames.size(), expectedNames.size()*2));
		assertEquals(1, disconnects);
	}

}