class Sport < ActiveRecord::Base
  belongs_to :user
  has_many :leagues

  validates :name, presence: true, uniqueness: { case_sensitive: false }
end
