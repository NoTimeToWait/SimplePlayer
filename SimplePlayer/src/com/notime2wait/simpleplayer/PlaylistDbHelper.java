package com.notime2wait.simpleplayer;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.provider.BaseColumns;
import android.support.v4.content.Loader;
import android.util.Log;

import java.util.Arrays;

public class PlaylistDbHelper extends SQLiteOpenHelper {
	
	private static String LOG_TAG = PlaylistDbHelper.class.getName();

    private static final String DATABASE_NAME = "playlist_database.db";
    private static final String[] DEFAULT_PLAYLISTS = {MainActivity.getMusicData().getFavoritesTitle(), MainActivity.getMusicData().getRecentTitle()};
    private static final int DATABASE_VERSION = 3;
    public static final String PLAYLIST_TABLE_NAME = "playlist_table";
    public static final String TRACKLIST_TABLE_NAME = "tracklist_table";

    private static final String SQL_CREATE_PLAYLIST_TABLE = "CREATE TABLE IF NOT EXISTS "
            + PLAYLIST_TABLE_NAME + " (" + BaseColumns._ID + " INTEGER PRIMARY KEY,"
            + PlaylistEntry.COLUMN_TITLE + " TEXT UNIQUE NOT NULL);";

    private static final String SQL_DELETE_PLAYLIST_TABLE = "DROP TABLE IF EXISTS "
            + PLAYLIST_TABLE_NAME;
    
    
    private static final String SQL_CREATE_TRACKLIST_TABLE = "CREATE TABLE IF NOT EXISTS "
            + TRACKLIST_TABLE_NAME + " (" + BaseColumns._ID + " INTEGER PRIMARY KEY,"
            + TracklistEntry.COLUMN_TITLE + " TEXT NOT NULL," 
            + TracklistEntry.COLUMN_PATH + " TEXT NOT NULL," 
            + TracklistEntry.COLUMN_ALBUM + " TEXT,"
            + TracklistEntry.COLUMN_ARTIST + " TEXT,"
            + TracklistEntry.COLUMN_ART + " TEXT,"
    		+ TracklistEntry.COLUMN_TRACKNUM + " INTEGER NOT NULL," 
    		+ TracklistEntry.COLUMN_PLAYLIST_NAME + " TEXT NOT NULL," 
            + " FOREIGN KEY ("+TracklistEntry.COLUMN_PLAYLIST_NAME+") REFERENCES " + PLAYLIST_TABLE_NAME + " (" + PlaylistEntry.COLUMN_TITLE + ") ON DELETE CASCADE ON UPDATE CASCADE);";

    private static final String SQL_DELETE_TRACKLIST_TABLE = "DROP TABLE IF EXISTS "
            + TRACKLIST_TABLE_NAME;
    
    //private static final String GET_PLAYLIST_ROWID = "SELECT " + BaseColumns._ID + " FROM " + PLAYLIST_TABLE_NAME 
    //   				+ " WHERE " + PlaylistEntry.COLUMN_TITLE + "=" + PLAYLIST_TITLE;
    
    private Context mContext;

	private Loader mPlFragmentLoader;

