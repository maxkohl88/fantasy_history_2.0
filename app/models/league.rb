class League < ActiveRecord::Base
  belongs_to :sport
  has_many :records
  has_many :teams
  has_many :baseball_stats

  validates :sport_id, :espn_id, presence: true

  def all_time_records_total
    all_records = self.records
    team_ids = all_records.map{ |record| record.team_id }.uniq
    records_sum = []
    team_ids.each do |id|
      hash = {}
      total_wins = all_records.where(team_id: id).map(&:wins).reduce(0, &:+)
      total_losses = all_records.where(team_id: id).map(&:losses).reduce(0, &:+)
      total_ties = all_records.where(team_id: id).map(&:ties).reduce(0, &:+)
      team_name = Team.find(id).name

      hash[:wins] = total_wins
      hash[:losses] = total_losses
      hash[:ties] = total_ties
      hash[:team_name] = team_name

      records_sum << hash
    end
    records_sum
    end

  def get_all_years
    self.records.map{ |record| record.year }.uniq
  end

  def convert_to_hash
    total_records = []
    self.each do |record|
      hash = {}
      hash[:wins] = record.wins
      hash[:losses] = record.losses
      hash[:ties] = record.ties
      hash[:team_name] = record.team.name

      total_records << hash
    end
    total_records
  end
end
