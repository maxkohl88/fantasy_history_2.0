package com.importio.api.clientlite.data;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Synchronized;
import lombok.experimental.FieldDefaults;
import lombok.extern.java.Log;

import com.importio.api.clientlite.MessageCallback;
import com.importio.api.clientlite.data.QueryMessage.MessageType;

/**
 * This class is responsible for tracking an in-progress query and reporting on its progress
 * 
 * @author dev@import.io
 * @see https://github.com/import-io/importio-client-libs/tree/master/java
 */
@Log
@FieldDefaults(level=AccessLevel.PRIVATE)
public class ExecutingQuery {

	/**
	 * The number of jobs the server has started in the process of executing this query
	 */
	int jobsStarted;
	
	/**
	 * The number of jobs that the server has completed on this query
	 */
	int jobsCompleted;
	
	/**
	 * The number of jobs the servers thus far have spawned when processing this query
	 */
	int jobsSpawned;
	
	/**
	 * Track the number of messages we receive for this query. Atomic so as to ensure
	 * consistency when receiving messages asynchronously from the underlying connection
	 */
	AtomicInteger messages = new AtomicInteger();
	
	/**
	 * The identifier for this query that the client and server use to associate messages
	 * with the specific query
	 */
	public UUID queryId;
	
	/**
	 * This flag is set to true once the server has sent enough STOP messages to indicate
	 * that all of the INITed and SPAWNed jobs for this query have been completed
	 */
	@Getter
	boolean finished = false;
	
	/**
	 * A callback to be used whenever a new message is received and processed relating to this query
	 */
	MessageCallback messageCallback;
	
	/**
	 * The query object that was used to begin the query process on the server
	 */
	@Getter Query query;

	/**
	 * The executor we will use to execute asynchronous operations
	 */
	ExecutorService executorService;
	
	/**
	 * Construct a new instance of the ExecutingQuery to track the progress of the specific query on the
	 * import.io platform
	 * 
	 * @param executorService
	 * @param query
	 * @param messageCallback
	 */
	public ExecutingQuery(ExecutorService executorService, Query query, MessageCallback messageCallback) {
		this.executorService = executorService;
		this.query = query;
		this.messageCallback = messageCallback;
	}

	/**
	 * When the CometD channel provides a message for this specific query, handle it by updating our current progress
	 * and then pass it off to the callback for use by the user 
	 * 
	 * @param message
	 */
	public void onMessage(final QueryMessage message) {
		
		log.log(Level.INFO, "Received {0} message", message.getType());
		
		final Progress progress = updateProgress(message);
		
		if(messageCallback != null) {
			executorService.submit(new Runnable() {
				public void run() {
					messageCallback.onMessage(query, message, progress);
				}
			});
		}
		
	}

	/**
	 * Updates the progress of this specific query based on the arrival of a new message
	 * 
	 * @param message
	 * @return
	 */
	@Synchronized
	private Progress updateProgress(QueryMessage message) {
		
		// Analyse the type of the message to update our progress tracking metrics
		switch (message.getType()) {
			case SPAWN:
				// A new job has been spawned by the server
				jobsSpawned++;
				break;
			case INIT:
			case START:
				// A new job has been initialised or started
				jobsStarted++;
				break;
			case STOP:
				// A job has been completed
				jobsCompleted++;
				break;
			case MESSAGE:
				// A message has been received, so just track how many we have
				messages.incrementAndGet();
				break;
			case CANCEL:
			case ERROR:
			case UNAUTH:
			default:
				// There has been some kind of problem
				break;
		}
		
		// Update the finished state based on our tracked message metrics
		finished = jobsStarted == jobsCompleted && jobsSpawned + 1 == jobsStarted && jobsStarted > 0;
		
		// If there is an error or the user is not authorised correctly then mark this query as finished
		if(message.getType() == MessageType.ERROR || message.getType() == MessageType.UNAUTH ||
				message.getType() == MessageType.CANCEL || message.getType() == MessageType.DISCONNECT) {
			finished = true;
		}
		
		// Return an object which indicates the current progress level
		return getProgress();
	}
	

	/**
	 * Returns a {@see Progress} object which indicates to what level of completion this query is
	 * 
	 * @return
	 */
	public Progress getProgress() {
		return new Progress(finished, jobsSpawned, jobsStarted,jobsCompleted, messages.get());
	}

}
