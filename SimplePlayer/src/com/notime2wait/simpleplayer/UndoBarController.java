/*
 * Copyright 2012 Roman Nurik
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.notime2wait.simpleplayer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewPropertyAnimator;
import android.widget.TextView;

public class UndoBarController {
	private Context mContext;
    private View mBarView;
    private TextView mMessageView;
    private ViewPropertyAnimator mBarAnimator;
    //private Handler mHideHandler = new Handler();

    // private UndoListener mUndoListener;

    // State objects
    private Undoable mUndoToken;

    public interface Undoable {
        public void undo();
        /**
         * 
         * @return message that will be shown in the UndoBar
         */
        public String getUndoMessage();
    }

    public UndoBarController(View undoBarView) {
        mBarView = undoBarView;
        mBarAnimator = mBarView.animate();
        mMessageView = (TextView) mBarView.findViewById(R.id.undobar_message);
        
        //Log.e("WWWWWW", mBarView.findViewById(R.id.undobar_button)+"");
        mBarView.findViewById(R.id.undobar_button)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                    	mUndoToken.undo();
                        hideUndoBar(false);
                    }
                });
        //hide UndoBar on screen touch(change of focus). 
        //ActionBar touch does not hide undo bar since it doesn't change focus
        //Hide UndoBar functionality on ActionBar touch should be implemented discretely
        mBarView.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {

				Log.e("WWWWWWXX", ""+hasFocus);
				if(!hasFocus) {
					hideUndoBar(false);
				}
			}
		});

        hideUndoBar(true);
    }
    
    public boolean handleUndoToken(Undoable undoToken) {
    	mUndoToken = undoToken;
    	if (undoToken==null)
    		return false;
    	String message = undoToken.getUndoMessage();
    	if (message.isEmpty()) 
    		message = mBarView.getResources().getString(R.string.default_undo_message);
    	showUndoBar(true, message);
    	return true;
    }
    
    private void showUndoBar(boolean immediate, String message) {
        mMessageView.setText(message);
        Log.e("WWWWWW0", ""+(mBarView.getVisibility()==View.VISIBLE));
        mBarView.setVisibility(View.VISIBLE);
        if (immediate) {
            mBarView.setAlpha(1);
        } else {
            mBarAnimator.cancel();
            mBarAnimator
                    .alpha(1)
                    .setDuration(
                            mBarView.getResources()
                                    .getInteger(android.R.integer.config_shortAnimTime))
                    .setListener(null);
        }

        Log.e("WWWWWW1", ""+(mBarView.getVisibility()==View.VISIBLE));
        mBarView.requestFocus();

        Log.e("WWWWWW2", ""+(mBarView.getVisibility()==View.VISIBLE));
    }

    private void hideUndoBar(boolean immediate) {

        Log.e("WWWWWW3", ""+(mBarView.getVisibility()==View.VISIBLE));
        if (immediate) {
            mBarView.setVisibility(View.GONE);
            mBarView.setAlpha(0);
            mUndoToken = null;

        } else {
            mBarAnimator.cancel();
            mBarAnimator
                    .alpha(0)
                    .setDuration(mBarView.getResources()
                            .getInteger(android.R.integer.config_shortAnimTime))
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mBarView.setVisibility(View.GONE);
                            mUndoToken = null;
                        }
                    });
        }

        Log.e("WWWWWW4", ""+(mBarView.getVisibility()==View.VISIBLE));
    }
/*
    public void onSaveInstanceState(Bundle outState) {
        outState.putCharSequence("undo_message", mUndoMessage);
        outState.putParcelable("undo_token", mUndoToken);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mUndoMessage = savedInstanceState.getCharSequence("undo_message");
            mUndoToken = savedInstanceState.getParcelable("undo_token");

            if (mUndoToken != null || !TextUtils.isEmpty(mUndoMessage)) {
                showUndoBar(true, mUndoMessage, mUndoToken);
            }
        }
    }
*/
}

