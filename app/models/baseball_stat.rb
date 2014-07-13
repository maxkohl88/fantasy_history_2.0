class BaseballStat < ActiveRecord::Base
  belongs_to :player
  belongs_to :league

  validates :name, :total_points, presence: true
end
