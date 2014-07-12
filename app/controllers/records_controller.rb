require 'importio.rb'

class RecordsController < ApplicationController
  SPORTS_ABBREVIATIONS = {
    baseball: 'flb',
    basketball: 'fba',
    football: 'ffl'
  }

  def new
    @sport = Sport.find(params[:sport_id])
    @league = @sport.leagues.find(params[:league_id])
    @record = @league.records.new
  end

  def create

    @sport = Sport.find(params[:sport_id])
    @league = @sport.leagues.find(params[:league_id])
    @new_records = []

    client = Importio::new("b66ea9de-3b8e-497f-931a-91d25710d3b1","v/S9sPDS/kPHTbxtzA8F9l6SKg4x1PDkWeNXDOxYPBbIgWlawN5xrodjtnds6mgQCS6g8AFS9AKPoTaNM9kI1Q==", "https://query.import.io")

    data_rows = []

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
    client.connect

    client.query({"input"=>{"webpage/url"=>"http://games.espn.go.com/#{SPORTS_ABBREVIATIONS[@sport.name.downcase.to_sym]}/standings?leagueId=#{@league.espn_id}&seasonId=#{record_params[:year]}"},"connectorGuids"=>["d5170dfc-fdd8-4227-8d19-446480420690"]}, callback )

    client.join
    client.disconnect

    importio_data_array = data_rows[0]

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

      # binding.pry
    end

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
