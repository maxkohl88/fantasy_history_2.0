class AddTeamReferenceToRecords < ActiveRecord::Migration
  def up
    remove_column :records, :team_name
    remove_column :records, :team_url
    add_column :records, :team_id, :integer, references: :team, index: true
  end

  def down
    remove_column :records, :team_id
    add_column :records, :team_url, :string
    add_column :records, :team_name, :string
  end
end
