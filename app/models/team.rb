class Team < ActiveRecord::Base
  belongs_to :league
  has_many :records
end
