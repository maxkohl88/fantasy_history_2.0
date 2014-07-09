package com.importio.api.clientlite.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.UUID;

import org.junit.Test;

import com.importio.api.clientlite.ImportIO;

/**
 * Test 1
 * 
 * Test that specifying incorrect username and password raises an exception
 * 
 * @author dev@import.io
 * @see https://github.com/import-io/importio-client-libs/tree/master/java
 */
public class IO1Test extends TestHelper {

	@Test
	public void test() {
		ImportIO client = new ImportIO();
		client.setHost(host);
		try {
			client.login(UUID.randomUUID().toString(), UUID.randomUUID().toString());
			// Should have thrown an IOException
			fail();
		} catch (IOException e) {
			assertEquals("Connect failed, status 401", e.getMessage());
		}
	}

}
