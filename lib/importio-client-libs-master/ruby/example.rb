#
# import.io client library - example code
# 
# This file is an example for integrating with import.io using the Ruby client library
#
# Dependencies: Ruby 1.9, http-cookie
#
# @author: dev@import.io
# @source: https://github.com/import-io/importio-client-libs/tree/master/python
# 

require "./lib/importio.rb"
#require "importio"
require "json" 

# You have two choices for authenticating with the Ruby client: you can use your API key
# or your username and password. Username and password is quicker to get started with, but
# API key authentication will be more reliable for really large query volumes.
# If you need it, you can get YOUR_USER_GUID and YOUR_API_KEY from your account page, at
# http://import.io/data/account

# To use an API key for authentication, use the following code:
client = Importio::new("YOUR_USER_GUID", "YOUR_API_KEY")
# If you wish, you may configure HTTP proxies that ruby can use to connect
# to import.io. If you need to do this, uncomment the following line and fill in the
# correct details to specify an HTTP proxy:
#client.proxy("127.0.0.1", 3128)
# Once we have initialised the client, we need to connect it to the server:
client.connect


# If you wish to use username and password based authentication, first create a client:
#client = Importio::new
# Use the proxy command if you wish to use a proxy (must be between the new client and login lines
# if you are doing username/password auth):
#client.proxy("127.0.0.1", 3128)
# Next you need to log in to import.io using your username and password, like so:
#client.login("YOUR_USERNAME", "YOUR_PASSWORD")

# Define here a global variable that we can put all our results in to when they come back from
# the server, so we can use the data later on in the script
data_rows = []

# In order to receive the data from the queries we issue, we need to define a callback method
# This method will receive each message that comes back from the queries, and we can take that
# data and store it for use in our app
callback = lambda do |query, message|
  # Disconnect messages happen if we disconnect the client library while a query is in progress
  if message["type"] == "DISCONNECT"
    puts "The query was cancelled as the client was disconnected"
  end
  if message["type"] == "MESSAGE"
    if message["data"].key?("errorType")
      # In this case, we received a message, but it was an error from the external service
      puts "Got an error!"
      puts JSON.pretty_generate(message["data"])
  	else
      # We got a message and it was not an error, so we can process the data
      puts "Got data!"
      puts JSON.pretty_generate(message["data"])
      # Save the data we got in our dataRows variable for later
      data_rows << message["data"]["results"]
    end
  end
  if query.finished
    puts "Query finished"
  end
end

# Issue three queries to the same data source with different inputs
# You can modify the inputs and connectorGuids so as to query your own sources
# To find out more, visit the integrate page at http://import.io/data/integrate/#ruby
client.query({"input"=>{"query"=>"server"},"connectorGuids"=>["39df3fe4-c716-478b-9b80-bdbee43bfbde"]}, callback)
client.query({"input"=>{"query"=>"ubuntu"},"connectorGuids"=>["39df3fe4-c716-478b-9b80-bdbee43bfbde"]}, callback)
client.query({"input"=>{"query"=>"clocks"},"connectorGuids"=>["39df3fe4-c716-478b-9b80-bdbee43bfbde"]}, callback)

puts "Queries dispatched, now waiting for results"

# Now we have issued all of the queries, we can wait for all of the threads to complete meaning the queries are done
client.join

puts "Join completed, all results returned"

# It is best practice to disconnect when you are finished sending queries and getting data - it allows us to
# clean up resources on the client and the server
client.disconnect

# Now we can print out the data we got
puts "All data received:"
puts JSON.pretty_generate(data_rows)