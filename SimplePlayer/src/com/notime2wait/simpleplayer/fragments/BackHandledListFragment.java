package com.notime2wait.simpleplayer.fragments;

import com.notime2wait.simpleplayer.fragments.IBackHandledFragment.BackHandlerInterface;

import android.os.Bundle;
import android.support.v4.app.ListFragment;

public abstract class BackHandledListFragment extends ListFragment implements IBackHandledFragment {
	private BackHandlerInterface backHandlerActivity;

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
    public void onStart() {
        super.onStart();
		
	// Mark this fragment as the selected Fragment.
        backHandlerActivity.setSelectedFragment(this);
    }
	
    
}   
