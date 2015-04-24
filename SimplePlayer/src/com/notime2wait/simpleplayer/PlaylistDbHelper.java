package com.notime2wait.simpleplayer;

import com.notime2wait.simpleplayer.MusicData.Track;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

public class PlaylistDbHelper extends SQLiteOpenHelper {
	
	private static String LOG_TAG = PlaylistDbHelper.class.getName();

    private static final String DATABASE_NAME = "playlist_database.db";
    private static final String[] DEFAULT_PLAYLISTS = {"Favorites", "Recent"};
    private static final int DATABASE_VERSION = 2;
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
    		+ TracklistEntry.COLUMN_TRACKNUM + " INTEGER NOT NULL," 
    		+ TracklistEntry.COLUMN_PLAYLIST_NAME + " TEXT NOT NULL," 
            + " FOREIGN KEY ("+TracklistEntry.COLUMN_PLAYLIST_NAME+") REFERENCES " + PLAYLIST_TABLE_NAME + " (" + PlaylistEntry.COLUMN_TITLE + ") ON DELETE CASCADE ON UPDATE CASCADE);";

    private static final String SQL_DELETE_TRACKLIST_TABLE = "DROP TABLE IF EXISTS "
            + TRACKLIST_TABLE_NAME;
    
    //private static final String GET_PLAYLIST_ROWID = "SELECT " + BaseColumns._ID + " FROM " + PLAYLIST_TABLE_NAME 
    //   				+ " WHERE " + PlaylistEntry.COLUMN_TITLE + "=" + PLAYLIST_TITLE;

