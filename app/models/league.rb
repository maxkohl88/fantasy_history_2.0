class League < ActiveRecord::Base
  belongs_to :sport
  has_many :records
  has_many :teams

  validates :sport_id, :espn_id, presence: true

  def all_time_records_total
    all_records = self.records
    team_ids = all_records.map{ |record| record.team_id }.uniq
    records_sum = []
    team_ids.each do |id|
      total_wins = all_records.where(team_id: id).map(&:wins).reduce(0, &:+)
      total_losses = all_records.where(team_id: id).map(&:losses).reduce(0, &:+)
      total_ties = all_records.where(team_id: id).map(&:ties).reduce(0, &:+)
      team_name = Team.find(id).name

      records_sum << [total_wins, total_losses, total_ties, team_name]
    end
    records_sum
  end

  def get_all_years
    self.records.map{ |record| record.year }.uniq
  end
end
