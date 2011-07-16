package net.sroz.grocerylist;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class Provider extends ContentProvider {
	private static final int ITEMS = 1;
	private static final int ITEMS_ID = 2;
	private static final int LISTS = 3;
	private static final int LISTS_ID = 4;
	
	public static final String KEY_ROWID = "_id";
	public static final String KEY_TEXT = "Item";
	public static final String KEY_CHECKED = "Checked";
	public static final String KEY_LIST_ID = "List_id";
	public static final String KEY_LIST = "List";
	public static final Uri CONTENT_URI =
        Uri.parse("content://net.sroz.grocerylist/item");
	static final String[] ITEMS_QUERY_COLUMNS = {
		KEY_ROWID,
		KEY_TEXT,
		KEY_CHECKED,
		KEY_LIST_ID
	};
	static final String[] LISTS_QUERY_COLUMS = {
		KEY_ROWID,
		KEY_LIST
	};
	public static final String DEFAULT_SORT_ORDER =
		KEY_TEXT + " ASC";
	
	private static final String TAG = "GroceryList";
	
	public static final String DATABASE_NAME = "GroceryList.db";
	public static final String DATABASE_ITEMS_TABLE = "tblItems";
	public static final String DATABASE_LISTS_TABLE = "tblLists";
	public static final int DATABASE_VERSION = 2;
	
	private static final String DATABASE_CREATE =
		"CREATE TABLE " + DATABASE_ITEMS_TABLE + " ("
		+ KEY_ROWID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
		+ KEY_TEXT + " TEXT NOT NULL, "
		+ KEY_CHECKED + " INTEGER NOT NULL, "
		+ KEY_LIST_ID + " INTEGER NOT NULL"
		+ ");"
		+ "CREATE TABLE " + DATABASE_LISTS_TABLE + "("
		+ KEY_ROWID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
		+ KEY_LIST + " TEXT NOT NULL"
		+ ");"
		+ "INSERT INTO " + DATABASE_LISTS_TABLE
		+ " (" + KEY_LIST + ") VALUES ('Groceries');";
	
	private DatabaseHelper DBHelper;
	private static final UriMatcher sURLMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	
	static {
		sURLMatcher.addURI("net.sroz.grocerylist", "list", LISTS);
		sURLMatcher.addURI("net.sroz.grocerylist", "list/#", LISTS_ID);
		sURLMatcher.addURI("net.sroz.grocerylist", "list/#/item", ITEMS);
		sURLMatcher.addURI("net.sroz.grocerylist", "list/#/item/#", ITEMS_ID);
	}
	
	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context c) {
			super(c, DATABASE_NAME, null, DATABASE_VERSION);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", attempting lossless upgrade...");
			int curVersion = oldVersion;
			while (curVersion < newVersion) {
				switch (curVersion) {
					case 1:
						try {
							onCreate(db);
							db.execSQL("ALTER TABLE tblGroceries ADD COLUMN " + KEY_LIST_ID + " DEFAULT 0");
							db.execSQL("INSERT INTO " + DATABASE_ITEMS_TABLE + " SELECT * FROM tblGroceries");
							db.execSQL("DROP TABLE tblGroceries");
						} catch(Exception e) {
							db.execSQL(
									"DROP TABLE IF EXISTS tblItems;"
									+ "DROP TABLE IF EXISTS tblLists;"
									+ "DROP TABLE IF EXISTS tblGroeries;"
									);
							onCreate(db);
						}
						break;
					default:
						Log.e(TAG, "Unexpected database version number (" + curVersion +")");
						Log.w(TAG, "Unable to upgrade database to version " + curVersion + ": Destroying previous data");
						db.execSQL("select 'drop table ' || name || ';' from sqlite_master where type = 'table';");
						onCreate(db);
						break;
				}
				curVersion++;
			}
		}
	}
	
	public Provider() {
		// Nothing to do
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = DBHelper.getWritableDatabase();
		int count = 0;
		long item_id = 0;
		long list_id = 0;
		String segment = "";
		switch (sURLMatcher.match(uri)) {
			case ITEMS:
				// Delete whatever the caller has selected
				count = db.delete(DATABASE_ITEMS_TABLE, selection, selectionArgs);
				break;
			case ITEMS_ID:
				segment = uri.getPathSegments().get(1);
				item_id = Long.parseLong(segment);
				segment = uri.getPathSegments().get(3);
				item_id = Long.parseLong(segment);
				if (TextUtils.isEmpty(selection)) {
					selection = KEY_ROWID + "=" + item_id + " AND " + KEY_LIST_ID + "=" + list_id;
				} else {
					selection = KEY_ROWID + "=" + item_id + " AND " + KEY_LIST_ID + "=" + list_id + " AND (" + selection + ")";
				}
				count = db.delete(DATABASE_ITEMS_TABLE, selection, selectionArgs);
				break;
			case LISTS:
				count = db.delete(DATABASE_LISTS_TABLE, selection, selectionArgs);
				break;
			case LISTS_ID:
				segment = uri.getPathSegments().get(1);
				list_id = Long.parseLong(segment);
				
				if (TextUtils.isEmpty(selection)) {
					selection = KEY_LIST_ID + "=" + list_id;
				} else {
					selection += KEY_LIST_ID + "=" + list_id + " AND (" + selection + ")";
				}
				count = db.delete(DATABASE_LISTS_TABLE, selection, selectionArgs);
				break;
			default:
				throw new IllegalArgumentException("Cannot delete from URI: " + uri);
		}
		
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public String getType(Uri uri) {
		switch (sURLMatcher.match(uri)) {
			case ITEMS:
				return "vnd.sroz.cursor.dir/items";
			case ITEMS_ID:
				return "vnd.sroz.cursor.item/items";
			case LISTS:
				return "vnd.sroz.cursor.dir/lists";
			case LISTS_ID:
				return "vnd.sroz.cursor.item/lists";
			default:
				throw new IllegalArgumentException("Unknown URI");
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		if (sURLMatcher.match(uri) != ITEMS)
			throw new IllegalArgumentException("Cannot insert into URI: " + uri);
		
		ContentValues values;
		if (initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}
		
		if (!values.containsKey(KEY_TEXT))
			throw new IllegalArgumentException("Missing required key: KEY_ITEM");
		
		if (!values.containsKey(KEY_LIST))
			throw new IllegalArgumentException("Missing required key: KEY_LIST");
		
		if (!values.containsKey(KEY_CHECKED))
			values.put(KEY_CHECKED, false); // New items are created unchecked
		
		
		SQLiteDatabase db = DBHelper.getWritableDatabase();
		long rowId = db.insert(DATABASE_ITEMS_TABLE, null, values);
		if (rowId < 0)
			throw new SQLException("Failed to insert row into " + uri);
		Uri newUri = ContentUris.withAppendedId(CONTENT_URI, rowId);
		getContext().getContentResolver().notifyChange(newUri, null);
		return newUri;
	}

	@Override
	public boolean onCreate() {
		DBHelper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,	String[] selectionArgs, String sortOrder) {
		SQLiteDatabase db = DBHelper.getReadableDatabase();
		long rowId = 0;
		String segment = "";
		Cursor cursor = null;
		switch (sURLMatcher.match(uri)) {
			case ITEMS:
				// Nothing to add, query is over all items
				break;
			case ITEMS_ID:
				// Query is on a specific item
				segment = uri.getPathSegments().get(1);
				rowId = Long.parseLong(segment);
				if (TextUtils.isEmpty(selection)) {
					selection = KEY_ROWID + "=" + rowId;
				} else {
					// And include whatever selection the caller gave
					selection = KEY_ROWID + "=" + rowId + " AND (" + selection + ")";
				}
				break;
			default:
				throw new IllegalArgumentException("Cannot delete from URI: " + uri);
		}
		cursor = db.query(DATABASE_ITEMS_TABLE, projection, selection, selectionArgs, null, null, null);
		if (cursor != null) {
			cursor.setNotificationUri(getContext().getContentResolver(), uri);
		}
		return cursor;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		int count;
		long rowId = 0;
		int match = sURLMatcher.match(uri);
		String segment = "";
		SQLiteDatabase db = DBHelper.getWritableDatabase();
		switch (match) {
			case ITEMS_ID:
				segment = uri.getPathSegments().get(1);
				rowId = Long.parseLong(segment);
				count = db.update(DATABASE_ITEMS_TABLE, values, KEY_ROWID + "=?", new String[] {Long.toString(rowId)});
				break;
			default:
				throw new UnsupportedOperationException("Cannot update URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}
}
