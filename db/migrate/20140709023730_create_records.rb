class CreateRecords < ActiveRecord::Migration
  def change
    create_table :records do |t|
      t.date :year
      t.integer :wins
      t.integer :losses
      t.integer :ties
      t.boolean :championship
      t.references :league, index: true

      t.timestamp
    end
  end
end
