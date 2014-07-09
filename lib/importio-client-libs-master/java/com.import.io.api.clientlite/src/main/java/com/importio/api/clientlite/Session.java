package com.importio.api.clientlite;

import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import com.importio.api.clientlite.data.ExecutingQuery;
import com.importio.api.clientlite.data.Query;
import com.importio.api.clientlite.data.QueryMessage;
import com.importio.api.clientlite.data.RequestMessage;
import com.importio.api.clientlite.data.ResponseMessage;
import com.importio.api.clientlite.data.QueryMessage.MessageType;
import com.importio.api.clientlite.json.JacksonJsonImplementation;
import com.importio.api.clientlite.json.JsonImplementation;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.extern.java.Log;

/**
 * Session manager, used for managing the message channel, sending queries and receiving data
 *
 * @author dev@import.io
 * @see https://github.com/import-io/importio-client-libs/tree/master/java
 */
@Log
@FieldDefaults(level=AccessLevel.PRIVATE) 
public class Session {

	/**
	 * Content types and other constants that we need to use to interact with the import.io API
	 */
	static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded";
	static final String JSON_CONTENT_TYPE = "application/json;charset=UTF-8";
	static final String UTF_8 = "UTF-8";
	
	/**
	 * The JSON implemenation for this session
	 */
	JsonImplementation jsonImplementation;

	/**
	 * Cookie manager to manage our cookies between requests
	 */
	final CookieManager cookieManager = new CookieManager();
	final ThreadLocal<CookieManager> cookieManagers = new ThreadLocal<CookieManager>();
	
	/**
	 * The executor service to use for this session
	 */
	ExecutorService executorService;
	
	/**
	 * Configuration for the CometD client
	 */
	static final String MESSAGING_CHANNEL = "/messaging";
	static final String CLIENT_NAME = "import.io Java (lite) client";
	static final String CLIENT_VERSION = "2.0.0";
	
	/**
	 * Maintain a reference to the main client
	 */
	ImportIO io;

	/**
	 * A map which stores the currently running queries that have been issued but are yet to be completed
	 */
	final ConcurrentHashMap<String, ExecutingQuery> queries = new ConcurrentHashMap<String, ExecutingQuery>();
	
	/**
	 * Thread which allows us to connect to the import.io platform asynchronously
	 */
	private Thread pollThread;
	
	/**
	 * The domain host for our session
	 */
	String host;
	
	/**
	 * An incrementing count for the message identifier we send, as each needs to be different
	 */
	int msgId = 1;
	
	/**
	 * Once we are connected, we need to store a client identifier here
	 */
	@Setter String clientId;
	
	/**
	 * If we are using User GUID and API key as credentials, store them here
	 */
	UUID userId;
	String apiKey;
	

	/**
	 * Flag to indicate whether we are currently connected to the query server
	 */
	@Getter boolean connected = false;
	
	/**
	 * Flag to indicate whether we are in the process of connecting to the server
	 */
	boolean connecting = false;
	
	/**
	 * Flag to indicate whether we are currently in the process of disconnecting
	 */
	boolean disconnecting = false;
	
	/**
	 * Flag to indicate whether we have a running poll to the server
	 */
	boolean polling = false;
	
	/**
	 * Constructs a new Session object
	 * 
	 * @param io
	 * @param host
	 * @param userId
	 * @param apiKey
	 */
	public Session(ImportIO io, String host, UUID userId, String apiKey, JsonImplementation jsonImplementation, ExecutorService executorService) {
		this.io = io;
		this.host = host;
		this.userId = userId;
		this.apiKey = apiKey;
		this.jsonImplementation = jsonImplementation;
		this.executorService = executorService;
		setupCookieHandler();
	}
	
	/**
	 * Does an authentication to the import.io platform
	 * 
	 * @param username
	 * @param password
	 * @throws IOException
	 */
	public void login(String username, String password) throws IOException {
		cookieManagers.set(cookieManager);
		try {
			HttpURLConnection urlConnection = (HttpURLConnection) new URL("https://api." + host + "/auth/login").openConnection();
			urlConnection.setDoOutput(true); 
			urlConnection.setDoInput(true); 
			urlConnection.setRequestMethod("POST");
			
			urlConnection.setRequestProperty("Content-Type", FORM_CONTENT_TYPE);
			urlConnection.setRequestProperty("Accept-Encoding", "gzip");
			
			OutputStreamWriter writer = new OutputStreamWriter(urlConnection.getOutputStream());
			writer.write("username=");
			writer.write(URLEncoder.encode(username,UTF_8));
			writer.write("&password=");
			writer.write(URLEncoder.encode(password,UTF_8));
			writer.close();
			
			if ( urlConnection.getResponseCode() != 200 ) {
				throw new IOException("Connect failed, status " + urlConnection.getResponseCode() );
			}
			
		} finally {
			cookieManagers.remove();
		}
	}
	
