package net.sroz.grocerylist;

import net.sroz.grocerylist.R;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;


public class CurrentListActivity extends Activity {
	AutoCompleteTextView tvNewItem;
	Button btnAddItem;
	ListView lvItems;
	LayoutInflater mFactory;
	Cursor mCursor;
	
	static final int DIALOG_LIST_ID = 0;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        tvNewItem = (AutoCompleteTextView)findViewById(R.id.tvNewItem);
        btnAddItem = (Button)findViewById(R.id.btnAddItem);
        lvItems = (ListView)findViewById(R.id.lvItems);
        
        mFactory = LayoutInflater.from(this);
        mCursor = getContentResolver().query(Provider.CONTENT_URI, Provider.ITEMS_QUERY_COLUMNS, null, null, Provider.DEFAULT_SORT_ORDER);
        
        lvItems.setAdapter(new GroceryListAdapter(this, mCursor));
    	lvItems.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				CheckedTextView cv = (CheckedTextView) view;
				boolean isChecked = !cv.isChecked();
				CurrentListActivity.toggle_item(getApplicationContext(), id, isChecked);
			}
		});
    	lvItems.setOnItemLongClickListener(new OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				/* TODO Show context menu on long click */
				CurrentListActivity.delete_item(getApplicationContext(), id);
				return true;
			}
    	});
        
        btnAddItem.setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			CurrentListActivity.add_item(getApplicationContext(), tvNewItem.getText().toString());
    			tvNewItem.setText("");
    		}
        });
        
        tvNewItem.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
					// XXX This should be moved to it's own method in the Activity
	    			CurrentListActivity.add_item(getApplicationContext(), tvNewItem.getText().toString());
	    			tvNewItem.setText("");
					return true;
				}
				return false;
			}
		});
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	getMenuInflater().inflate(R.menu.main, menu);
    	return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	boolean haveItems = (CurrentListActivity.get_item_count(getApplicationContext()) > 0);
		menu.findItem(R.id.checkout).setEnabled(haveItems);
		menu.findItem(R.id.clear).setEnabled(haveItems);
    	menu.findItem(R.id.settings).setEnabled(false);
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	boolean ret = false;
    	int id = item.getItemId();
    	switch (id) {
	    	case R.id.checkout:
	    		delete_checked_items(getApplicationContext());
	    		ret = true;
	    		break;
	    	case R.id.clear:
	    		delete_all_items(getApplicationContext());
	    		ret = true;
	    		break;
	    	case R.id.settings:
	    		Toast.makeText(getApplicationContext(), "Not yet implemented", Toast.LENGTH_SHORT).show();
	    		ret = true;
	    		break;
	    	case R.id.list:
	    		showDialog(DIALOG_LIST_ID);
	    		break;
    		default:
    			ret = super.onOptionsItemSelected(item);
    	}
    	return ret;
    }
    
    @Override
    public Dialog onCreateDialog(int id) {
    	Dialog dialog = null;
    	switch (id) {
    		case DIALOG_LIST_ID:
    			//TODO: Build list selection dialog
    			break;
    		default:
    			break;
    	}
    	return dialog;
    }
    
    
	private class GroceryListAdapter extends CursorAdapter {
		public GroceryListAdapter(Context context, Cursor c) {
			super(context, c);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return mFactory.inflate(android.R.layout.simple_list_item_multiple_choice, parent, false);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			final Item item = new Item(cursor);
			CheckedTextView cv = (CheckedTextView) view;
			cv.setText(item.text);
			cv.setChecked(item.checked);
		}
	}
	
	public static Item get_item(ContentResolver resolver, long id) {
		Uri uri = ContentUris.withAppendedId(Provider.CONTENT_URI, id);
		Cursor c = resolver.query(uri, Provider.ITEMS_QUERY_COLUMNS, null, null, null);
		Item item = null;
		if (c != null) {
			if (c.moveToFirst()) {
				item = new Item(c);
			}
			c.close();
		}
		return item;
	}
	
    public static int get_item_count(Context c) {
		Cursor cursor = null;
		int count = 0;
		cursor = c.getContentResolver().query(Provider.CONTENT_URI, new String[] {"COUNT(_ID) as count"}, null, null, null);
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				cursor.moveToFirst();
				count = cursor.getInt(cursor.getColumnIndex("count"));
			}
		}
		return count; 
	}

	public static void add_item(Context c, String text) {
		if (TextUtils.isEmpty(text))
			return;
		Item item = find_item(c, text);
		if (item != null) {
			Toast.makeText(c, "Item already in list", Toast.LENGTH_SHORT).show();
			return;
		}
		ContentValues values = new ContentValues(1);
		values.put(Provider.KEY_TEXT, text);
		c.getContentResolver().insert(Provider.CONTENT_URI, values);	
	}

	private static Item find_item(Context c, String text) {
		Item item = null;
		Cursor cursor = null;
		cursor = c.getContentResolver().query(Provider.CONTENT_URI, Provider.ITEMS_QUERY_COLUMNS, Provider.KEY_TEXT+"=?", new String[] {text}, null);
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				cursor.moveToFirst();
				item = new Item(cursor);
			}
		}
		return item;
	}

	public static void delete_item(Context c, long id) {
		Uri uri = ContentUris.withAppendedId(Provider.CONTENT_URI, id);
		c.getContentResolver().delete(uri, null, null);
	}
	
	public static void delete_all_items(Context c) {
		c.getContentResolver().delete(Provider.CONTENT_URI, null, null);
	}
	
	public static void delete_checked_items(Context c) {
		c.getContentResolver().delete(Provider.CONTENT_URI, Provider.KEY_CHECKED + "=?", new String[] {Integer.toString(1)});
	}

	public static void toggle_item(Context c, long id, boolean isChecked) {
		ContentValues values = new ContentValues(1);
		values.put(Provider.KEY_CHECKED, isChecked ? 1 : 0);
		
		Uri uri = ContentUris.withAppendedId(Provider.CONTENT_URI, id);
		c.getContentResolver().update(uri, values, null, null);
	}
	

}
