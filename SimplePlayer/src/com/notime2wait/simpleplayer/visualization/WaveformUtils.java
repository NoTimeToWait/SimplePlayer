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
import com.notime2wait.simpleplayer.MusicData.Track;
import com.notime2wait.simpleplayer.visualization.IVisuals.OnVisualsUpdateListener;

public class WaveformUtils extends AsyncTask<Track, Void, Bitmap> implements IVisuals {
		
	//private Track mTrack;
	private short[] mHeights = new short[0];
	double[] smoothedGains = new double[0];
    private int gainHist[] = new int[256];
    private int maxFrames;
    
	private int REQ_WIDTH=320; //should be the screen width in most cases
	private int REQ_HEIGHT=45; //the height of waveform top part
	private int UPDATE_MILESTONE=4; //a percentage milestone at which waveform visual update occurs
	
	private double maxGain;
	private double minGain;
	private int factor;
	private double range; //diffrence between maxGain and minGain
	private static boolean LATTER_SMOOTH = true;
	private boolean isReady = false;
	
    private Bitmap cachedBitmap = null;
    private boolean initialized = false;
    private final Paint linePaint = new Paint();
    private final Paint maskPaint = new Paint();
    private NinePatchDrawable waveformBkgd;
    
    private CheapSoundFile mSoundFile;
	OnVisualsUpdateListener mUpdateListener;
    
 // The matrix is stored in a single array, and its treated as follows: [ a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t ]
    // When applied to a color [r, g, b, a], the resulting color is computed as (after clamping) ;
    //   R' = a*R + b*G + c*B + d*A + e; 
    //   G' = f*R + g*G + h*B + i*A + j; 
    //   B' = k*R + l*G + m*B + n*A + o; 
    //   A' = p*R + q*G + r*B + s*A + t; 
    
    
    float[] COLOR_MTRX = {
    	    0, 0, 0, 0, 255,
    	    0, 0, 0, 0, 255,
    	    0, 0, 0, 0, 255,
    	    1, 1, 1, -1, 0,
    	};
    
    //private static CachedDrawingView cachedView = new CachedDrawingView();
	
	public WaveformUtils(int waveformWidth, int waveformHeight, Context context) {
		this.REQ_WIDTH = waveformWidth;
		this.REQ_HEIGHT = waveformHeight;
		
		 linePaint.setColor(Color.WHITE);

     	ColorMatrix cm = new ColorMatrix(COLOR_MTRX);
     	ColorMatrixColorFilter filter = new ColorMatrixColorFilter(cm);
     	maskPaint.setColorFilter(filter);
     	maskPaint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
     	
     	waveformBkgd = (NinePatchDrawable)context.getResources().getDrawable(R.drawable.waveform_bg);
		
	}
	
	public boolean isReady() {
		return isReady;
	}
	