	/**
	 * Connect the client library to the import.io platform, if not already connected
	 * 
	 * @throws IOException
	 */
	public void connect() throws IOException {
		// If we're already connected then don't reconnect
		if (connected || connecting) {
			return;
		}
		
		connecting = true;
		
		// If there is no JSON implementation specified, default to the Jackson one
		if (jsonImplementation == null) {
			jsonImplementation = new JacksonJsonImplementation();
		}
		
		// Initialise the connection by handshaking with the server
		handshake();
		
		// Subscribe to our messaging channel
		subscribe(MESSAGING_CHANNEL);
		
		connected = true;
		
		// Start the polling background thread (which maintains the connection to the platform)
		pollThread = new Thread(new Runnable() {
			public void run() {
				poll();
			}
		});
		pollThread.setDaemon(true);
		pollThread.start();
		
		connecting = false;
	}
	
	/**
	 * Make a generic CometD request to the import.io platform
	 * 
	 * @param channel
	 * @param path
	 * @param data
	 * @param throwExceptionOnFail
	 * @return
	 * @throws IOException
	 */
	private List<ResponseMessage> request(String channel, String path, RequestMessage data, boolean throwExceptionOnFail) throws IOException {
		cookieManagers.set(cookieManager);
		try {
		
			// If there is no data specified as input, we need to create one to send the metadata
			if ( data == null ) {
				data = new RequestMessage();
			}
			
			// Provide the messaging channel we are using
			data.setChannel(channel);
			// Set the specific message ID
			data.setId(msgId++);
			// Use our client ID so the server can identify us
			data.setClientId(clientId); 
			
			// Build the URL based on host configuration, and whether we need to add authentication
			String url = format("https://query.%s/query/comet/%s", host, path);
			if ( apiKey != null ) {
				StringBuilder builder = new StringBuilder(url);
				builder.append('?');
				builder.append("_user=");
				builder.append(userId.toString());
				builder.append('&');
				builder.append("_apikey=");
				builder.append(URLEncoder.encode(apiKey, UTF_8));
				url = builder.toString();
			}
			
			// Initialise the connection to send the data
			HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
			urlConnection.setRequestMethod("POST");
			
			// Specify the headers needed for the CometD protocol
			urlConnection.setRequestProperty("Content-Type", JSON_CONTENT_TYPE);
			urlConnection.setRequestProperty("Accept-Encoding", "gzip");
			
			// Identify this client with the import.io platform
			urlConnection.setRequestProperty("import-io-client", CLIENT_NAME);
			urlConnection.setRequestProperty("import-io-client-version", CLIENT_VERSION);

			// The request objects have to be an array of messages
			List<RequestMessage> requestArray = new ArrayList<RequestMessage>();
			requestArray.add(data);
			
			urlConnection.setDoOutput(true); 
			jsonImplementation.writeRequest(urlConnection.getOutputStream(), requestArray);
			
			// Don't process the response if we've disconnected in the meantime
			if (!connected && !connecting) {
				// Just return an empty array
				return new ArrayList<ResponseMessage>();
			}
			
			// Check that we got a valid response code (e.g. the server might be down)
			if (urlConnection.getResponseCode() != 200) {
				String error_message = "Unable to connect to import.io, status " + urlConnection.getResponseCode() + " for URL " + url;
				if (throwExceptionOnFail) {
					throw new IOException(error_message);
				} else {
					log.warning(error_message);
					return new ArrayList<ResponseMessage>();
				}
			}
			
			String encoding = urlConnection.getContentEncoding();
			
			// Parse the response messages from the CometD server
			final List<ResponseMessage> list = jsonImplementation.readResponse(makeInputStream(urlConnection, encoding));
			for (ResponseMessage msg : list) {
				// Process each message and ascertain its result
				if ( msg.getSuccessful() != null && ! msg.getSuccessful() ) {
					String err = "Unsuccessful request:" + msg;
					// Only identify this as a problem if we are connected and not disconnecting
					if (this.connected && !this.disconnecting && !this.connecting) {
						// If we get a 402 unknown client we need to reconnect
						if (msg.getError() != null && msg.getError().equals("402::Unknown client")) {
							log.warning("402 received, reconnecting");
							this.io.reconnect();
						} else if (throwExceptionOnFail) {
							// Only throw the exception if requested to
							throw new IOException(err);
						}
						// Always log out as a problem
						log.warning(err);
					}
					// We can't process this message as it was not successful, so skip to the next
					continue;
				}
				
				// Disregard messages that are not on the messaging channel we are using 
				if (!MESSAGING_CHANNEL.equals(msg.getChannel())) {
					continue;
				}

				// Now we have identified the message as successful and on our channel, attempt to handle it
				try {
					processMessage(msg);
				} catch ( Exception e ) {
					log.log(Level.SEVERE, "Processing message failed: "+msg, e);
				}
			}
			
			return list;
			
		} finally {
			cookieManagers.remove();
		}
	}
	
