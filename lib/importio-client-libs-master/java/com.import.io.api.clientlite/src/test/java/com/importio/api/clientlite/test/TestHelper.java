package com.importio.api.clientlite.test;

import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;

/**
 * A test helper which allows us to use the same parameters for all of our tests
 * 
 * @author dev@import.io
 * @see https://github.com/import-io/importio-client-libs/tree/master/java
 */
public class TestHelper {

	/**
	 * Parameters that are passed through from the running script
	 */
	protected static String userGuid;
	protected static String apiKey;
	protected static String username;
	protected static String password;
	protected static String host;
	
	/**
	 * This is used as verification data in a couple of tests
	 */
	List<String> expectedNames = Arrays.asList(
		"Iron Man",
		"Captain America",
		"Hulk",
		"Thor",
		"Black Widow",
		"Hawkeye"
	);
	
	/**
	 * Before the test is run, read in the required system properties
	 * 
	 * @throws Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		host = System.getProperty("host");
		userGuid = System.getProperty("userGuid");
		apiKey = System.getProperty("apiKey");
		username = System.getProperty("username");
		password = System.getProperty("password");
	}

}
