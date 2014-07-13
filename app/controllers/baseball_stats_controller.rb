require 'importio.rb'

class BaseballStatsController < ApplicationController
  SPORTS_ABBREVIATIONS = {
    baseball: 'flb',
    basketball: 'fba',
    football: 'ffl'
  }

  def new
    @sport = Sport.find(params[:sport_id])
    @league = @sport.leagues.find(params[:league_id])
    @baseball_stat = @league.baseball_stats.new

    @team_options = @league.teams.all.map { |team| [team.name, team.id]}
  end

  def create
    @sport = Sport.find(params[:sport_id])
    @league = @sport.leagues.find(params[:league_id])
    stat_reference = params[:baseball_stat]
    @team = @league.teams.find(stat_reference["team_id"].to_i)
    @team_espn_id = @team.team_url.split('&teamId=')[1].to_i
    @new_stats = []

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

    client.query({"input"=>{"webpage/url"=>"http://games.espn.go.com/#{SPORTS_ABBREVIATIONS[@sport.name.downcase.to_sym]}/activestats?leagueId=#{@league.espn_id}&seasonId=#{stat_params[:year]}&teamId=#{@team_espn_id}&filter=1"},"connectorGuids"=>["8b8eb655-011c-42c2-a4ff-1d95fa62cdbb"]}, callback )

    client.join
    client.disconnect



    importio_data_array = data_rows[0]

    # importio_data_array.each do |row|
    #   @player = @team.players.new
    #   @player.name = row['batters']
    #   @player.league_id = @league.id

    #   @player.save
    # end

    importio_data_array.each do |row|
      unless row['batters'].nil?
        @stat = @team.baseball_stats.new(stat_params)
        @stat.games_played = row['games_played/_source']
        @stat.runs = row['runs/_source']
        @stat.total_bases = row['total_bases/_source']
        @stat.rbis = row['rbis/_source']
        @stat.walks = row['walks/_source']
        @stat.strikeouts = row['strikeouts/_source']
        @stat.steals = row['steals/_source']
        @stat.total_points = row['total_points/_source']
        @stat.year = stat_params[:year]
        @stat.team_id = @team.id
        # @stat.player_id = Player.find_by(name: row['batters']).id
        @stat.league_id = @league.id
        @stat.player_name = row['batters']
        @stat.save
      end
    end

    # if @new_stats.each { |stat| stat.save }
    #   redirect_to sport_league_path(@sport, @league), success: 'Team stats imported!'
    # else
    #   render :new
    # end
  end

  def index

  end

  private

  def stat_params
    params.require(:baseball_stat).permit(:year, :team_id)
  end
end
