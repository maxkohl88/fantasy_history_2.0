class CreateBaseballStats < ActiveRecord::Migration
  def up
    create_table :baseball_stats do |t|
      t.integer :year
      t.integer :games_played
      t.integer :runs
      t.integer :total_bases
      t.integer :rbis
      t.integer :walks
      t.integer :strikeouts
      t.integer :steals
      t.integer :total_points
      t.references :player
      t.references :league
      t.references :team

      t.timestamps
    end
  end

  def down
    drop_table :baseball_stats
  end
end
