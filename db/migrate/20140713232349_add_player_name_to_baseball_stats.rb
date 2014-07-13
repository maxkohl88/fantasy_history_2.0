class AddPlayerNameToBaseballStats < ActiveRecord::Migration
  def up
    add_column :baseball_stats, :player_name, :string
  end

  def down
    remove_column :baseball_stats, :player_name
  end
end
