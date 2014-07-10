class RecordsController < ApplicationController
  def new
    @sport = Sport.find(params[:sport_id])
    @league = @sport.leagues.find(params[:league_id])
    @record = @league.records.new
  end

  def create
    @sport = Sport.find(params[:sport_id])
    @league = @sport.leagues.find(params[:league_id])
    @record = @league.records.new

    if @record.save
      redirect_to sport_league_path(@sport, @league)
    else
      render :new
    end
  end
end
