class AddTeamNameToRecords < ActiveRecord::Migration
  def up
    add_column :records, :team_name, :string, index: true
  end

  def down
    remove_column :records, :team_name
  end
end
