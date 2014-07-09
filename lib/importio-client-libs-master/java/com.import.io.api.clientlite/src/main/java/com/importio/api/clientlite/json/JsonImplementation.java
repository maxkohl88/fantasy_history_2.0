package com.importio.api.clientlite.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import com.importio.api.clientlite.data.RequestMessage;
import com.importio.api.clientlite.data.ResponseMessage;

/**
 * An interface for providing JSON parsing implementations to the import.io client
 * 
 * @author dev@import.io
 * @see https://github.com/import-io/importio-client-libs/tree/master/java
 */
public interface JsonImplementation {

	/**
	 * Takes a list of CometD messages and serializes them on to the OutputStream
	 * 
	 * @param outputStream
	 * @param data
	 * @throws IOException
	 */
	void writeRequest(OutputStream outputStream, List<RequestMessage> data) throws IOException;

	/**
	 * Reads a list of responses from the InputStream
	 * 
	 * @param inputStream
	 * @return
	 * @throws IOException
	 */
	List<ResponseMessage> readResponse(InputStream inputStream) throws IOException;

}