    public PlaylistDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
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
    	if (MainActivity.DEBUG) Log.e(LOG_TAG,
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
    
    public Cursor getPlaylists(SQLiteDatabase db) {
    	return db.query(PLAYLIST_TABLE_NAME, new String[] {BaseColumns._ID, PlaylistEntry.COLUMN_TITLE}, null, null, null, null, PlaylistEntry.COLUMN_TITLE);
    }
    
    public Cursor getTracklist(SQLiteDatabase db, String playlistName) {
    	String[] columns = {BaseColumns._ID,
    						TracklistEntry.COLUMN_TITLE, 
    						TracklistEntry.COLUMN_PATH,
    						TracklistEntry.COLUMN_ALBUM,
    						TracklistEntry.COLUMN_ARTIST,
    			}; 
    	return db.query(TRACKLIST_TABLE_NAME, columns, TracklistEntry.COLUMN_PLAYLIST_NAME+"=?", new String[] {playlistName}, null, null, TracklistEntry.COLUMN_TRACKNUM);
    }
    
    public boolean savePlaylist(SQLiteDatabase db, IPlaylist<Track> playlist) {
    	if (entryExists(db, PLAYLIST_TABLE_NAME, PlaylistEntry.COLUMN_TITLE, playlist.getTitle())) 
    			removePlaylist(db, playlist);
    	addPlaylist(db, playlist);
    	return true;
    }
    
    public static boolean entryExists(SQLiteDatabase db, String table, String field, String fieldValue) {
        Cursor cursor = db.query(table, null, field+"=?", new String[] {fieldValue}, null, null, null);
            if(cursor.getCount() <= 0) return false;      
        return true;
    }
    
    //TODO:
    public boolean addPlaylist(SQLiteDatabase db, IPlaylist<Track> playlist) {
    	ContentValues cv = new ContentValues();
    	cv.put(PlaylistEntry.COLUMN_TITLE, playlist.getTitle());
    	db.insert(PLAYLIST_TABLE_NAME, null, cv);
    	for (int i=0; i<playlist.getPlaylistSize(); i++) {
    		Track track = playlist.getTrack(i);
    		cv = new ContentValues();
    		cv.put(TracklistEntry.COLUMN_TITLE, track.getTitle());
    		cv.put(TracklistEntry.COLUMN_PATH, track.getPath());
    		cv.put(TracklistEntry.COLUMN_ALBUM, track.getAlbum());
    		cv.put(TracklistEntry.COLUMN_ARTIST, track.getArtist());
    		cv.put(TracklistEntry.COLUMN_TRACKNUM, i);
    		cv.put(TracklistEntry.COLUMN_PLAYLIST_NAME, playlist.getTitle());
    		db.insert(TRACKLIST_TABLE_NAME, null, cv);
    	}
    	return true;
    }
    
    
    
    public boolean removePlaylist(SQLiteDatabase db, IPlaylist<Track> playlist) {
    	
    	return db.delete(PLAYLIST_TABLE_NAME, PlaylistEntry.COLUMN_TITLE + "=?", new String[] {playlist.getTitle()})>0;
    	
    }
    
    
    public boolean addTrackToPlaylist(SQLiteDatabase db, IPlaylist<Track> playlist, Track track, int trackNum) {
    	db.execSQL("UPDATE " + TRACKLIST_TABLE_NAME + " SET " 
    			+ TracklistEntry.COLUMN_TRACKNUM + "=" + TracklistEntry.COLUMN_TRACKNUM + "-1" +
    			" WHERE " + TracklistEntry.COLUMN_TRACKNUM + ">=" + trackNum + " AND " + TracklistEntry.COLUMN_PLAYLIST_NAME +"=" + playlist.getTitle());
    	ContentValues cv = new ContentValues();
    	cv.put(TracklistEntry.COLUMN_TITLE, track.getTitle());
		cv.put(TracklistEntry.COLUMN_PATH, track.getPath());
		cv.put(TracklistEntry.COLUMN_ALBUM, track.getAlbum());
		cv.put(TracklistEntry.COLUMN_ARTIST, track.getArtist());
		cv.put(TracklistEntry.COLUMN_TRACKNUM, trackNum);
		cv.put(TracklistEntry.COLUMN_PLAYLIST_NAME, playlist.getTitle());
		db.insert(TRACKLIST_TABLE_NAME, null, cv);
    	return true;
    }
    
    public boolean removeTrackFromPlaylist(SQLiteDatabase db, IPlaylist<Track> playlist, int trackNum) {
    	
    	Track track = playlist.getTrack(trackNum);
    	db.execSQL("UPDATE " + TRACKLIST_TABLE_NAME + " SET " 
    			+ TracklistEntry.COLUMN_TRACKNUM + "=" + TracklistEntry.COLUMN_TRACKNUM + "-1" +
    			" WHERE " + TracklistEntry.COLUMN_TRACKNUM + ">" + trackNum + " AND " + TracklistEntry.COLUMN_PLAYLIST_NAME +"=" + playlist.getTitle());
    	String whereClause = TracklistEntry.COLUMN_PLAYLIST_NAME + "=? AND " + TracklistEntry.COLUMN_TITLE + "=? AND " + TracklistEntry.COLUMN_TRACKNUM  + "=?";
    	return db.delete(PLAYLIST_TABLE_NAME, whereClause , new String[] {playlist.getTitle(), track.getTitle(), String.valueOf(trackNum)})>0;
    }
    
    public boolean moveTrack(SQLiteDatabase db, IPlaylist<Track> playlist, int trackNumOld, int trackNumNew ) {
    	
    	Track track = playlist.getTrack(trackNumOld);
    	
    	db.execSQL("UPDATE " + TRACKLIST_TABLE_NAME + " SET " 
    			+ TracklistEntry.COLUMN_TRACKNUM + "=" + TracklistEntry.COLUMN_TRACKNUM + "-1" +
    			" WHERE " + TracklistEntry.COLUMN_PLAYLIST_NAME +"=" + playlist.getTitle() + " AND " + TracklistEntry.COLUMN_TRACKNUM + " BETWEEN " + trackNumOld + " AND " + trackNumNew);
    	
    	ContentValues cv = new ContentValues();
    	cv.put(TracklistEntry.COLUMN_TRACKNUM, trackNumNew);
    	String whereClause = TracklistEntry.COLUMN_PLAYLIST_NAME + "=? AND " + TracklistEntry.COLUMN_TITLE + "=? AND " + TracklistEntry.COLUMN_TRACKNUM + "=?";
    	db.update(TRACKLIST_TABLE_NAME, cv, whereClause, new String[] {playlist.getTitle(), track.getTitle(), String.valueOf(trackNumOld-1)});
    	//whereClause = TracklistEntry.COLUMN_PLAYLIST_NAME + "=? AND " + TracklistEntry.COLUMN_TITLE + "=? AND " + TracklistEntry.COLUMN_TRACKNUM  + "=?";
    	//return db.delete(PLAYLIST_TABLE_NAME, whereClause , new String[] {playlist.getTitle(), track.getTitle(), String.valueOf(trackNum)})>0;
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
        public static final String COLUMN_TRACKNUM = "track_num";
        public static final String COLUMN_PLAYLIST_NAME = "playlist_name";
    }
}
