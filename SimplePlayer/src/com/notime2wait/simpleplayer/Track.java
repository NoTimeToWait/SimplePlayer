package com.notime2wait.simpleplayer;

import android.os.Parcel;
import android.os.Parcelable;

public class Track implements Parcelable {

	public static String LOG_TAG = Track.class.getName();

	private String title;
	private String path;
	private String album;
	private String artist;
	private String albumArt;
	/*
	public Track(String title, String path) {
		this.title = title;
		this.path = path;
	}*/
	
	public Track(String title, String path, String album, String artist, String albumArt) {
		this.title = title;
		this.path = path;
		this.album = album;
		this.artist = artist;
		this.albumArt = albumArt;
	}
	
	public String getPath() {
		return path;
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getAlbum() {
		return album;
	}
	
	public String getArtist() {
		return artist;
	}
	
	public String getAlbumArt(boolean dbCheck) {
		String art;
		if (dbCheck&&(albumArt==null||albumArt.isEmpty())) {
			art = MusicData.getInstance().getArt(this);
			if (art==null||art.isEmpty()) albumArt = "none";
			else albumArt = art;
		}
		if ("none".equals(albumArt)) return null;
		return albumArt;
	}
	
	public Track clone() {
		return new Track(title, path, album, artist, albumArt);
		
	}
	
	public static final Parcelable.Creator<Track> CREATOR = new Parcelable.Creator<Track>() {
		public Track createFromParcel(Parcel in) {
			return new Track(in);
		}

		public Track[] newArray(int size) {
			return new Track[size];
		}
	};
	
	private Track(Parcel parcel) {
		this.title = parcel.readString();
		this.path = parcel.readString();
		this.album = parcel.readString();
		this.artist = parcel.readString();
		this.albumArt = parcel.readString();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(title);
		dest.writeString(path);
		dest.writeString(album);
		dest.writeString(artist);
		dest.writeString(albumArt);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass())
			return false;
		Track other = (Track) obj;
		if (!title.equals(other.title))	return false;
		if (!album.equals(other.album))	return false;
		if (!artist.equals(other.artist)) return false;
		return true;
	}

	@Override
	public int hashCode() {
		int result = 1;
		result = 37*result+title.hashCode();
		result = 37*result+album.hashCode();
		result = 37*result+artist.hashCode();
		return result;
	}
}
