package com.importio.api.clientlite.test;

import java.util.UUID;

import com.importio.api.clientlite.ImportIO;

public class TestLibrary extends ImportIO {

	public TestLibrary(UUID userId, String apiKey) {
		super(userId, apiKey);
	}

	public void setTestClientId(String n) {
		this.setClientId(n);
	}

}
