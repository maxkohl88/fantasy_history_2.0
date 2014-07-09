'''
import.io client library - test cases

This file contains tests that verify the functionality of the import.io Python client

Dependencies: Python 2.7

@author: dev@import.io
@source: https://github.com/import-io/importio-client-libs/tree/master/python
'''

import importio, latch, sys, uuid, time

# Retrieve the credentials from the command line
host = sys.argv[1]
username = sys.argv[2]
password = sys.argv[3]
userguid = sys.argv[4]
api_key = sys.argv[5]

'''
Test 1

Test that specifying incorrect username and password raises an exception
'''

client = importio.importio(host= "http://query." + host)

try:
	client.login(str(uuid.uuid4()), str(uuid.uuid4()), host = "https://api." + host)
	print "Test 1: Failed (did not throw exception)"
	sys.exit(1)
except Exception:
	print "Test 1: Success"

client.disconnect()

'''
Test 2

Test that providing an incorrect user GUID raises an exception
'''

client = importio.importio(host= "http://query." + host, user_id=str(uuid.uuid4()), api_key=api_key)

try:
	client.connect()
	print "Test 2: Failed (did not throw exception)"
	sys.exit(2)
except Exception:
	print "Test 2: Success"


'''
Test 3

Test that providing an incorrect API key raises an exception
'''

client = importio.importio(host= "http://query." + host, user_id=userguid, api_key=str(uuid.uuid4()))

try:
	client.connect()
	print "Test 3: Failed (did not throw exception)"
	sys.exit(3)
except Exception:
	print "Test 3: Success"


'''
Test 4

Test that querying a source that doesn't exist returns an error
'''

test4latch = latch.latch(1)
test4pass = False

def test4callback(query, message):
	global test4pass
	if message["type"] == "MESSAGE" and "errorType" in message["data"]:
		if message["data"]["errorType"] == "ConnectorNotFoundException":
			test4pass = True
		else:
			print "Unexpected error: %s" % message["data"]["errorType"]

	if query.finished(): test4latch.countdown()

client = importio.importio(host= "http://query." + host, user_id=userguid, api_key=api_key)
client.connect()
client.query({ "input":{ "query": "server" }, "connectorGuids": [ str(uuid.uuid4()) ] }, test4callback)

test4latch.await()
client.disconnect()

if not test4pass:
	print "Test 4: Failed (no/wrong error message)"
	sys.exit(4)
else:
	print "Test 4: Success"

'''
Test 5

Test that querying a source that returns an error is handled correctly
'''

test5latch = latch.latch(1)
test5pass = False

def test5callback(query, message):
	global test5pass

	if message["type"] == "MESSAGE" and "errorType" in message["data"]:
		if message["data"]["errorType"] == "UnauthorizedException":
			test5pass = True
		else:
			print "Unexpected error: %s" % message["data"]["errorType"]
	if query.finished(): test5latch.countdown()

client = importio.importio(host= "http://query." + host, user_id=userguid, api_key=api_key)
client.connect()
client.query({ "input":{ "query": "server" }, "connectorGuids": [ "eeba9430-bdf2-46c8-9dab-e1ca3c322339" ] }, test5callback)

test5latch.await()
client.disconnect()

if not test5pass:
	print "Test 5: Failed (no/wrong error message)"
	sys.exit(5)
else:
	print "Test 5: Success"

# Set up the expected data for the query tests
expectedData = [
	"Iron Man",
	"Captain America",
	"Hulk",
	"Thor",
	"Black Widow",
	"Hawkeye"
]

'''
Test 6

Tests querying a working source with user GUID and API key
'''

test6latch = latch.latch(1)
test6data = []
test6pass = True

def test6callback(query, message):
	global test6data

	if message["type"] == "MESSAGE":
		for result in message["data"]["results"]:
			test6data.append(result["name"])

	if query.finished(): test6latch.countdown()

client = importio.importio(host= "http://query." + host, user_id=userguid, api_key=api_key)
client.connect()
client.query({ "input":{ "query": "server" }, "connectorGuids": [ "1ac5de1d-cf28-4e8a-b56f-3c42a24b1ef2" ] }, test6callback)

test6latch.await()
client.disconnect()

for index, value in enumerate(test6data):
	if value != expectedData[index]:
		test6pass = False
		print "Test 6: Index %i does not match (%s, %s)" % (index, value, expectedData[index])

if not test6pass:
	print "Test 6: Failed (returned data did not match)"
	sys.exit(6)
else:
	print "Test 6: Success"

'''
Test 7

Tests querying a working source with username and password
'''

test7latch = latch.latch(1)
test7data = []
test7pass = True

def test7callback(query, message):
	global test7data

	if message["type"] == "MESSAGE":
		for result in message["data"]["results"]:
			test7data.append(result["name"])

	if query.finished(): test7latch.countdown()

client = importio.importio(host= "http://query." + host)
client.login(username, password, host = "https://api." + host)
client.query({ "input":{ "query": "server" }, "connectorGuids": [ "1ac5de1d-cf28-4e8a-b56f-3c42a24b1ef2" ] }, test7callback)

test7latch.await()
client.disconnect()

for index, value in enumerate(test7data):
	if value != expectedData[index]:
		test7pass = False
		print "Test 7: Index %i does not match (%s, %s)" % (index, value, expectedData[index])

if not test7pass:
	print "Test 7: Failed (returned data did not match)"
	sys.exit(7)
else:
	print "Test 7: Success"

'''
Test 8

Tests querying a working source twice, with a client ID change in the middle
'''

test8latch = latch.latch(1)
test8data = []
test8pass = True
test8disconnects = 0

def test8callback(query, message):
	global test8data, test8disconnects

	if message["type"] == "MESSAGE":
		for result in message["data"]["results"]:
			test8data.append(result["name"])
	if message["type"] == "DISCONNECT":
		test8disconnects = test8disconnects + 1

	if query.finished(): test8latch.countdown()

client = importio.importio(host= "http://query." + host, user_id=userguid, api_key=api_key)
client.connect()
client.query({ "input":{ "query": "server" }, "connectorGuids": [ "1ac5de1d-cf28-4e8a-b56f-3c42a24b1ef2" ] }, test8callback)

test8latch.await()

print "Modifying client_id of the library for testing purposes"

client.session.client_id = "random"
test8latch = latch.latch(1)

# This query will fail
try:
	client.query({ "input":{ "query": "server" }, "connectorGuids": [ "1ac5de1d-cf28-4e8a-b56f-3c42a24b1ef2" ] }, test8callback)
	print "Test 8: Failed (query which should have thrown did not)"
	sys.exit(8)
except:
	pass

client.query({ "input":{ "query": "server" }, "connectorGuids": [ "1ac5de1d-cf28-4e8a-b56f-3c42a24b1ef2" ] }, test8callback)
test8latch.await()
client.disconnect()

for index, value in enumerate(test8data):
	idx = index
	if idx > len(expectedData)-1:
		idx = idx - len(expectedData)
	if value != expectedData[idx]:
		test8pass = False
		print "Test 8: Index %i does not match (%s, %s)" % (idx, value, expectedData[idx])

if not test8pass:
	print "Test 8: Failed (returned data did not match)"
	sys.exit(8)
elif test8disconnects != 1:
	print "Test 8: Failed (wrong number of disconnects)"
	sys.exit(8)
else:
	print "Test 8: Success"
