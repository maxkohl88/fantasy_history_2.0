package com.importio.api.clientlite.data;


import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

/**
 * Encapsulates all of the details to issue a query to the import.io platform
 * 
 * @author dev@import.io
 * @see https://github.com/import-io/importio-client-libs/tree/master/java
 */
@Data
@Accessors(chain=true)
@FieldDefaults(level=AccessLevel.PRIVATE)
public class Query {
	
	/**
	 * Enumerates the valid formats to expect data back from the API in
	 */
	public static enum Format { JSON, HTML, XML }
	
	/**
	 * This is an ID for the query, which does not need to be set to issue it
	 */
	UUID guid;
	
	/**
	 * We have a request ID here to allow us to link result messages back to the
	 * original query object
	 */
	String requestId;
	
	/**
	 * List of GUIDs of the connectors that we want to federate this query to
	 */
	List<UUID> connectorGuids;
	
	/**
	 * The input parameters and what their values are for this query. Each input name is a
	 * key in this map, and the value is what will be passed on to the source being queried 
	 */
	Map<String, Object> input;
	
	/**
	 * Optionally specify the maximum number of pages to return per connector GUID in this
	 * query. The server will impose a maximum and a default value for this parameter.
	 */
	Integer maxPages;
	
	/**
	 * Which page of results to start at. Not all sources support starting at a page
	 * other than the first one.
	 */
	Integer startPage;
	
	/**
	 * By setting this to true, return the result data as hierarchical objects rather than
	 * a flat key-value map.
	 */
	boolean asObjects;
	
	/**
	 * Currently the APIs ignore the value of this parameter
	 */
	@Deprecated
	boolean returningSource;
	
	/**
	 * Which data format to use. We use JSON as we have the {@see JsonImplementation} to convert the data for us
	 * @see Query.Format
	 */
	Format format = Format.JSON;

}
