class CreateTeams < ActiveRecord::Migration
  def up
    create_table :teams do |t|
      t.string :name
      t.string :team_url
      t.references :league, index: true

      t.timestamps
    end
  end

  def down
    remove_table :teams
  end
end