	public void getWaveformGains(Track track) {
		mSoundFile = CheapMP3.getFactory().create();
		mSoundFile.
		setProgressListener(new CheapSoundFile.ProgressListener() {
			
			private int progressPercent;
			private int previousFrame=-1;
			
			
			public boolean reportProgress(double fractionComplete) {
				int progressNewPercent = (int) (fractionComplete*100);
				int numFrames = mSoundFile.getNumFrames();
				if (numFrames<65) return true;
				int[] frameGains = mSoundFile.getFrameGains();
				//TODO:grow arrays 
				if (frameGains.length>maxFrames) {
					maxFrames = frameGains.length;
					smoothedGains = new double[maxFrames];
					Arrays.fill(smoothedGains, 0);
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
					
					return true;	
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

			        
				
				if (progressNewPercent!=progressPercent && progressNewPercent%UPDATE_MILESTONE==0) {
					//onProgressUpdate(mSoundFile.getNumFrames(), mSoundFile.getFrameGains());
					recalibrateHeights(numFrames, false);
					//publishProgress(mSoundFile.getFrameGains() , new int[] {mSoundFile.getNumFrames()});
					publishProgress();
				}
				progressPercent = progressNewPercent;
				return true;
			}
			
			
		});
		try {
			mSoundFile.ReadFile(new File(track.getPath()));
	        
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		isReady = true;
	}
	
	
	private void recalibrateHeights(int numFrames, boolean lastRecalibrate) {
		// Re-calibrate the min to be 2.5%
		minGain = 0;
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
        
        
     // Compute the heights
        int newLen = lastRecalibrate? mSoundFile.getNumFrames() : (int)(maxFrames*0.9);
        newLen/=factor;
        mHeights = new short[newLen];
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
	}
	

	public float[] getWaveformCoords() {
		
			//getWaveformGains();
			int numFrames = mHeights.length;
			
			int gainMax = 1;
			int index=0;
			short[] hist = new short[REQ_HEIGHT+1];
			for (int i=1; i<mHeights.length-1; i++) {
					if (mHeights[i]<REQ_HEIGHT&&mHeights[i]>0) hist[mHeights[i]]++;
				
				//if (mHeights[i]==mHeights[i-1]) mHeights[i] = (short) ((mHeights[i-1]+mHeights[i]+mHeights[i+1])/3.0);
			}
			for (int i=REQ_HEIGHT; i>0; i--) {
				if (hist[i]>REQ_WIDTH/40) {
					gainMax = i;
					break;
				}
			}
			
			float div = (float) gainMax/REQ_HEIGHT;	
		    float height_scale = (float) (REQ_HEIGHT/gainMax);
			if (div<0.5f) height_scale = (float) ((REQ_HEIGHT*3/4)/gainMax);
			if (div<0.3f) height_scale = (float) ((REQ_HEIGHT/2)/gainMax);
			if (div<0.15f) height_scale = (float) ((REQ_HEIGHT/3)/gainMax);
			if (MainActivity.DEBUG) Log.e("WWWWW", "Gain "+gainMax+"Index"+index+" Div "+div+" Scale "+height_scale);
			
			for (int i=1; i<mHeights.length-1; i++) {
				mHeights[i]*=height_scale;
				//smooth 1px pikes (especially in the beginning and ending)
				if ((mHeights[i]>gainMax*3.0/4.0)&&
					 mHeights[i-1]<REQ_HEIGHT/10 &&
					 mHeights[i+1]*height_scale<REQ_HEIGHT/10) 
							mHeights[i] = mHeights[i-1]; 
			}
		    float[] result = new float[REQ_WIDTH*4];
		    int remainder = 0;
		    int addFactor = 0;
		    if (REQ_WIDTH>numFrames) {
		    	remainder = REQ_WIDTH-numFrames;
		    	addFactor = (int) Math.ceil((double)numFrames/remainder);
		    }
		    int count = 1;
		    float addFrameHeight = 0;
		    
		    for (int i=1; i<REQ_WIDTH; i++) {

		    	result[i*4] = i;  
				result[i*4+2] = i; 
			    if ((remainder>5 && i%addFactor==0) || count>mHeights.length-2) {
			    	addFrameHeight = (mHeights[count-1]+mHeights[count])/2f; 
			    	result[i*4+1] = REQ_HEIGHT - addFrameHeight;
				    result[i*4+3] = REQ_HEIGHT + addFrameHeight/2;
					continue;
			    }

				result[i*4+1] = REQ_HEIGHT - mHeights[count];
			    result[i*4+3] = REQ_HEIGHT + mHeights[count]/2;
			    count++;
			    
		    }
		    			
		return result;
	}
	
	
	private void updateBitmap(){
    	float[] coords = getWaveformCoords();
        		
    	int width = REQ_WIDTH;
    	//1.5 because total waveform height consists of top part (REQ_HEIGHT) and bottom part (REQ_HEIGHT/2)
    	int height = (int) (REQ_HEIGHT*1.5); 
        //prepare alpha mask bitmap (could be loading external resource instead of drawing from scratch)
        Bitmap alphaMask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas alpha = new Canvas(alphaMask);
        alpha.drawLines(coords, linePaint);
        	                    
        cachedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas cachedCanvas = new Canvas(cachedBitmap);
        //draw 9patched background
        Rect npdBounds = new Rect(0, 0, width, height);
        //Log.e("WWWWW", "this.getWidth()"+width+"this.getHeight()"+height);
        waveformBkgd.setBounds(npdBounds);
        waveformBkgd.draw(cachedCanvas);
            //apply mask
        cachedCanvas.drawBitmap(alphaMask, 0, 0, maskPaint);

		Log.e("UUUUUUUUUUU","UUUUUUUUUUUUU Width"+this.REQ_WIDTH);
            
    }
    
	/*
	public void setTrack(Track track){
		//TODO change to doInBackground
		
		doInBackground(track);
		
	}*/
    /*
    public Bitmap getBitmap(){
    	updateBitmap();
    	return cachedBitmap;
    }*/

	@Override
	protected Bitmap doInBackground(Track... tracks) {
		// TODO Auto-generated method stub
		Arrays.fill(gainHist, 0);
		Arrays.fill(mHeights, (short) 0);
		maxFrames = 0;
		getWaveformGains(tracks[0]);
		return null;
	}
	
	@Override
	protected void onProgressUpdate(Void... args) {
		//int[] frameGains = args[0];
		//int numFrames = args[1][0];
		//updateGains(frameGains, numFrames);
		updateBitmap();
		mUpdateListener.onVisualsUpdate(cachedBitmap);
	}
	
	@Override
	protected void onPostExecute(Bitmap result) {
		int[] frameGains = mSoundFile.getFrameGains();
		int numFrames =mSoundFile.getNumFrames();
		//updateGains(frameGains, numFrames);
		recalibrateHeights(numFrames, true);
		updateBitmap();
		result = cachedBitmap;
		Log.e("UUUUUUUUUUU","UUUUUUUUUUUUU Width"+this.REQ_WIDTH+" "+numFrames+"/"+frameGains.length);
		mUpdateListener.onVisualsUpdate(cachedBitmap);
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
	
	
	/*
	public class CachedDrawingView extends View {
        
       
        private float[] coords; //= new float[][] {new float[] {0.0000f,86.5500f,0.0000f,21.9000f},new float[] {0.0000f,21.9000f,1.0000f,90.5500f},new float[] {1.0000f,90.5500f,1.0000f,5.6500f},new float[] {1.0000f,5.6500f,2.0000f,99.8000f},new float[] {2.0000f,99.8000f,2.0000f,5.3000f},new float[] {2.0000f,5.3000f,3.0000f,89.9500f},new float[] {3.0000f,89.9500f,3.0000f,23.9000f},new float[] {3.0000f,23.9000f,4.0000f,95.0000f},new float[] {4.0000f,95.0000f,4.0000f,21.9000f},new float[] {4.0000f,21.9000f,5.0000f,98.0000f},new float[] {5.0000f,98.0000f,5.0000f,10.4500f},new float[] {5.0000f,10.4500f,6.0000f,96.8000f},new float[] {6.0000f,96.8000f,6.0000f,24.5000f},new float[] {6.0000f,24.5000f,7.0000f,98.8000f},new float[] {7.0000f,98.8000f,7.0000f,31.2500f},new float[] {7.0000f,31.2500f,8.0000f,87.2500f},new float[] {8.0000f,87.2500f,8.0000f,21.8500f},new float[] {8.0000f,21.8500f,9.0000f,92.3500f},new float[] {9.0000f,92.3500f,9.0000f,11.8000f},new float[] {9.0000f,11.8000f,10.0000f,91.6500f},new float[] {10.0000f,91.6500f,10.0000f,6.6500f},new float[] {10.0000f,6.6500f,11.0000f,86.2000f},new float[] {11.0000f,86.2000f,11.0000f,1.7500f},new float[] {11.0000f,1.7500f,12.0000f,64.0500f},new float[] {12.0000f,64.0500f,12.0000f,27.0500f},new float[] {12.0000f,27.0500f,13.0000f,81.7000f},new float[] {13.0000f,81.7000f,13.0000f,4.2500f},new float[] {13.0000f,4.2500f,14.0000f,87.5500f},new float[] {14.0000f,87.5500f,14.0000f,3.8500f},new float[] {14.0000f,3.8500f,15.0000f,82.0500f}};
        
        public CachedDrawingView(Context context) {
                super(context);
                initView(context);
        }

        public CachedDrawingView(Context context, AttributeSet attrs) {
                super(context, attrs);
                initView(context);
        }

        public CachedDrawingView(Context context, AttributeSet attrs, int defStyle) {
                super(context, attrs, defStyle);
                initView(context);
        }
        private void initView(Context ctx) {
                if(!initialized) {
                        initialized = true;
                        linePaint.setColor(Color.WHITE);

                    	ColorMatrix cm = new ColorMatrix(COLOR_MTRX);
                    	ColorMatrixColorFilter filter = new ColorMatrixColorFilter(cm);
                    	maskPaint.setColorFilter(filter);
                    	maskPaint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
                    	
                    	waveformBkgd = (NinePatchDrawable)getResources().getDrawable(R.drawable.waveform_bg);
                }
        }
        
        @Override
        public void onDraw(Canvas canvas) {
        		
        	if (isReady()) 
        	{
        		if(cachedBitmap == null) update();
            	//long time = System.nanoTime();
                
                canvas.drawBitmap(cachedBitmap, 0, 0, null);

                //time = System.nanoTime() - time;
                //Log.e("WWWWW", "Took"+time / 1e9+"seconds to draw5");
                //canvas.drawBitmap(cachedBitmap, 250, 1, null);
        	}
        }
        
        private void update(){
        	coords = getWaveformCoords();
            		
        	int width = REQ_WIDTH;
        	//1.5 because total waveform height consists of top part (REQ_HEIGHT) and bottom part (REQ_HEIGHT/2)
        	int height = (int) (REQ_HEIGHT*1.5); 
            //prepare alpha mask bitmap (could be loading external resource instead of drawing from scratch)
            Bitmap alphaMask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas alpha = new Canvas(alphaMask);
            alpha.drawLines(coords, linePaint);
            	                    
            cachedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas cachedCanvas = new Canvas(cachedBitmap);
            //draw 9patched background
            Rect npdBounds = new Rect(0, 0, width, height);
            Log.e("WWWWW", "this.getWidth()"+width+"this.getHeight()"+height);
            waveformBkgd.setBounds(npdBounds);
            waveformBkgd.draw(cachedCanvas);
                //apply mask
            cachedCanvas.drawBitmap(alphaMask, 0, 0, maskPaint);
                
        }
        
        public Bitmap getBitmap(){
        	update();
        	return cachedBitmap;
        }
        
}*/

    

}
