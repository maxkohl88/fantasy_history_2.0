class AddTeamUrlToRecords < ActiveRecord::Migration
  def up
    add_column :records, :team_url, :text
  end

  def down
    delete_column :records, :team_url
  end
end
