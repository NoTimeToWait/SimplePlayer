package com.notime2wait.simpleplayer.visualization;

import java.io.*;
import java.util.Arrays;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.NinePatchDrawable;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.notime2wait.simpleplayer.MainActivity;
import com.notime2wait.simpleplayer.R;
import com.notime2wait.simpleplayer.Track;
import com.notime2wait.simpleplayer.visualization.IVisuals.OnVisualsUpdateListener;

public class WaveformUtils implements IVisuals {
    
	private int REQ_WIDTH=-1; //should be the screen width in most cases
	private int REQ_HEIGHT=-1; //the height of waveform top part (approx. 2/3 of the total Height)
		
    private Bitmap cachedBitmap = null;
    private final Paint maskPaint = new Paint();
    private NinePatchDrawable waveformBkgd;
    private int[] heights;
    
    private CheapSoundFile mSoundFile;
	private OnVisualsUpdateListener mUpdateListener;
	private AsyncTask<Track, Void, Void> task;
	
	public WaveformUtils(int waveformWidth, int waveformHeight, Context context) {
		this.REQ_WIDTH = waveformWidth;
		this.REQ_HEIGHT = waveformHeight;
     	maskPaint.setAlpha(0);
     	maskPaint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
     	waveformBkgd = (NinePatchDrawable)context.getResources().getDrawable(R.drawable.waveform_bg);
	}
	
	public int[] getHeights() {
		return heights;
	}
	
	public void calculateWaveform(int[] array){
		heights = array;
		updateBitmap(getDrawLines(array));
		mUpdateListener.onVisualsUpdate(cachedBitmap);
	}
	
