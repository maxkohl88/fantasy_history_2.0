class LeaguesController < ApplicationController
  def index
    @leagues = League.all
  end

  def show
  end

  def new
    @league = League.new
  end

  def create
    @league = League.new(league_params)

    if @league.save
      redirect_to leagues_path, notice: "You've successfully added your league!"
    else
      render :new
    end
  end

  private

  def league_params
    params.require(:league).permit(:name)
  end
end
