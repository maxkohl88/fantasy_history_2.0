require "./ruby/importio.rb"
require "json"
require 'pry'

# To use an API key for authentication, use the following code:
client = Importio::new("b66ea9de-3b8e-497f-931a-91d25710d3b1","v/S9sPDS/kPHTbxtzA8F9l6SKg4x1PDkWeNXDOxYPBbIgWlawN5xrodjtnds6mgQCS6g8AFS9AKPoTaNM9kI1Q==", "https://query.import.io")
# binding.pry
# Once we have started the client and authenticated, we need to connect it to the server:

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
client.connect

# Issue queries to your data sources with your specified inputs
# You can modify the inputs and connectorGuids so as to query your own sources
# Query for tile People's League Season Standings
client.query({"input"=>{"webpage/url"=>"http://games.espn.go.com/flb/standings?leagueId=140424&seasonId=2014"},"connectorGuids"=>["d5170dfc-fdd8-4227-8d19-446480420690"]}, callback )

puts "Queries dispatched, now waiting for results"

# Now we have issued all of the queries, we can wait for all of the threads to complete meaning the queries are done
client.join

puts "Join completed, all results returned"

# It is best practice to disconnect when you are finished sending queries and getting data - it allows us to
# clean up resources on the client and the server
client.disconnect

# Now we can print out the data we got
puts "All data received:"
results = JSON.pretty_generate(data_rows)

binding.pry
