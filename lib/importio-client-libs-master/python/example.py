'''
import.io client library - example code

This file is an example for integrating with import.io using the Python client library

Dependencies: Python 2.7, importio.py, latch.py (both included in client bundle)

@author: dev@import.io
@source: https://github.com/import-io/importio-client-libs/tree/master/python
'''

import logging, json, importio, latch

# You do not need to do this, but setting the logging level will reveal logs about
# what the import.io client is doing and will surface more information on errors
logging.basicConfig(level=logging.INFO)

# If you wish, you may configure HTTP proxies that Python can use to connect
# to import.io. If you need to do this, uncomment the following line and fill in the
# correct details to specify an HTTP proxy:

#proxies = { "http": "127.0.0.1:3128" }

# Then you can use the "proxies" variable when instanciating a new client library object
# For more details on this see below

# You have two choices for authenticating with the Python client: you can use your API key
# or your username and password. Username and password is quicker to get started with, but
# API key authentication will be more reliable for really large query volumes.
# If you need it, you can get YOUR_USER_GUID and YOUR_API_KEY from your account page, at
# http://import.io/data/account

# To use an API key for authentication, use the following code to initialise the library
client = importio.importio(user_id="YOUR_USER_GUID", api_key="YOUR_API_KEY")
# If you want to use the client library with API keys and proxies, use this command:
#client = importio.importio(user_id="YOUR_USER_GUID", api_key="YOUR_API_KEY", proxies=proxies)

# Once you have initialised the client, connect it to the server:
client.connect()

# If you wish to use username and password based authentication, first create a client:
#client = importio.importio()
# If you wish to use proxies with your username and password, then you can do so like this:
#client = importio.importio(proxies=proxies)

# Next you need to log in to import.io using your username and password, like so:
#client.login("YOUR_USERNAME", "YOUR_PASSWORD")

# Because import.io queries are asynchronous, for this simple script we will use a "latch"
# to stop the script from exiting before all of our queries are returned
# For more information on the latch class, see the latch.py file included in this client library
queryLatch = latch.latch(3)

# Define here a global variable that we can put all our results in to when they come back from
# the server, so we can use the data later on in the script
dataRows = []

# In order to receive the data from the queries we issue, we need to define a callback method
# This method will receive each message that comes back from the queries, and we can take that
# data and store it for use in our app
def callback(query, message):
    global dataRows
    
    # Disconnect messages happen if we disconnect the client library while a query is in progress
    if message["type"] == "DISCONNECT":
        print "Query in progress when library disconnected"
        print json.dumps(message["data"], indent = 4)

    # Check the message we receive actually has some data in it
    if message["type"] == "MESSAGE":
        if "errorType" in message["data"]:
            # In this case, we received a message, but it was an error from the external service
            print "Got an error!" 
            print json.dumps(message["data"], indent = 4)
        else:
            # We got a message and it was not an error, so we can process the data
            print "Got data!"
            print json.dumps(message["data"], indent = 4)
            # Save the data we got in our dataRows variable for later
            dataRows.extend(message["data"]["results"])
    
    # When the query is finished, countdown the latch so the program can continue when everything is done
    if query.finished(): queryLatch.countdown()

# Issue three queries to the same data source with different inputs
# You can modify the inputs and connectorGuids so as to query your own sources
# To find out more, visit the integrate page at http://import.io/data/integrate/#python
client.query({"input":{ "query": "server" },"connectorGuids": [ "39df3fe4-c716-478b-9b80-bdbee43bfbde" ]}, callback)
client.query({"input":{ "query": "ubuntu" },"connectorGuids": [ "39df3fe4-c716-478b-9b80-bdbee43bfbde" ]}, callback)
client.query({"input":{ "query": "clocks" },"connectorGuids": [ "39df3fe4-c716-478b-9b80-bdbee43bfbde" ]}, callback)

print "Queries dispatched, now waiting for results"

# Now we have issued all of the queries, we can "await" on the latch so that we know when it is all done
queryLatch.await()

print "Latch has completed, all results returned"

# It is best practice to disconnect when you are finished sending queries and getting data - it allows us to
# clean up resources on the client and the server
client.disconnect()

# Now we can print out the data we got
print "All data received:"
print json.dumps(dataRows, indent = 4)