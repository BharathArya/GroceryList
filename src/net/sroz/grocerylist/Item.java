package net.sroz.grocerylist;

import android.database.Cursor;

public class Item {
	public long id;
	public String text;
	public boolean checked;
		
	public Item(Cursor c) {
		id = c.getLong(c.getColumnIndex(Provider.KEY_ROWID));
		text = c.getString(c.getColumnIndex(Provider.KEY_TEXT));
		checked = (c.getInt(c.getColumnIndex(Provider.KEY_CHECKED)) == 1);
	}
}
