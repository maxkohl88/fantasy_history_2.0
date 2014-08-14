require 'importio.rb'

class RecordsController < ApplicationController

  def new
    @sport = Sport.find(params[:sport_id])
    @league = @sport.leagues.find(params[:league_id])
    @record = @league.records.new
    @league_years = @league.get_all_years
  end

  def create
    @sport = Sport.find(params[:sport_id])
    @league = @sport.leagues.find(params[:league_id])

    Record.import_from_importio(@sport, @league, record_params)
    redirect_to sport_league_path(@sport, @league)
  end

  private

  def record_params
    params.require(:record).permit(:year)
  end
end
