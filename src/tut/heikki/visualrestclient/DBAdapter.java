package tut.heikki.visualrestclient;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBAdapter {

	public static final String KEY_ROWID = "_id";
    public static final String KEY_NAME = "name";
    public static final String KEY_FILEDATE = "filedate";
    public static final String KEY_SIZE = "size";
    public static final String KEY_FILETYPE = "filetype";
    public static final String KEY_PATH = "path";
    public static final String KEY_BLOB_HASH = "blob_hash";
    public static final String KEY_STATUS = "status";
    public static final String KEY_THUMB_UPLOADED = "thumb_uploaded";
    public static final String KEY_COMMIT_HASH = "commit_hash";
    private static final String TAG = "DBAdapter";
    private static final String DATABASE_NAME = "VisualrestDB";
    private static final String DATABASE_TABLE = "files";
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_CREATE =
        "create table files (_id integer primary key autoincrement, "
        + "name text not null, filedate text not null, "
        + "size integer not null, filetype text not null, "
        + "path text not null, blob_hash text not null, "
        + "status text not null, thumb_uploaded integer, commit_hash text not null);";
    private final Context context;
    private DatabaseHelper DBHelper;
    private SQLiteDatabase db;
    public DBAdapter(Context ctx)
    {
        this.context = ctx;
        DBHelper = new DatabaseHelper(context);
    }
    private static class DatabaseHelper extends SQLiteOpenHelper
    {
        DatabaseHelper(Context context)
        {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }
        @Override
        public void onCreate(SQLiteDatabase db)
        {
            try {
                db.execSQL(DATABASE_CREATE);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
        {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS files");
            onCreate(db);
        }
    }
    //---opens the database---
    public DBAdapter open() throws SQLException
    {
        db = DBHelper.getWritableDatabase();
        return this;
    }
    //---closes the database---
    public void close()
    {
        DBHelper.close();
    }
    //---insert a contact into the database---
    public long insertFile(String name, String filedate, Integer size, String filetype, String path, String blob_hash, String status, Integer thumb_uploaded, String commit_hash)
    {
    	// Make sure a file with the same information does not already exist
        Cursor mCursor =
                db.query(true, DATABASE_TABLE, new String[] {KEY_ROWID,
                KEY_NAME, KEY_FILEDATE, KEY_SIZE, KEY_FILETYPE, KEY_PATH, KEY_BLOB_HASH, KEY_STATUS}, 
                KEY_BLOB_HASH + "='" + blob_hash+"' AND " + KEY_NAME + "='" + name + "' AND " + 
                KEY_PATH + "='" + path + "'",
                null, null, null, null, null);
        if (mCursor != null && mCursor.moveToFirst()) {
            return -1;
        }
        
    	
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_NAME, name);
        initialValues.put(KEY_FILEDATE, filedate);
        initialValues.put(KEY_SIZE, size);
        initialValues.put(KEY_FILETYPE, filetype);
        initialValues.put(KEY_PATH, path);
        initialValues.put(KEY_BLOB_HASH, blob_hash);
        initialValues.put(KEY_STATUS, status);
        initialValues.put(KEY_THUMB_UPLOADED, thumb_uploaded);
        initialValues.put(KEY_COMMIT_HASH, commit_hash);
        return db.insert(DATABASE_TABLE, null, initialValues);
    }
    //---deletes a particular row---
    public boolean deleteRow(long rowId)
    {
        return db.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
    }
    
    //---deletes rows with name and path---
    public boolean deleteRowNamePath(String name, String path)
    {
        return db.delete(DATABASE_TABLE, KEY_NAME + "='" + name + "' AND " + KEY_PATH + "='" + path + "'", null) > 0;
    }
    
    //---retrieves all files, which thumbnail is not yet uploaded
    public Cursor getAllFilesWithCommitHash(String commit_hash){
        return db.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_NAME,
        		KEY_FILEDATE, KEY_SIZE, KEY_FILETYPE, KEY_PATH, KEY_BLOB_HASH, KEY_STATUS, KEY_THUMB_UPLOADED, KEY_COMMIT_HASH}, 
        		KEY_COMMIT_HASH + "='" + commit_hash + "'", 
        		null, null, null, 
        		null);
    }
    
    
    //---retrieves all files, which thumbnail is not yet uploaded
    public Cursor getAllFilesWithThumbnailNotUploaded(){
        return db.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_NAME,
        		KEY_FILEDATE, KEY_SIZE, KEY_FILETYPE, KEY_PATH, KEY_BLOB_HASH, KEY_STATUS, KEY_THUMB_UPLOADED}, 
        		KEY_THUMB_UPLOADED + "=0", 
        		null, null, null, 
        		null);
    }
    
    
    //---retrieves all the files---
    public Cursor getAllFiles()
    {
        return db.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_NAME,
        		KEY_FILEDATE, KEY_SIZE, KEY_FILETYPE, KEY_PATH, KEY_BLOB_HASH, KEY_STATUS, KEY_THUMB_UPLOADED, KEY_COMMIT_HASH}, 
        		null, null, null, 
        		null,
        	//	KEY_FILEDATE + " desc", 
        		null);
    }
    
  //---retrieves all the files with only name and path---
    public Cursor getAllFilesWithNameAndPath()
    {
        return db.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_NAME, KEY_PATH}, 
                null, null, null, 
                null,
            //  KEY_FILEDATE + " desc", 
                null);
    }
    
    //---retrieves a particular file---
    public Cursor getFile(long rowId) throws SQLException
    {
        Cursor mCursor =
                db.query(true, DATABASE_TABLE, new String[] {KEY_ROWID,
                KEY_NAME, KEY_FILEDATE, KEY_SIZE, KEY_FILETYPE, KEY_PATH, KEY_BLOB_HASH, KEY_STATUS, KEY_THUMB_UPLOADED, KEY_COMMIT_HASH}, 
                KEY_ROWID + "=" + rowId, null,
                null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }
    
    //---retrieves a particular file---
    public Cursor getFileWithNameAndPath(String name, String path) throws SQLException
    {
    	String where = KEY_NAME + "='" + name + "' AND " + KEY_PATH + "='" + path+"'";
        Cursor mCursor =
                db.query(true, DATABASE_TABLE, new String[] {KEY_ROWID,
                KEY_NAME, KEY_FILEDATE, KEY_SIZE, KEY_FILETYPE, KEY_PATH, KEY_BLOB_HASH, KEY_STATUS, KEY_THUMB_UPLOADED, KEY_COMMIT_HASH}, 
                where, null,
                null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }
    
    
    //---updates a file---
    public boolean updateFile(long rowId, String name, String filedate, Integer size, String filetype, String path, String blob_hash, String status, Integer thumb_uploaded, String commit_hash)
    {
        ContentValues args = new ContentValues();
        args.put(KEY_NAME, name);
        args.put(KEY_FILEDATE, filedate);
        args.put(KEY_SIZE, size);
        args.put(KEY_FILETYPE, filetype);
        args.put(KEY_PATH, path);
        args.put(KEY_BLOB_HASH, blob_hash);
        args.put(KEY_STATUS, status);
        args.put(KEY_THUMB_UPLOADED, thumb_uploaded);
        args.put(KEY_COMMIT_HASH, commit_hash);
        return db.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }

	
}
