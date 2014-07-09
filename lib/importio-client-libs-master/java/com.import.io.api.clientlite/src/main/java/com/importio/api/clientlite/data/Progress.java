package com.importio.api.clientlite.data;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * This class wraps up the various pieces of data we track about the progress of queries
 * to allow us to decide how far through a query is, and whether it has finished yet.
 * 
 * @author dev@import.io
 * @see https://github.com/import-io/importio-client-libs/tree/master/java
 */
@Data
@FieldDefaults(level=AccessLevel.PRIVATE)
public class Progress {
	
	/**
	 * The total number of finished jobs
	 */
	final boolean finished;
	
	/**
	 * The total number of jobs spawned
	 */
	final int jobsSpawned;
	
	/**
	 * The total number of jobs started
	 */
	final int jobsStarted;
	
	/**
	 * The total number of jobs completed
	 */
	final int jobsCompleted;
	
	/**
	 * The number of messages returned so far
	 */
	final int messages;
}