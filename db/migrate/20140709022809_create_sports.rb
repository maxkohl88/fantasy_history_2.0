class CreateSports < ActiveRecord::Migration
  def up
    create_table :sports do |t|
      t.text :name

      t.timestamps
    end
  end

  def down
    drop_table :sports
  end
end
