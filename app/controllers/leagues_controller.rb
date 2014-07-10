class LeaguesController < ApplicationController
  def index
    @leagues = League.all
    @sport = Sport.find(params[:sport_id])
  end

  def show
    @sport = Sport.find(params[:id])
    @league = League.find(params[:id])
  end

  def new
    @league = League.new
    @sports_options = Sport.all.map{ |sport| [sport.name, sport.id]}
  end

  def create
    @league = League.new(league_params)
    @sport = @league.sport_id

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
