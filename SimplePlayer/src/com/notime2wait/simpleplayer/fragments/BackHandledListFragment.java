package com.notime2wait.simpleplayer.fragments;

import com.notime2wait.simpleplayer.MainActivity;
import com.notime2wait.simpleplayer.fragments.IBackHandledFragment.BackHandlerInterface;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.ListView;

public abstract class BackHandledListFragment extends ListFragment implements IBackHandledFragment {
	private BackHandlerInterface backHandlerActivity;
	//private static final String LIST_STATE = "listState";
	private Pair<Integer, Integer> mListState = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	if(!(getActivity()  instanceof BackHandlerInterface)) {
	    throw new ClassCastException("Hosting activity must implement BackHandlerInterface");
	} else {
		backHandlerActivity = (BackHandlerInterface) getActivity();
	}
    }
	

    @Override
	public void onActivityCreated(Bundle savedInstanceState) {
	    super.onActivityCreated(savedInstanceState);
	    //if (savedInstanceState!=null)
	    //	mListState = savedInstanceState.getParcelable(LIST_STATE);
    }
    
    @Override
    public void onStart() {
        super.onStart();
		
	// Mark this fragment as the selected Fragment.
        backHandlerActivity.setSelectedFragment(this);
    }
    
    protected void saveScrollState(){
		ListView mList = getListView();
		int index = mList.getFirstVisiblePosition();
		View v = mList.getChildAt(0);
		int top = (v == null) ? 0 : (v.getTop() - mList.getPaddingTop());
    	mListState = new Pair<Integer, Integer>(index, top);
        //state.putParcelable(LIST_STATE, mListState);
    }
    
    protected void restoreScrollState() {
    	if (mListState != null) {
            getListView().setSelectionFromTop(mListState.first, mListState.second);
    	}
        mListState = null;
    }
    
   /* @Override
	public void onResume() {
        super.onResume();
        restoreScrollState();
    }
    
    protected void saveScrollState(){
    	mListState = getListView().onSaveInstanceState();
        //state.putParcelable(LIST_STATE, mListState);
    }
    
    protected void restoreScrollState() {
    	if (mListState != null)
            getListView().onRestoreInstanceState(mListState);
        mListState = null;
    }

    @Override
	public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        mListState = getListView().onSaveInstanceState();
        state.putParcelable(LIST_STATE, mListState);
    }
    
    /*@Override
	public boolean onBackPressed() {
    	restoreScrollState();
    	return true;
    }
    */
	
    
}   
