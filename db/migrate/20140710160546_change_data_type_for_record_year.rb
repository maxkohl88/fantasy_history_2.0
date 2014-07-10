class ChangeDataTypeForRecordYear < ActiveRecord::Migration
  def up
    remove_column :records, :year

    add_column :records, :year, :integer
  end

  def down
    remove_column :records, :year

    add_column :records, :year, :date
  end
end
