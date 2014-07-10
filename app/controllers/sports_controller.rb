class SportsController < ApplicationController

  def index
    @sports = Sport.all
  end

  def show
    @sport = Sport.find(params[:id])
    @league = League.all
  end

end
