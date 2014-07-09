package com.importio.api.clientlite.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.UUID;

import org.junit.Test;

import com.importio.api.clientlite.ImportIO;

/**
 * Test 2
 * 
 * Test that providing an incorrect user GUID raises an exception
 * 
 * @author dev@import.io
 * @see https://github.com/import-io/importio-client-libs/tree/master/java
 */
public class IO2Test extends TestHelper {

	@Test
	public void test() {
		ImportIO client = new ImportIO(UUID.randomUUID(), apiKey);
		client.setHost(host);
		try {
			client.connect();
			// Should have thrown an IOException
			fail();
		} catch (IOException e) {
			assertEquals(true, e.getMessage().startsWith("Unable to connect to import.io"));
		}
	}

}
