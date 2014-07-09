package com.importio.api.clientlite;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.AccessLevel;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.extern.java.Log;

import com.importio.api.clientlite.data.Query;
import com.importio.api.clientlite.data.QueryRequest;
import com.importio.api.clientlite.json.JsonImplementation;

/**
 * The main interface to the import.io client library, used to initialise the connection
 * to the import.io platform and issuing and receiving data from queries
 * 
 * @author dev@import.io
 * @see https://github.com/import-io/importio-client-libs/tree/master/java
 */
@Log
@FieldDefaults(level=AccessLevel.PRIVATE) 
public class ImportIO {
	
	/**
	 * The {@see JsonImplementation} that we are going to use
	 */
	@Setter JsonImplementation jsonImplementation;
	
	/**
	 * Allows the user to override the executor service that we use to execute callback
	 * functions on
	 */
	@Setter ExecutorService executorService = Executors.newSingleThreadExecutor();
	
	/**
	 * The hostname for the API servers
	 */
	@Setter String host = "import.io";
	
	/**
	 * If we are using User GUID and API key as credentials, store them here
	 */
	UUID userId;
	String apiKey;
	
	/**
	 * If we are using user/pass authentication, store them here for future sessions
	 */
	String username;
	String password;
	
	/**
	 * A queue of queries that were requested while not connected
	 */
	Queue<QueryRequest> queue = new LinkedList<QueryRequest>();
	
	/**
	 * The session we are currently using
	 */
	Session session;
	
	/**
	 * Constructor for when logging in later (e.g. with username and password)
	 */
	public ImportIO() {
		this(null,null);
	}
	
	/**
	 * Construct a new client with User GUID and API key authentication 
	 * 
	 * @param userId
	 * @param apiKey
	 */
	public ImportIO(UUID userId, String apiKey) {
		this.userId = userId;
		this.apiKey = apiKey;
	}
	
	/**
	 * Construct a new client with User GUID and API key authentication, and a hostname to connect to
	 * 
	 * @param userId
	 * @param apiKey
	 */
	public ImportIO(UUID userId, String apiKey, String host) {
		this.userId = userId;
		this.apiKey = apiKey;
		this.host = host;
	}
	
	/**
	 * If you want to use cookie-based authentication, this method will log you in with
	 * a username and password to get a session
	 * 
	 * @param username
	 * @param password
	 * @throws IOException
	 */
	public void login(String username, String password) throws IOException {
		// Copy the configuration to the local state of the library
		this.username = username;
		this.password = password;
		
		// If we don't have a session, then connect one
		if (this.session == null) {
			this.connect();
		}
		
		// Once connected, do the login
		this.session.login(this.username, this.password);
	}
	
	/**
	 * Reconnects the client to the platform by establishing a new session
	 * @throws IOException 
	 */
	public void reconnect() throws IOException {
		
		log.info("Reconnecting");
		
		// Disconnect an old session, if there is one
		if (this.session != null) {
			log.warning("Already have a session, using that; call disconnect() to end it");
			this.disconnect();
		}
		
		// Reconnect using username/password if required
		if (this.username != null) {
			this.login(this.username, this.password);
		} else {
			this.connect();
		}
	}
	
	/**
	 * Connect this client to the import.io server if not already connected
	 * 
	 * @throws IOException
	 */
	public void connect() throws IOException {
		
		log.info("Connecting");
		
		// Check if there is a session already first
		if (this.session != null) {
			return;
		}
		
		// Create a new session and connect it
		this.session = new Session(this, this.host, this.userId, this.apiKey, this.jsonImplementation, this.executorService);
		this.session.connect();
		
		// Execute each of the queued queries
		Iterator<QueryRequest> queueIterator = queue.iterator();
		while (queueIterator.hasNext()) {
			QueryRequest entry = queueIterator.next();
			this.session.query(entry.getQuery(), entry.getCallback());
			queueIterator.remove();
		}
	}

	/**
	 * Call this method to ask the client library to disconnect from the import.io server
	 * It is best practice to disconnect when you are finished with querying, so as to clean
	 * up resources on both the client and server
	 * 
	 * @throws IOException
	 */
	public void disconnect() throws IOException {
		// Disconnect and remove the session, if we have one
		if (this.session != null) {
			log.info("Disconnecting");
			
			this.session.disconnect();
			this.session = null;
		} else {
			log.info("Already disconnected");
		}
	}
	
	/**
	 * Send a query to the import.io platform
	 * 
	 * @param query
	 * @param callback
	 * @throws IOException
	 */
	public void query(Query query, MessageCallback callback) throws IOException {
		// If there is no valid session, queue the query
		if (this.session == null || !this.session.isConnected()) {
			log.info("Queueing query: no connected session");
			this.queue.add(new QueryRequest(query, callback));
		} else {
			log.info("Issuing query");
			this.session.query(query, callback);
		}
	}

	/**
	 * Helper method used in tests to override the Client ID
	 * 
	 * @param n
	 */
	protected void setClientId(String n) {
		this.session.setClientId(n);
	}
	
}
