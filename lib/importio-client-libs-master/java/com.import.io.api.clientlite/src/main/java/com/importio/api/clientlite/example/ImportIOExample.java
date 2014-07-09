package com.importio.api.clientlite.example;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import com.importio.api.clientlite.ImportIO;
import com.importio.api.clientlite.MessageCallback;
import com.importio.api.clientlite.data.Progress;
import com.importio.api.clientlite.data.Query;
import com.importio.api.clientlite.data.QueryMessage;
import com.importio.api.clientlite.data.QueryMessage.MessageType;

/**
 * An example class for making use of the import.io Java client library
 * 
 * @author dev@import.io
 * @see https://github.com/import-io/importio-client-libs/tree/master/java
 */
public class ImportIOExample {
	
	public static void main(String[] args) throws IOException, InterruptedException {

		/**
		 * You have two choices for authenticating with the Java client: you can use your API key
		 * or your username and password. Username and password is quicker to get started with, but
		 * API key authentication will be more reliable for really large query volumes.
		 * If you need it, you can get YOUR_USER_GUID and YOUR_API_KEY from your account page, at
		 * http://import.io/data/account
		 */
		
		/**
		 * To use an API key for authentication, use the following code to initialise then connect the library
		 */
		ImportIO client = new ImportIO(UUID.fromString("YOUR_USER_GUID"), "YOUR_API_KEY");
		client.connect();
		
		/**
		 * If you wish to use username and password based authentication, first create a client:
		 */
		//ImportIO client = new ImportIO();
		/**
		 * Next you need to log in to import.io using your username and password, like so:
		 */
		//client.login("YOUR_USERNAME", "YOUR_PASSWORD");
		
		/**
		 * Because import.io queries are asynchronous, for this simple script we will use a {@see CountdownLatch}
		 * to stop the script from exiting before all of our queries are returned. We are doing three queries in this
		 * example so we initialise it with "3"
		 */
		final CountDownLatch latch = new CountDownLatch(3);
		
		final List<Object> dataRows = new ArrayList<Object>();
		
		/**
		 * In order to receive the data from the queries we issue, we need to define a callback method
		 * This method will receive each message that comes back from the queries, and we can take that
		 * data and store it for use in our app. {@see MessageCallback}
		 */
		MessageCallback messageCallback = new MessageCallback() {
			/**
			 * This method is called every time a new message is received from the server relating to the
			 * query that we issued
			 */
			@SuppressWarnings("unchecked")
			public void onMessage(Query query, QueryMessage message, Progress progress) {
				if (message.getType() == MessageType.MESSAGE) {
					HashMap<String, Object> resultMessage = (HashMap<String, Object>) message.getData();
					if (resultMessage.containsKey("errorType")) {
						// In this case, we received a message, but it was an error from the external service
						System.err.println("Got an error!");
						System.err.println(message);
					} else {
						// We got a message and it was not an error, so we can process the data
						System.out.println("Got data!");
						System.out.println(message);
						// Save the data we got in our dataRows variable for later
						List<Object> results = (List<Object>) resultMessage.get("results");
						dataRows.addAll(results);
					}
				}
				// When the query is finished, countdown the latch so the program can continue when everything is done
				if ( progress.isFinished() ) {
					latch.countDown();
				}
			}
		};
		
		// Generate a list of the connector GUIDs we are going to query
		List<UUID> connectorGuids = Arrays.asList(
			UUID.fromString("39df3fe4-c716-478b-9b80-bdbee43bfbde")
		);
		
		// Generate query objects using the helper method below
		Query q1 = generateQueryObject(connectorGuids, "server");
		client.query(q1, messageCallback);
		
		Query q2 = generateQueryObject(connectorGuids, "ubuntu");
		client.query(q2, messageCallback);
		
		Query q3 = generateQueryObject(connectorGuids, "clocks");
		client.query(q3, messageCallback);
		
		// Wait on all of the queries to be completed
		latch.await();
		
		// It is best practice to disconnect when you are finished sending queries and getting data - it allows us to
		// clean up resources on the client and the server
		client.disconnect();
		
		// Now we can print out the data we got
		System.out.println("All data received:");
		System.out.println(dataRows);
	}

	/**
	 * A helper method that allows us to generate a query based on inputs to send to the server
	 * 
	 * @param client
	 * @param messageCallback
	 * @param connectorGuids
	 * @param queryValue
	 */
	private static Query generateQueryObject(List<UUID> connectorGuids, String queryValue) {
		// Generate a map of inputs we wish to send
		Map<String, Object> queryInput = new HashMap<String,Object>();
		queryInput.put("query", queryValue);
		// Generate a query object, and specify the connector GUIDs and the input
		Query query = new Query();
		query.setConnectorGuids(connectorGuids);
		query.setInput(queryInput);
		return query;
	}

}
