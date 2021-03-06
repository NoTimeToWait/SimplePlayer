package com.notime2wait.simpleplayer;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

public class BgView extends View {

	public static String LOG_TAG = BgView.class.getName();
	
    private Bitmap image;
    private String lastPath; 
    private Bitmap bgImage;
    private Bitmap albumImage;
    private Bitmap alphaMask;
    private CropParam mCropParam = CropParam.SCALE_CROP;
    private Point mDisplaySize = new Point();
    private boolean defaultBackground = false;
    private float DIP = this.getContext().getResources().getDisplayMetrics().density;
    
    private final Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    
    float[] COLOR_MTRX = {
    	    0, 0, 0, 0, 255,
    	    0, 0, 0, 0, 255,
    	    0, 0, 0, 0, 255,
    	    1, 1, 1, -1, 0,
    	};
    
    

    public BgView(Context context) { 
        super(context); 
        ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getSize(this.mDisplaySize);
        if (defaultBackground) setDefaultBackground();//simpleScaleOptionTest(1);
        init();
    } 
    
    public BgView(Context context, AttributeSet attrs) { 
        super(context, attrs); 
        ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getSize(this.mDisplaySize);
        if (defaultBackground) setDefaultBackground();//simpleScaleOptionTest(1);
        init();
    } 
    
    public void init() {
    	ColorMatrix cm = new ColorMatrix(COLOR_MTRX);
     	ColorMatrixColorFilter filter = new ColorMatrixColorFilter(cm);
     	maskPaint.setColorFilter(filter);
     	maskPaint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
     	alphaMask = BitmapFactory.decodeResource(getResources(), R.drawable.crystal_alpha_hdpi);
    }

    @Override
    protected void onDraw(Canvas canvas) {
       super.onDraw(canvas);

       if(image != null) 
         canvas.drawBitmap(image, 0, 0, null); 
       if (albumImage!=null) {
    	   //Paint colorFilterPaint = new Paint();
    	   //colorFilterPaint.setColorFilter(new LightingColorFilter(0xffffff, 0x880000));
    	   //canvas.drawBitmap(albumImage, 0, 0, colorFilterPaint); 
    	   canvas.drawBitmap(albumImage, 0, 0, null); 
       }
    }
    
    public void setNoCrop() {
    	mCropParam = CropParam.NO_CROP;
    }
    
    public void setOnlyCrop() {
    	mCropParam = CropParam.ONLY_CROP;
    }
    
    public void setDefaultCrop() {
    	mCropParam = CropParam.SCALE_CROP;
    }
    
    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
    // Raw height and width of image
    int scale = 1;
    while ((options.outWidth / Math.pow(scale, 2) >= reqWidth)&&
    		(options.outHeight / Math.pow(scale, 2) >= reqHeight)){
       scale++;
    }
    if (MainActivity.DEBUG) Log.d(LOG_TAG, "scale dimensions by 1/" + Math.pow(scale, 2) + ", orig-width: " + options.outWidth + "orig-height: " + options.outHeight);

