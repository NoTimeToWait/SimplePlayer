package com.notime2wait.simpleplayer;

import com.notime2wait.simpleplayer.MusicData.Track;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;


public class StickyListAdapter extends BaseAdapter implements
StickyListHeadersAdapter {

private LayoutInflater inflater;
String[] folders;
Track[] tracks;

public StickyListAdapter(Context context) {
inflater = LayoutInflater.from(context);
folders = MainActivity.getMusicData().getFolders();
tracks = MainActivity.getMusicData().getTracks();
Log.v("Length", "Length"+folders.length);
//mCountries = context.getResources().getStringArray(R.array.countries);
// mSectionIndices = getSectionIndices();
// mSectionLetters = getSectionLetters();
}

	@Override
	public int getCount() {
	return tracks.length;
	}

	@Override
	public Object getItem(int position) {
	return tracks[position];
	}

	@Override
	public long getItemId(int position) {
	return position;
	}

	@Override 
	public View getView(int position, View convertView, ViewGroup parent) {
	ViewHolder holder;
	if (convertView == null) {
    	holder = new ViewHolder();
    	convertView = inflater.inflate(R.layout.tracklist_item, parent, false);
    	holder.trackName = (TextView)convertView.findViewById(R.id.trackName);
   		convertView.setTag(holder);
	} else {
		holder = (ViewHolder) convertView.getTag();
	}
	holder.trackName.setText(tracks[position].getTitle());
	return convertView;
	}

	@Override 
	public View getHeaderView(int position, View convertView, ViewGroup parent) {
	HeaderViewHolder holder;

	if (convertView == null) {
    	holder = new HeaderViewHolder();
    	convertView = inflater.inflate(R.layout.folderlist_item, parent, false);
    	holder.folderName = (TextView)convertView.findViewById(R.id.folderName);
		holder.folderPath = (TextView)convertView.findViewById(R.id.folderPath);
		convertView.setTag(holder);
	} else {
		holder = (HeaderViewHolder) convertView.getTag();
	}

	String folder_item = MainActivity.getMusicData().getFolderName(position);//folders[position];
	//String folder_item = getItem(position);

	int slash_position = folder_item.lastIndexOf('/');
	String name = folder_item.substring(slash_position);    
	holder.folderName.setText(name);
	holder.folderPath.setText(folder_item.substring(0, slash_position));

	return convertView;
	}


	class HeaderViewHolder {
		TextView folderName;
		TextView folderPath;
	}

	@Override
	public long getHeaderId(int position) {
		String folder_item = MainActivity.getMusicData().getFolderName(position);
		int slash_position = folder_item.lastIndexOf('/');
		return folder_item.substring(slash_position).hashCode();
	}


	class ViewHolder {
		TextView trackName;
	}

}