    public PlaylistDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }
    
    public Context getContext() {
    	return mContext;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_PLAYLIST_TABLE);
        db.execSQL(SQL_CREATE_TRACKLIST_TABLE);
        ContentValues cv = new ContentValues();
        for (String playlistName:DEFAULT_PLAYLISTS) {
        	cv.put(PlaylistEntry.COLUMN_TITLE, playlistName);
        	db.insert(PLAYLIST_TABLE_NAME, null, cv);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    	if (MainActivity.DEBUG) Log.i(LOG_TAG,
    	        "Upgrading database from version " + oldVersion + " to "
    	            + newVersion + ", which will destroy all old data");
    	db.execSQL(SQL_DELETE_PLAYLIST_TABLE);
        db.execSQL(SQL_DELETE_TRACKLIST_TABLE);
        onCreate(db);
    }
    
    
    
    @Override
    public void onOpen(SQLiteDatabase db) {
    	db.execSQL("PRAGMA foreign_keys = ON;"); 
    }
    
    public Cursor getPlaylists() {
    	return getReadableDatabase().query(PLAYLIST_TABLE_NAME, new String[] {BaseColumns._ID, PlaylistEntry.COLUMN_TITLE}, null, null, null, null, PlaylistEntry.COLUMN_TITLE);
    }
    
    public Cursor getTracklist(String playlistName) {
    	return getTracklist(getReadableDatabase(), playlistName);
    }
    
    private Cursor getTracklist(SQLiteDatabase db, String playlistName) {

    	String[] columns = {BaseColumns._ID,
    						TracklistEntry.COLUMN_TITLE, 
    						TracklistEntry.COLUMN_PATH,
    						TracklistEntry.COLUMN_ALBUM,
    						TracklistEntry.COLUMN_ARTIST,
    						TracklistEntry.COLUMN_ART,
    			}; 
    	return db.query(TRACKLIST_TABLE_NAME, columns, TracklistEntry.COLUMN_PLAYLIST_NAME + "=?", new String[]{playlistName}, null, null, TracklistEntry.COLUMN_TRACKNUM);
    }

	public void setLoader(Loader loader) {
		mPlFragmentLoader = loader;
	}
    
    public boolean savePlaylist( final IPlaylist<Track> playlist) {
    	AsyncTask<IPlaylist<Track>, Void, Void> savePlaylistTask = new AsyncTask<IPlaylist<Track>, Void, Void>() {
			@Override
			protected Void doInBackground(IPlaylist<Track>... arg0) {
				SQLiteDatabase db = getWritableDatabase();
				if (entryExists(db, PLAYLIST_TABLE_NAME, PlaylistEntry.COLUMN_TITLE, playlist.getTitle())) 
	    			removePlaylist(db, playlist);
				addPlaylist(db, playlist);
				return null;
			}
    	};
    	savePlaylistTask.execute(playlist);
    	return true;
    }
    
    public boolean saveTrackToPlaylist(String playlistTitle, final Track track) {
    	if (playlistTitle == null || playlistTitle.isEmpty() || track==null) return false;
    	SQLiteDatabase db = getWritableDatabase();
    	if (Arrays.asList(DEFAULT_PLAYLISTS).contains(playlistTitle)
    			|| entryExists(db, PLAYLIST_TABLE_NAME, PlaylistEntry.COLUMN_TITLE, playlistTitle)) {
    		Cursor cursor = getTracklist(db, playlistTitle);
    		int size = cursor.getCount();
    		saveTrackToPlaylist(db, playlistTitle, track, size);
    		return true;
    	}
    	return false;
		
    }
    
    //save track to a database with minimum calls
    //all integrity checks must be implemented in methods that call this method
    private boolean saveTrackToPlaylist(SQLiteDatabase db, String playlistTitle, final Track track, int tracknum) {
    	ContentValues cv = new ContentValues();
    	cv.put(TracklistEntry.COLUMN_TITLE, track.getTitle());
		cv.put(TracklistEntry.COLUMN_PATH, track.getPath());
		cv.put(TracklistEntry.COLUMN_ALBUM, track.getAlbum());
		cv.put(TracklistEntry.COLUMN_ARTIST, track.getArtist());
		cv.put(TracklistEntry.COLUMN_ART, track.getAlbumArt(false));
		cv.put(TracklistEntry.COLUMN_TRACKNUM, tracknum);
		cv.put(TracklistEntry.COLUMN_PLAYLIST_NAME, playlistTitle);
		db.insert(TRACKLIST_TABLE_NAME, null, cv);
		if (mPlFragmentLoader!=null) mPlFragmentLoader.forceLoad();
		return true;
    }

	public boolean removeTrackFromPlaylist(IPlaylist<Track> playlist, int trackNum) {
		if (playlist==null) return false;
		SQLiteDatabase db = getWritableDatabase();
		/*String whereClause = TracklistEntry.COLUMN_PLAYLIST_NAME + "=? AND " + TracklistEntry.COLUMN_PATH + "=?";
		String[] whereAttr = new String[] {playlistTitle, track.getPath()};
		return db.delete(TRACKLIST_TABLE_NAME, whereClause , whereAttr)>0;*/
		return removeTrackFromPlaylist(db, playlist, trackNum);
	}
    
    public static boolean entryExists(SQLiteDatabase db, String table, String field, String fieldValue) {
        Cursor cursor = db.query(table, null, field+"=?", new String[] {fieldValue}, null, null, null);
            if(cursor.getCount() <= 0) return false;      
        return true;
    }
    
    //TODO:
    private boolean addPlaylist(SQLiteDatabase db, IPlaylist<Track> playlist) {
    	ContentValues cv = new ContentValues();
    	cv.put(PlaylistEntry.COLUMN_TITLE, playlist.getTitle());
    	db.insert(PLAYLIST_TABLE_NAME, null, cv);
    	for (int i=0; i<playlist.getPlaylistSize(); i++)
    		saveTrackToPlaylist(db, playlist.getTitle(), playlist.getTrack(i), i);
    	return true;
    }
    
    
    
    private boolean removePlaylist(SQLiteDatabase db, IPlaylist<Track> playlist) {
    	
    	return db.delete(PLAYLIST_TABLE_NAME, PlaylistEntry.COLUMN_TITLE + "=?", new String[] {playlist.getTitle()})>0;
    	
    }
    
    
    private boolean addTrackToPlaylist(SQLiteDatabase db, IPlaylist<Track> playlist, Track track, int trackNum) {
    	db.execSQL("UPDATE " + TRACKLIST_TABLE_NAME + " SET " 
    			+ TracklistEntry.COLUMN_TRACKNUM + "=" + TracklistEntry.COLUMN_TRACKNUM + "-1" +
    			" WHERE " + TracklistEntry.COLUMN_TRACKNUM + ">=" + trackNum + " AND " + TracklistEntry.COLUMN_PLAYLIST_NAME +"=" + playlist.getTitle());
    	ContentValues cv = new ContentValues();
    	cv.put(TracklistEntry.COLUMN_TITLE, track.getTitle());
		cv.put(TracklistEntry.COLUMN_PATH, track.getPath());
		cv.put(TracklistEntry.COLUMN_ALBUM, track.getAlbum());
		cv.put(TracklistEntry.COLUMN_ARTIST, track.getArtist());
		cv.put(TracklistEntry.COLUMN_ART, track.getAlbumArt(false));
		cv.put(TracklistEntry.COLUMN_TRACKNUM, trackNum);
		cv.put(TracklistEntry.COLUMN_PLAYLIST_NAME, playlist.getTitle());
		db.insert(TRACKLIST_TABLE_NAME, null, cv);
    	return true;
    }
    
    private boolean removeTrackFromPlaylist(SQLiteDatabase db, IPlaylist<Track> playlist, int trackNum) {
    	
    	Track track = playlist.getTrack(trackNum);
		String whereClause =  TracklistEntry.COLUMN_TRACKNUM + ">? AND " + TracklistEntry.COLUMN_PLAYLIST_NAME +"=?";
    	db.execSQL("UPDATE " + TRACKLIST_TABLE_NAME
				+ " SET " + TracklistEntry.COLUMN_TRACKNUM + "=" + TracklistEntry.COLUMN_TRACKNUM + "-1"
				+ " WHERE " + whereClause
    			, new String[] {String.valueOf(trackNum), playlist.getTitle()});

    	whereClause = TracklistEntry.COLUMN_PLAYLIST_NAME + "=? AND " + TracklistEntry.COLUMN_TITLE + "=? AND " + TracklistEntry.COLUMN_TRACKNUM  + "=?";
		int deleted = db.delete(TRACKLIST_TABLE_NAME, whereClause , new String[] {playlist.getTitle(), track.getTitle(), String.valueOf(trackNum)});
		if (mPlFragmentLoader!=null) mPlFragmentLoader.forceLoad();
		Log.i(LOG_TAG, "DELETED "+deleted);
    	return deleted>0;
    }
    
    private boolean moveTrack(SQLiteDatabase db, IPlaylist<Track> playlist, int trackNumOld, int trackNumNew ) {
    	
    	Track track = playlist.getTrack(trackNumOld);
    	
    	db.execSQL("UPDATE " + TRACKLIST_TABLE_NAME
				+ " SET " + TracklistEntry.COLUMN_TRACKNUM + "=" + TracklistEntry.COLUMN_TRACKNUM + "-1"
				+ " WHERE " + TracklistEntry.COLUMN_PLAYLIST_NAME +"='" + playlist.getTitle() + "' AND " + TracklistEntry.COLUMN_TRACKNUM + " BETWEEN " + trackNumOld + " AND " + trackNumNew);
    	
    	ContentValues cv = new ContentValues();
    	cv.put(TracklistEntry.COLUMN_TRACKNUM, trackNumNew);
    	String whereClause = TracklistEntry.COLUMN_PLAYLIST_NAME + "=? AND " + TracklistEntry.COLUMN_TITLE + "=? AND " + TracklistEntry.COLUMN_TRACKNUM + "=?";
    	db.update(TRACKLIST_TABLE_NAME, cv, whereClause, new String[] {playlist.getTitle(), track.getTitle(), String.valueOf(trackNumOld-1)});
    	//whereClause = TracklistEntry.COLUMN_PLAYLIST_NAME + "=? AND " + TracklistEntry.COLUMN_TITLE + "=? AND " + TracklistEntry.COLUMN_TRACKNUM  + "=?";
    	//return db.delete(PLAYLIST_TABLE_NAME, whereClause , new String[] {playlist.getTitle(), track.getTitle(), String.valueOf(trackNum)})>0;
		if (mPlFragmentLoader!=null) mPlFragmentLoader.forceLoad();
		return true;
    }
    
    public static abstract class PlaylistEntry implements BaseColumns {
        public static final String TABLE_NAME = "playlist_table";
        public static final String COLUMN_TITLE = "playlist_title";
    }
    
    public static abstract class TracklistEntry implements BaseColumns {
        public static final String TABLE_NAME = "tracklist_table";
        public static final String COLUMN_TITLE = "track_title";
        public static final String COLUMN_PATH = "path";
        public static final String COLUMN_ALBUM = "album";
        public static final String COLUMN_ARTIST = "artist";
        public static final String COLUMN_ART = "art";
        public static final String COLUMN_TRACKNUM = "track_num";
        public static final String COLUMN_PLAYLIST_NAME = "playlist_name";
    }
}
