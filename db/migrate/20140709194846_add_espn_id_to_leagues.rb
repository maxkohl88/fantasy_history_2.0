class AddEspnIdToLeagues < ActiveRecord::Migration
  def up
    add_column :leagues, :espn_id, :integer
  end

  def down
    remove_column :leagues, :espn_id
  end
end
