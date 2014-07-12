class Team < ActiveRecord::Base
  belongs_to :league
  has_many :records

  validates :name, :team_url, :league_id, presence: true
end
