class Team < ActiveRecord::Base
  belongs_to :league
  has_many :records
  has_many :players
  has_many :baseball_stats

  validates :name, :team_url, :league_id, presence: true
  validates :team_url, uniqueness: true
end
