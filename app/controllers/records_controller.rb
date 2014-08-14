require 'importio.rb'

class RecordsController < ApplicationController
  #constant to save the sport abbreviations needed to construct the league page
  #url for ESPN
  SPORTS_ABBREVIATIONS = {
    baseball: 'flb',
    basketball: 'fba',
    football: 'ffl'
  }

  def new
    @sport = Sport.find(params[:sport_id])
    @league = @sport.leagues.find(params[:league_id])
    @record = @league.records.new
    @league_years = @league.get_all_years
  end

  def create

    @sport = Sport.find(params[:sport_id])
    @league = @sport.leagues.find(params[:league_id])
    @new_records = []

    #defines the Importio API client
    client = Importio::new("b66ea9de-3b8e-497f-931a-91d25710d3b1", ENV["IMPORTIO_TOKEN"], "https://query.import.io")
    #create a blank array where the returned data will be stored
    data_rows = []

    #define callbacks that are sent with the API query
    callback = lambda do |query, message|
      if message["type"] == "DISCONNECT"
        puts "The query was cancelled as the client was disconnected"
      end
      if message["type"] == "MESSAGE"
        if message["data"].key?("errorType")
          puts "Got an error!"
          puts JSON.pretty_generate(message["data"])
        else
          puts "Got data!"
          puts JSON.pretty_generate(message["data"])
          data_rows << message["data"]["results"]
        end
      end
      if query.finished
        puts "Query finished"
      end
    end

    #connect to the API
    client.connect

    #dynamically build the request URL and make the request to the Importio API
    client.query({"input"=>{"webpage/url"=>"http://games.espn.go.com/#{SPORTS_ABBREVIATIONS[@sport.name.downcase.to_sym]}/standings?leagueId=#{@league.espn_id}&seasonId=#{record_params[:year]}"},"connectorGuids"=>["d5170dfc-fdd8-4227-8d19-446480420690"]}, callback )

    #join all data together
    client.join
    #disconnect from API
    client.disconnect
    #extract the actual JSON hash from the data_results
    importio_data_array = data_rows[0]

    #this is a beast. loop through each returned block of JSON and create new
    #teams and new records for that league. Teams validate on uniqueness so
    #this won't create a new team if it already exists in the database.
    importio_data_array.each do |row|
      @team = @league.teams.new
      @team.name = row['team/_title']
      @team.team_url = row['team/_source'].split('&seasonId')[0]

      @team.save

      @record = @league.records.new(record_params)
      @record.wins = row['wins/_source']
      @record.losses = row['losses/_source']
      @record.ties = row['ties/_source']
      @record.year = record_params[:year]
      @record.team_id = Team.find_by(team_url: row['team/_source'].split('&seasonId')[0]).id

      @new_records << @record
    end

    #save all those new records
    if @new_records.each { |record| record.save }
      redirect_to sport_league_path(@sport, @league), success: 'League records imported!'
    else
      render :new
    end
  end

  private

  def record_params
    params.require(:record).permit(:year)
  end
end
