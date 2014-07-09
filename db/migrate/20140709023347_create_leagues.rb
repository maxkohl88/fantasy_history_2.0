class CreateLeagues < ActiveRecord::Migration
  def up
    create_table :leagues do |t|
      t.text :name
      t.references :sport, index: true

      t.timestamps
    end
  end

  def down
    drop_table :leagues
  end
end