	public void calculateWaveform(Track track) {
		if (cachedBitmap!=null && mSoundFile != null && track.getPath().equals(mSoundFile.getFilepath())) {
			mUpdateListener.onVisualsUpdate(cachedBitmap);
			return;
		}
		if (task!=null) task.cancel(true);
		task = new AsyncTask<Track, Void, Void>() {
			@Override
			protected Void doInBackground(Track... tracks) {
				calculateSoundGains(tracks[0], this);
				if(this.isCancelled()) return null; 
				updateBitmap(getWaveformCoords(mSoundFile));
				return null;
			}
			
			@Override
			protected void onProgressUpdate(Void... coords) {
				//int[] frameGains = args[0];
				//int numFrames = args[1][0];
				//updateGains(frameGains, numFrames);
				//updateBitmap(coords[0].getCoords());
				//mUpdateListener.onVisualsUpdate(cachedBitmap);
			}
			
			@Override
			protected void onPostExecute(Void result) {
				mUpdateListener.onVisualsUpdate(cachedBitmap);
		    }
		};
		task.execute(track);
	}
	
	
	private CheapSoundFile calculateSoundGains(Track track, final AsyncTask<Track, Void, Void> taskSelf) {
		mSoundFile = CheapMP3.getFactory().create();
		//mHeights = new int[this.REQ_WIDTH];
		//CheapSoundFile mSoundFile1 = CheapMP3.getFactory().create();
		//CheapSoundFile mSoundFile2 = CheapMP33.getFactory().create();
		mSoundFile.
		setProgressListener(new CheapSoundFile.ProgressListener() {
			
			private int progressPercent;
			private int lastFrameNum=0;
			private int heightCount=0;
			
			
			public boolean reportProgress(double fractionComplete) {
				
				if (taskSelf.isCancelled()) return false;
				//int progressNewPercent = (int) (fractionComplete*100);
				/*int numFrames = mSoundFile.getNumFrames();
				if (numFrames<65) return true;
				int[] frameGains = mSoundFile.getFrameGains();
				int count;
				int ratio = frameGains.length/REQ_WIDTH;
				int remainder = frameGains.length%REQ_WIDTH;
				if (ratio>=1) {
					int remainderStep = 0;
					
				}
				else {
					//TODO:
				}*/
				/*int remainderStep = (int) Math.ceil(((double)REQ_WIDTH)/(frameGains.length%REQ_WIDTH));
				int div = frameGains.length/REQ_WIDTH;
				if (div>0) {
					for (int i=lastFrameNum; i<numFrames; i+=div) {
						if (heightCount%remainderStep==0) i++;
						mHeights[heightCount] = frameGains[i];
						heightCount++;
					}
					
				}
				else if (frameGains.length<REQ_WIDTH) {
					
				}
				else {
					int div = frameGains.length/(frameGains.length%REQ_WIDTH);
					for (int i=lastFrameNum; i<numFrames; i++) {
						if (i%div==0) continue;
						mHeights[heightCount] = frameGains[i];
						heightCount++;
					}
				}
				lastFrameNum = mSoundFile.getNumFrames();*/
				/*
				//TODO:grow arrays 
				if (frameGains.length>maxFrames) {
					maxFrames = frameGains.length;
					smoothedGains = new double[maxFrames];
					//Arrays.fill(smoothedGains, 0);
					for (int i=1; i<numFrames-1; i++) {
						smoothedGains[i] = (double)(
			                    (frameGains[i-1] + frameGains[i] + frameGains[i+1])/3.0);
			            if (smoothedGains[i] < 0)
			                	smoothedGains[i] = 0;
				        if (smoothedGains[i] > 255)
				            	smoothedGains[i] = 255;
				        if (smoothedGains[i] > maxGain)
				                maxGain = smoothedGains[i];
				            
				        gainHist[(int) smoothedGains[i]]++;
					}
					
					factor = (int) Math.ceil((double)maxFrames/REQ_WIDTH);
			        if (factor == 0) factor = 1;	
				}
		        //int gainHist[] = new int[256];
		        int i = numFrames-2;
		            smoothedGains[i] = (double)(
		                    (frameGains[i-1] + frameGains[i] + frameGains[i+1])/3.0);
		            if (smoothedGains[i] < 0)
		                	smoothedGains[i] = 0;
			        if (smoothedGains[i] > 255)
			            	smoothedGains[i] = 255;
			        if (smoothedGains[i] > maxGain)
			                maxGain = smoothedGains[i];
			            
			        gainHist[(int) smoothedGains[i]]++;
		        
		            //smoothedGains[numFrames - 1] = 1;
		        //}

			        recalibrateHeights(numFrames, false);
					//publishProgress(mSoundFile.getFrameGains() , new int[] {mSoundFile.getNumFrames()});

					//publishProgress(new LineCoords(getWaveformCoords()));    
				
				*/
				return true;
			}
			
			
		});
		try {

			mSoundFile.ReadFile(track.getPath());
			/*
			double start = System.nanoTime();
			mSoundFile1.ReadFile(new File(track.getPath()));
			double end1 = System.nanoTime() - start;
			start = System.nanoTime();
			//mSoundFile2.ReadFile(new File(track.getPath()));
			double end2 = System.nanoTime() - start;
			
			//int div = mSoundFile1.getNumFrames()-mSoundFile2.getNumFrames();
			int[] gains1 = mSoundFile1.getFrameGains();
			//int[] gains2 = mSoundFile2.getFrameGains();
			{
				Log.e("```````````", "``````````````"+"Systime  "+end1/1.0e9);
				//Log.e("````````````", "````````````"+"Systime2  "+end2/1.0e9+" Size:"+mSoundFile1.getNumFrames()+"div "+div);
				//Log.e("````````````", "````````````"+"Equals"+Arrays.equals(mSoundFile1.getFrameGains(), mSoundFile2.getFrameGains()));
				//Log.e("1", "zz"+Arrays.toString(gains1));
				//Log.e("2", "zz"+Arrays.toString(gains2));
				Log.w("","");
			}*/
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return mSoundFile;
	}
	
	private Bitmap updateBitmap(float[] coords){
    	//float[] coords = getWaveformCoords();
        		
    	int width = REQ_WIDTH;
    	//1.5 because total waveform height consists of top part (REQ_HEIGHT) and bottom part (REQ_HEIGHT/2)
    	int height = (int) (REQ_HEIGHT*1.5); 
        cachedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas cachedCanvas = new Canvas(cachedBitmap);
        //draw 9patched background
        
        waveformBkgd.setBounds(0, 0, width, height);
        waveformBkgd.draw(cachedCanvas);
        
      //apply mask    
        //cachedCanvas.drawBitmap(alphaMask, 0, 0, maskPaint);
        cachedCanvas.drawLines(coords, maskPaint);

        return cachedBitmap;    
    }
    
	
	private float[] getWaveformCoords (CheapSoundFile soundFile) {
		int length = soundFile.getNumFrames();
		int[] gains = soundFile.getFrameGains();
		//Re-calibrate the max to be 95% of all gains
		int upperBound = soundFile.getUpperBound(0.05f);
		//Re-calibrate the min to be 2.5% of all gains
		int lowerBound = soundFile.getLowerBound(0.025f);
		//getWaveformGains();
		float squeezeFrame = upperBound-lowerBound;
		//upperBound = upperBound*REQ_HEIGHT/255;
		float div = upperBound/255.0f;	
	    float heightScale;
		if (div<0.15f) heightScale = ((float)(REQ_HEIGHT/3))/squeezeFrame;
		else if (div<0.3f) heightScale = ((float)(REQ_HEIGHT/2))/squeezeFrame;
		else if (div<0.5f) heightScale = ((float)(REQ_HEIGHT*3/4))/squeezeFrame;
		else heightScale =  ((float)REQ_HEIGHT)/squeezeFrame;
		
		if (squeezeFrame/10<1) heightScale *= squeezeFrame/10;

		//Log.e("DIV", "DIV"+div+" upperBound "+upperBound+" lowerBound "+lowerBound);
		/*
		//upperBound = upperBound*REQ_HEIGHT/255;
				float div = squeezeFrame/REQ_HEIGHT;	
			    float heightScale =  REQ_HEIGHT/squeezeFrame;
				if (div<0.15f) heightScale *= 0.33f;
				else if (div<0.3f) heightScale *= 0.5f;
				else if (div<0.5f) heightScale *= 0.75f;
				Log.e("DIV", "DIV"+div+" upperBound "+upperBound+" lowerBound "+lowerBound);
				*/
		
		heights = new int[REQ_WIDTH];
		int ratio = length/REQ_WIDTH;
		int remainder = length%REQ_WIDTH;
		int tempIndex = 0;
		if (ratio>=1) {
			double remainderStep = (double)remainder/heights.length;
			for (int i=1; i<heights.length; i++) {
				tempIndex = i*ratio+(int)(i*remainderStep);
				heights[i] = (gains[tempIndex-1]+gains[tempIndex]+gains[tempIndex+1])/3-lowerBound;
				if (heights[i]<0) heights[i] = 0;
				else heights[i] *= heightScale;
			}
		}
		else {
			//TODO: check for errors while growing array
			remainder = REQ_WIDTH - length;
			double remainderStep = (double)remainder/length;
			for (int i=1; i<length; i++) {
				tempIndex = i+(int)(i*remainderStep);
				if (tempIndex>REQ_WIDTH) break;
				heights[tempIndex] = (gains[i-1]+gains[i]+gains[i+1])/3-lowerBound;
				if (heights[tempIndex]<0) heights[tempIndex] = 0;
				else heights[tempIndex] *= heightScale;
			}
		}
		
		//Log.e("", "Gainlen "+gains.length+" REQ_WIDTH "+REQ_WIDTH+" ratio "+ratio+" remainder "+remainder+" tempIndex "+tempIndex);
		
		
		int counter;
		int check = (int) ((upperBound-lowerBound)*heightScale);
		for (counter=heights.length-2; counter>heights.length-50; counter-=2) 
			if (!((heights[counter]>=check)&&(heights[counter]==heights[counter-1])&&(heights[counter]==heights[counter+1])))
				break;

		for (int i = counter; i<heights.length; i++)
			heights[i] = 0;
		/*
		while (counter>heights.length*9/10) {
			if ((heights[counter]>=check)&&(heights[counter]==heights[counter-1])&&(heights[counter]==heights[counter+1]))
				counter-=2;
			else break;
		}*/
		//smooth 1px spikes
		for (int i=1; i<heights.length-1; i++) 
			if ((heights[i]>heights[i+1]&&heights[i]>heights[i-1])||(heights[i]<heights[i+1]&&heights[i]<heights[i-1])) 
				heights[i] = (heights[i+1]+heights[i-1])/2;	
		
		
		/*
		if (gains[i]>upperBound) adjustedGainValue = (gains[i]/2-lowerBound)*heightScale;
		if ((gains[i]>gains[i+1]&&gains[i]<gains[i-1])||(gains[i]<gains[i+1]&&gains[i]>gains[i-1]))
		   adjustedGainValue = ((gains[i-1]+gains[i]+gains[i+1])/3-lowerBound)*heightScale;
		else adjustedGainValue = ((gains[i-1]+gains[i+1])/2-lowerBound)*heightScale;
		if (adjustedGainValue<0) adjustedGainValue=0;*/
		
		//prepare line coordinates for the lines, that would erase the background, leaving the actual waveform)
		
		
		return getDrawLines(heights);
	}
	
	private float[] getDrawLines(int[] array) {
		float[] result = new float[array.length*8];
		for (int i=0; i<array.length; i++) {
			result[i*8] = result[i*8+2] = result[i*8+4] = result[i*8+6] =  i;   
			result[i*8+1] = 0;
		    result[i*8+3] = REQ_HEIGHT - array[i];
			result[i*8+5] = REQ_HEIGHT + array[i]/2;
		    result[i*8+7] = 100;
		}
		return result;
	}
	
	/*public void updateGains(int[] frameGains, int numFrames)
	{
		int newMaxFrames = frameGains.length;
		//TODO:grow arrays 
		if (newMaxFrames>maxFrames) {
			smoothedGains = new double[maxFrames];
			Arrays.fill(smoothedGains, 0);
		}
        double maxGain = 0;
        //int gainHist[] = new int[256];
        
        if (numFrames == 1) {
            smoothedGains[0] = frameGains[0];
        } else if (numFrames == 2) {
            smoothedGains[0] = frameGains[0];
            smoothedGains[1] = frameGains[1];
        } else if (numFrames > 2) {
            smoothedGains[0] = 1;
            for (int i = 1; i < numFrames - 1; i++) {
                smoothedGains[i] = (double)(
                    (frameGains[i-1] + frameGains[i] + frameGains[i+1])/3.0);
                if (smoothedGains[i] < 0)
                	smoothedGains[i] = 0;
	            if (smoothedGains[i] > 255)
	            	smoothedGains[i] = 255;

	            if (smoothedGains[i] > maxGain)
	                maxGain = smoothedGains[i];
	            
	            gainHist[(int) smoothedGains[i]]++;
            }
            smoothedGains[numFrames - 1] = 1;
        }

        
        // Re-calibrate the min to be 2.5%
        double minGain = 0;
        int sum = 0;
        while (minGain < 255 && sum < numFrames / 40) {
            sum += gainHist[(int)minGain];
            minGain++;
        }

        // Re-calibrate the max to be 99%
        sum = 0;
        while (maxGain > 2 && sum < numFrames / 100) {
            sum += gainHist[(int)maxGain];
            maxGain--;
        }
        
        int factor = (int) Math.ceil((double)maxFrames/REQ_WIDTH);
        if (factor == 0) factor = 1;
     // Compute the heights
        mHeights = new short[maxFrames/factor];
        range = maxGain - minGain;
        double value = smoothedGains[1];
        int count = 0;
        int temp_index;
        for (int i = 0; i < mHeights.length; i++) {
        	if (i<3||i>mHeights.length-4) {
        		mHeights[i] = 0;
        		continue;
        	}
        	temp_index = i*factor;
        	value = smoothedGains[temp_index];// heights[i*factor];
        	for(int j=1; j<factor; j++) {
        		value+=  smoothedGains[temp_index+j];//heights[i*factor+j];
        	}
        	value/=(double)factor;
        	value-= minGain;
        	value/=range;
        	if (value < 0.0) value = 0.0;
            if (value > 1.0) value = 1.0;
        	mHeights[i] = (short) (value*REQ_HEIGHT);
        
        }
		// TODO Auto-generated method stub
		return;
	}*/

	@Override
	public void setOnVisualsUpdateListener(
			OnVisualsUpdateListener visUpdateListener) {
		mUpdateListener = visUpdateListener;
		
	}
	
}