    return --scale;
    }
    
    public void setAlbumImage(String path) {
    	if ((path==null&&lastPath==null)||(path!=null&&path.equals(lastPath))) return;
    	lastPath = path;
    	BackgroundViewTask bgTask = new BackgroundViewTask();
    	bgTask.execute(path);
    }
    	//Bitmap.createBitmap(albumArt, x, y, width, height);
    	/*
    	float scaleRatio = (float) mDisplaySize.y / albumArt.getHeight();
    	int cropWidth, cropHeight;
    	cropHeight = albumArt.getHeight();
    	cropWidth = (int) (mDisplaySize.x / scaleRatio);
    	int cropX = (albumArt.getWidth() - cropWidth)/2;
    	RectF targetRect = new RectF(cropX, 0, cropX + cropWidth, cropHeight);
    	*/
    	//albumArt = Bitmap.createBitmap(albumArt, cropX, 0, cropX + cropWidth, cropHeight);
    	//Canvas albumCropCanvas = new Canvas(albumArt);
    	//albumCropCanvas.
    	//albumArt = Bitmap.createBitmap(albumArt, x, y, width, height);
    	
    	//Canvas albumCanvas = new Canvas(albumArt);
        //albumCanvas.setBitmap(albumArt);
    //}
    

    
    
    //for default bgImage
    private void setImage(Resources res, int resId,
            int reqWidth, int reqHeight) {
    	lastPath = null;
    	if (bgImage!=null) {
    		image = bgImage;
    		return;
    	}
    	// First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);
        //if (MainActivity.DEBUG) Log.e(LOG_TAG, "options.outHeight"+options.outHeight+"options.outWidth"+options.outWidth);
        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        //if (MainActivity.DEBUG) Log.e(LOG_TAG, "options.inSampleSize"+options.inSampleSize);
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        image = BitmapFactory.decodeResource(res, resId, options);
        if (mCropParam==CropParam.NO_CROP) {
        	float scaleX = (float) image.getWidth()/reqWidth;
            float scaleY = (float) image.getHeight()/reqHeight;

            if (scaleX!=1&&scaleY!=1)
            {
            	image = Bitmap.createScaledBitmap(image, reqWidth, reqHeight, true);
            	
            	if (MainActivity.DEBUG) Log.d(LOG_TAG, "Height1"+image.getHeight()+"Width"+image.getWidth()+"Mutable"+image.isMutable());
            	
            	int statusBarH = (int) (25*DIP);

            	Log.d(LOG_TAG, "Density"+statusBarH);
            	image = Bitmap.createBitmap(image, 0, statusBarH, reqWidth, reqHeight-statusBarH);
            	if (MainActivity.DEBUG) Log.d(LOG_TAG, "Height2"+image.getHeight()+"Width"+image.getWidth()+"Mutable"+image.isMutable());
            }
        }
        if (mCropParam==CropParam.SCALE_CROP) {
        	float scaleX = (float) image.getWidth()/reqWidth;
            float scaleY = (float) image.getHeight()/reqHeight;

            //Log.e(LOG_TAG, "ScaleX"+scaleX+"ScaleY"+scaleY);
            if (scaleX!=1&&scaleY!=1)
            {
            	image = (scaleX>scaleY)? Bitmap.createScaledBitmap(image, reqWidth, (int) (image.getHeight()*scaleX), true) 
            						   : Bitmap.createScaledBitmap(image, (int) (image.getWidth()*scaleY), reqHeight, true) ;
            	
            	image = Bitmap.createBitmap(image, 0, 0, reqWidth, reqHeight);
            }
        }
        if (mCropParam==CropParam.ONLY_CROP) {
        	int startX = image.getWidth()/2-reqWidth/2;
        	int startY = image.getHeight()/2-reqWidth/2; 
        	if (startX<0) startX = 0; 
        	if (startY<0) startY = 0;
        	image = Bitmap.createBitmap(image,  startX,  startY, reqWidth, reqHeight);
        }
        bgImage = image;
    
        
        if (MainActivity.DEBUG) Log.d(LOG_TAG, "Height"+image.getHeight()+"Width"+image.getWidth()+"Mutable"+image.isMutable());
    }
    
    public void setDefaultBackground(){
    	if (defaultBackground) return;
    	setNoCrop();
    	Log.d(LOG_TAG, "mDisplaySize.x"+mDisplaySize.x+"mDisplaySize.y"+mDisplaySize.y);
		//bgImageView.imageBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.simpleplayer_bg),960,1600,true);
    	//if (bgImage!=null) image = bgImage;
    	setImage(getResources(), R.drawable.simpleplayer_bg,mDisplaySize.x, mDisplaySize.y);
    	defaultBackground = true;
    }
    
    private class BackgroundViewTask extends AsyncTask<String, Void, Bitmap>{

		@Override
		protected Bitmap doInBackground(String... path) {
			Bitmap albumArt = BitmapFactory.decodeFile(path[0]);
	    	//if (alphaMask==null)
	    	//	alphaMask = BitmapFactory.decodeResource(getResources(), R.drawable.crystal_alpha_hdpi);
	    	//fit to screen and crop
	    	if (albumArt!=null) {
	        	albumArt = scaleCenterCrop(albumArt, mDisplaySize.x, mDisplaySize.y, false);
	    		Canvas albumCanvas = new Canvas(albumArt);
	    		albumCanvas.drawBitmap(alphaMask, 0, 0, maskPaint);
	    	}
			return albumArt;
		}
		
		@Override
		protected void onPostExecute(Bitmap result) {
	    	albumImage = result;
			invalidate();
	    }

        public Bitmap scaleCenterCrop(Bitmap source, int newWidth, int newHeight, boolean crop) {
            if (source==null) return null;
            int sourceWidth = source.getWidth();
            int sourceHeight = source.getHeight();

            float xScale = (float) newWidth / sourceWidth;
            float yScale = (float) newHeight / sourceHeight;
            float scale = crop? Math.max(xScale, yScale) : Math.min(xScale, yScale);

            float scaledWidth = scale * sourceWidth;
            float scaledHeight = scale * sourceHeight;

            float left = (newWidth - scaledWidth) / 2;
            float top = crop? (newHeight - scaledHeight) / 2 : 60;

            RectF targetRect = new RectF(left, top, left + scaledWidth, top + scaledHeight);

            Bitmap.Config bmpCfg = source.getConfig();
            if(bmpCfg == null)
                bmpCfg = Bitmap.Config.ARGB_8888;

            Bitmap dest = Bitmap.createBitmap(newWidth, newHeight, bmpCfg);
            Canvas canvas = new Canvas(dest);
            canvas.drawBitmap(source, null, targetRect, null);

            return dest;
        }
    
    }
    
    public enum CropParam {
    	NO_CROP, ONLY_CROP, SCALE_CROP;
    }
    
    }
