package com.notime2wait.simpleplayer;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

public class ButtonWheelView extends ViewGroup{
	
	//private WheelState state = WheelState.CLOSED;
	private boolean opened = false;
	private long totalDuration = 500;
	private int pivotX;
	private int pivotY;
	

	public ButtonWheelView(Context context) {
        super(context);
    }
	
    public ButtonWheelView(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public ButtonWheelView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    
    public boolean isOpened() {
    	return opened;
    }
    
    public void addHeaderView(View child){
        final ImageView openIcon = ((ImageView)child.findViewById(R.id.inner_icon));
    	child.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				animateLayout();
                openIcon.setImageResource(isOpened()? R.drawable.close_icon_small : R.drawable.open_icon_small);
			}
        });
    	addView(child, 0);
    }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        float xVector = pivotX - ev.getX();
        float yVector = ev.getY() - pivotY;
        float angle = (float)Math.atan2(yVector, xVector)+ (float)Math.PI;
        float factor = (float) (2*Math.PI/getChildCount());
        angle += factor/2; //since we have the 0 element centered vertically on circumference
        int index = (int) (angle/factor);
        index %= getChildCount();
        //Log.e("", "Index "+index+" Angle "+Math.toDegrees(angle));
        if (!isOpened() && index!=0) return false;
        return getChildAt(index).dispatchTouchEvent(ev);
    }
    
    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int count = getChildCount();
        int maxHeight = 0;
        int maxWidth = 0;
        int childState = 0;
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                this.measureChild(child, widthMeasureSpec, heightMeasureSpec);
                if (child.getMeasuredWidth()>maxWidth) maxWidth = child.getMeasuredWidth();
                if (child.getMeasuredHeight()>maxHeight) maxHeight = child.getMeasuredHeight();
                childState = combineMeasuredStates(childState, child.getMeasuredState());
            }
        }

        maxHeight = Math.max(maxHeight*2, getSuggestedMinimumHeight());
        maxWidth = Math.max(maxWidth*2, getSuggestedMinimumWidth());
        setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
                				resolveSizeAndState(maxHeight, heightMeasureSpec, childState));
    }
    
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		//Log.e("", "ON LAYOUT "+left+" "+top+" "+right+" "+bottom);
		// TODO Auto-generated method stub
		final int width = Math.abs(left - right);
		final int height = Math.abs(top - bottom);
		final int radius = Math.max(width, height)/2;
		final int count = getChildCount();
		pivotX = width/2;
		pivotY = height/2;
		for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
            	final int childwidth = child.getMeasuredWidth();
            	final int childheight = child.getMeasuredHeight();
            	child.layout(pivotX, pivotY-childheight/2, pivotX+childwidth, pivotY+childheight/2);
            	//Log.e("", "CHILD_LAYOUT "+width/2+" "+(height-childheight)/2+" "+width/2+childwidth+" "+(height+childheight)/2);
            //child.setPivotX(child.getPivotX()-childwidth/2);
            	View inner = child.findViewById(R.id.inner_icon);
            	inner.setRotation(360f/count*i);
            	child.setPivotX(0);
            	child.setPivotY(childheight/2);
            	if (i!=0) {
            		child.setRotation(360f - 360f/count*i);
            		child.setVisibility(View.INVISIBLE);
            	}
            }
            //rose rotation
            //child.setPivotX(0);
            //if (i!=0) child.setRotation(360f - 360f/count*i);
            // open 
            // child.animate().scaleX(1.1f).scaleY(1.1f).alpha(1).setDuration(durationPerChild).setStartDelay((i-1)*durationPerChild);
			// child.animate().scaleX(1.0f).scaleY(1.0f).setDuration(50).setStartDelay(i*durationPerChild);
            // close child.animate().scaleX(0.1f).scaleY(0.1f).alpha(0).setDuration(durationPerChild).setStartDelay((i-1)*durationPerChild);
			
		}
	}
	
	private void animateLayout() {
		final long durationPerChild = totalDuration/(getChildCount());
		for (int i=1; i<getChildCount(); i++) {
			Animation anim = AnimationUtils.loadAnimation(this.getContext(), isOpened()? R.anim.btn_wheel_hide : R.anim.btn_wheel_show); 
			anim.setDuration(durationPerChild);
			anim.setStartOffset(isOpened()? (getChildCount()-i)*durationPerChild : (i-1)*durationPerChild);
			((View)getChildAt(i)).startAnimation(anim);
		}
		opened=!opened;
	}
}
