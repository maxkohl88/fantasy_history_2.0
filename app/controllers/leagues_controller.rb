class LeaguesController < ApplicationController
  def index
    @leagues = League.all
    @sport = Sport.find(params[:sport_id])
  end

  def show
    @sport = Sport.find(params[:sport_id])
    @league = @sport.leagues.find(params[:id])

    @league_years = @league.get_all_years.sort.reverse

    @year_results = @league.records.where(year: params[:year])

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

    @league_records.sort!{ |x, y| x[:wins] <=> y[:wins] }

    @teams = @league.teams

    @table_values =[]

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
