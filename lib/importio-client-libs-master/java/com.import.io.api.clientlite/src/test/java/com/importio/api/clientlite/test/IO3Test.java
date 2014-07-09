package com.importio.api.clientlite.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.UUID;

import org.junit.Test;

import com.importio.api.clientlite.ImportIO;

/**
 * Test 3
 * 
 * Test that providing an incorrect API key raises an exception
 * 
 * @author dev@import.io
 * @see https://github.com/import-io/importio-client-libs/tree/master/java
 */
public class IO3Test extends TestHelper {

	@Test
	public void test() {
		ImportIO client = new ImportIO(UUID.fromString(userGuid), UUID.randomUUID().toString());
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
