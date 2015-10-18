package com.notime2wait.simpleplayer.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.Equalizer;
import android.media.audiofx.Visualizer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;

import com.notime2wait.simpleplayer.MainActivity;
import com.notime2wait.simpleplayer.R;

public class EqualizerFrag extends ListFragment{
    private static final String LOG_TAG = EqualizerFrag.class.getName();

    
    //private Visualizer mVisualizer;
    private Equalizer mEqualizer;
    private int HEADER_LISTNUM_OFFSET = 0;
	private ArrayAdapter<SeekBar> equalizerAdapter;
    private LinearLayout mLinearLayout;
    private int mDefaultGain;
    private View mHeaderView;
    private SeekBar[] bands;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        mEqualizer = new Equalizer(0, MainActivity.getSessionId());
        mEqualizer.setEnabled(true);
        bands = new SeekBar[mEqualizer.getNumberOfBands()];
        
    }
        
    @Override
	public void onActivityCreated(Bundle savedInstanceState) {
	    super.onActivityCreated(savedInstanceState);
	    //getListView().setDividerHeight(30);
        getListView().setDivider(null);
        float SCALE = getActivity().getResources().getDisplayMetrics().density;
        getListView().setPadding((int) (16*SCALE), (int) (8*SCALE), (int) (16*SCALE), 0);
        prepareHeaderView();
	    equalizerAdapter = getEqualizerAdapter();
	    setListAdapter(equalizerAdapter);
	    //setListShownNoAnimation(true);
    }
    
    private void prepareHeaderView() {	
    	if (mHeaderView!=null) return;
    	final short minEQLevel = mEqualizer.getBandLevelRange()[0];
    	final short maxEQLevel = mEqualizer.getBandLevelRange()[1];
		mHeaderView = getActivity().getLayoutInflater().inflate(R.layout.equalizer_band, null);
		TextView freqTextView = (TextView)mHeaderView.findViewById(R.id.bandFrequency);
	    freqTextView.setText("Amplify");
		/*TextView minDbTextView = (TextView)mHeaderView.findViewById(R.id.minDb);
        minDbTextView.setText((minEQLevel / 100) + " dB");
        TextView maxDbTextView = (TextView)mHeaderView.findViewById(R.id.maxDb);
        maxDbTextView.setText((maxEQLevel / 100) + " dB");*/
        SeekBar bar = (SeekBar)mHeaderView.findViewById(R.id.bandSeekBar);
        bar.setMax((maxEQLevel-minEQLevel));
        bar.setProgress((maxEQLevel-minEQLevel)/2);

        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
        	private int oldAmp=0;
        	
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            	for (short i=0; i<mEqualizer.getNumberOfBands(); i++)
            		//mEqualizer.setBandLevel(i, (short) (progress+minEQLevel+mEqualizer.getBandLevel(i)));
            		bands[i].setProgress(progress+minEQLevel+bands[i].getProgress()-oldAmp);
            	oldAmp = progress+minEQLevel;
            }

            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
    
    private ArrayAdapter<SeekBar> getEqualizerAdapter() {

        getListView().addHeaderView(mHeaderView);
		return new ArrayAdapter<SeekBar>(getActivity(),
			        R.layout.equalizer_band, bands) {
			  
			  final short minEQLevel=  mEqualizer.getBandLevelRange()[0];
			  final short maxEQLevel = mEqualizer.getBandLevelRange()[1];

			  @Override
			  public View getView(int position, View convertView, ViewGroup parent) {
			    View band_view = convertView;
			    final short band = (short) (position-HEADER_LISTNUM_OFFSET);
			    if (band_view == null) {
			       LayoutInflater inflater = getActivity().getLayoutInflater();
			    	//LayoutInflater inflater = LayoutInflater.from(getContext());
			    	band_view = inflater.inflate(R.layout.equalizer_band, parent, false);
			    }
			    TextView freqTextView = (TextView)band_view.findViewById(R.id.bandFrequency);
			    freqTextView.setText((mEqualizer.getCenterFreq(band) / 1000) + " Hz");
			    
			    /*TextView minDbTextView = (TextView)band_view.findViewById(R.id.minDb);
	            minDbTextView.setText((minEQLevel / 100) + " dB");
	            TextView maxDbTextView = (TextView)band_view.findViewById(R.id.maxDb);
	            maxDbTextView.setText((maxEQLevel / 100) + " dB");*/
	            
	            bands[band] = (SeekBar)band_view.findViewById(R.id.bandSeekBar);
	            bands[band].setMax(maxEQLevel-minEQLevel);
	            bands[band].setProgress(mEqualizer.getBandLevel(band)+(maxEQLevel-minEQLevel)/2);

	            bands[band].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
	                public void onProgressChanged(SeekBar seekBar, int progress,
	                        boolean fromUser) {
	                    mEqualizer.setBandLevel(band, (short) (progress+minEQLevel));
	                }

	                public void onStartTrackingTouch(SeekBar seekBar) {}
	                public void onStopTrackingTouch(SeekBar seekBar) {}
	            });


			    return band_view;
			  }
			  
		    }; 
	}

    @Override
	  public void onDestroyView() {
		super.onDestroyView();
		  setListAdapter(null);
		 // initialized = false;
		  //mHomePlaylist = null;
	  }
        
}


   
/*
    @Override
    protected void onPause() {
        super.onPause();

        if (isFinishing() && mPlayer != null) {
            mPlayer.release();
        }
    }
}*/

/**
 * A simple class that draws waveform data received from a
 * {@link Visualizer.OnDataCaptureListener#onWaveFormDataCapture }
 */
    /*
class VisualizerView extends View {
    private byte[] mBytes;
    private float[] mPoints;
    private Rect mRect = new Rect();

    private Paint mForePaint = new Paint();

    public VisualizerView(Context context) {
        super(context);
        init();
    }

    private void init() {
        mBytes = null;

        mForePaint.setStrokeWidth(1f);
        mForePaint.setAntiAlias(true);
        mForePaint.setColor(Color.rgb(0, 128, 255));
    }

    public void updateVisualizer(byte[] bytes) {
        mBytes = bytes;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mBytes == null) {
            return;
        }

        if (mPoints == null || mPoints.length < mBytes.length * 4) {
            mPoints = new float[mBytes.length * 4];
        }

        mRect.set(0, 0, getWidth(), getHeight());

        for (int i = 0; i < mBytes.length - 1; i++) {
            mPoints[i * 4] = mRect.width() * i / (mBytes.length - 1);
            mPoints[i * 4 + 1] = mRect.height() / 2
                    + ((byte) (mBytes[i] + 128)) * (mRect.height() / 2) / 128;
            mPoints[i * 4 + 2] = mRect.width() * (i + 1) / (mBytes.length - 1);
            mPoints[i * 4 + 3] = mRect.height() / 2
                    + ((byte) (mBytes[i + 1] + 128)) * (mRect.height() / 2) / 128;
        }

        canvas.drawLines(mPoints, mForePaint);
    }
}
*/