	/**
	 * Disconnect the client from the import.io platform. This allows us to clean up resources on both the
	 * client and the server
	 * 
	 * @throws IOException
	 */
	public void disconnect() throws IOException {
		// Set the disconnecting flag to show we are currently in the process of disconnecting
		disconnecting = true;
		// Prevent any further requests going to the server by removing the connection flag
		connected = false;
		// Send a "disconnected" message to all of the current queries, and then remove them
		Iterator<Entry<String, ExecutingQuery>> queryIterator = queries.entrySet().iterator();
		while (queryIterator.hasNext()) {
			Entry<String, ExecutingQuery> entry = queryIterator.next();
			QueryMessage message = new QueryMessage();
			message.setRequestId(entry.getKey());
			message.setType(MessageType.DISCONNECT);
			entry.getValue().onMessage(message);
			queryIterator.remove();
		}
		// Notify the server that we have disconnected
		request("/meta/disconnect", "", null, true);
		// Now we are disconnected, reset our client ID for the next connection
		clientId = null;
		// We have finished disconnecting
		disconnecting = false;
	}
	
	/**
	 * Send a query to the import.io platform
	 * 
	 * @param query
	 * @param callback
	 * @throws IOException
	 */
	public void query(Query query, MessageCallback callback) throws IOException {
		// We need to generate a unique request ID so we can track the responses to specific queries
		query.setRequestId(UUID.randomUUID().toString());
		// Add the query object to our internal list of currently running queries
		queries.put(query.getRequestId(), new ExecutingQuery(executorService, query, callback));
		// Dispatch the query to the import.io platform
		request("/service/query", "", new RequestMessage().setData(query), true);
	}

	/**
	 * For each message that we get in response from the import.io platform,
	 * this method handles those messages and manages the state of the overall connection
	 * based on their contents
	 * 
	 * @param msg
	 */
	private void processMessage(final ResponseMessage msg) {
		// Find the right query that this message relates to
		final ExecutingQuery query = queries.get(msg.getData().getRequestId());
		// If we don't recognise the request ID, then do not process the message
		if (query == null) {
			log.warning("Unknown Request ID returned from server: " + msg.getData().getRequestId());
			return;
		}
		// Call the callback for that query
		query.onMessage(msg.getData());
		// If this message has completed the query, then remove the query from the 
		if ( query.isFinished() ) {
			queries.remove(msg.getData().getRequestId());
		}
	}

	/**
	 * A helper method to generate an input stream with the correct encoding we need
	 * 
	 * @param urlConnection
	 * @param encoding
	 * @return
	 * @throws IOException
	 */
	private InputStream makeInputStream(HttpURLConnection urlConnection, String encoding) throws IOException {
		InputStream resultingInputStream;

		if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
			resultingInputStream = new GZIPInputStream(urlConnection.getInputStream());
		} else if (encoding != null && encoding.equalsIgnoreCase("deflate")) {
			resultingInputStream = new InflaterInputStream(urlConnection.getInputStream(), new Inflater(true));
		} else {
			resultingInputStream = urlConnection.getInputStream();
		}
		return resultingInputStream;
	}

	/**
	 * The polling thread, which maintains the connection to the platform
	 */
	private void poll() {
		// Make sure we are not polling already first
		if (polling) {
			log.warning("Already polling, so not polling again");
			return;
		}
		while (connected) {
			try {
				request("/meta/connect", "connect", null, false);
			} catch (IOException e) {
				log.log(Level.WARNING, "Exception thrown", e);
			}
		}
		polling = false;
	}

	/**
	 * Handshakes with the server to begin the CometD connection
	 * 
	 * @throws IOException
	 */
	private void handshake() throws IOException {
		List<ResponseMessage> list = request("/meta/handshake", "handshake", new RequestMessage().setMinimumVersion("0.9").setVersion("1.0").setSupportedConnectionTypes(Arrays.asList("long-polling")), true);
		if (list.size() > 0) {
			// Once the handshake is completed, we have a client ID which we need to use on subsequent requests, so store that up
			clientId = list.get(0).getClientId();
		}
	}
	
	/**
	 * Send a subscription request to the server, in order to subscribe to a specific messaging channel
	 * 
	 * @param subscription
	 * @throws IOException
	 */
	private void subscribe(String subscription) throws IOException {
		request("/meta/handshake", "handshake", new RequestMessage().setSubscription(subscription), true);
	}
	
	/**
	 * Initialises the cookie handler, which is used for each request to the import.io platform
	 * in order to ensure our cookies (such as auth, ELB stickiness) are maintained between
	 * requests.
	 */
	private void setupCookieHandler() {
		final CookieHandler def = CookieHandler.getDefault();
		CookieHandler.setDefault(new CookieHandler() {
			
			@Override
			public void put(URI uri, Map<String, List<String>> responseHeaders) throws IOException {
				CookieManager cookieManager = cookieManagers.get();
				if ( cookieManager != null ) {
					cookieManager.put(uri, responseHeaders);
				} else {
					def.put(uri, responseHeaders);
				}
			}
			
			@Override
			public Map<String, List<String>> get(URI uri, Map<String, List<String>> requestHeaders) throws IOException {
				CookieManager cookieManager = cookieManagers.get();
				if ( cookieManager != null ) {
					return cookieManager.get(uri, requestHeaders);
				}
				return def.get(uri, requestHeaders);
			}
		});
	}
	
}
