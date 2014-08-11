class LeaguesController < ApplicationController
  def index
    @leagues = League.all
    @sport = Sport.find(params[:sport_id])
  end

  def show
    @sport = Sport.find(params[:sport_id])
    @league = @sport.leagues.includes(:records).find(params[:id])

    #find all the years for which this league has records for
    @league_years = @league.get_all_years.sort.reverse

    #find all of the data needed to populate the league standings table
    @year_results = @league.records.where(year: params[:year])

    #determine if the table will show composite record history or show records
    #for a specific year
    if (params.has_key?(:year))
    total_records = []
      @year_results.each do |record|
        hash = {}
        hash[:wins] = record.wins
        hash[:losses] = record.losses
        hash[:ties] = record.ties
        hash[:team_name] = record.team.name

        total_records << hash
      end
      @league_records = total_records
    else
      @league_records = @league.all_time_records_total
    end

    #sort the data heading to the table by total wins
    @league_records.sort!{ |x, y| x[:wins] <=> y[:wins] }

    #find all teams for this league
    @teams = @league.teams

    #initialize an empty array for the data heading to the league graph
    @table_values =[]

    #this method converts the historical league records into an optimal format
    #for passing to the league graph
    @teams.each do |team|
      yearly_data = []

      @league_years.each do |year|
        target_record = team.records.find_by(year: year)
        unless target_record.nil? || yearly_data << [year, target_record[:wins]]
        end
      end

      unless yearly_data.empty? || @table_values << {name: team.name, data: yearly_data}
      end
    end
  end

  def new
    @sport = Sport.find(params[:sport_id])
    @league = @sport.leagues.new

    @sports_options = Sport.all.map{ |sport| [sport.name, sport.id]}
  end

  def create
    @sport = Sport.find(params[:sport_id])
    @league = @sport.leagues.new(league_params)

    if @league.save
      redirect_to sport_leagues_path(@sport), notice: "You've successfully added your league!"
    else
      render :new
    end
  end

  private

  def league_params
    params.require(:league).permit(:name, :sport_id, :espn_id)
  end
end
