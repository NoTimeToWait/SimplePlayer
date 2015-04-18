package com.notime2wait.simpleplayer.fragments;


public interface IBackHandledFragment {
    
    public String getTagText();
    public boolean onBackPressed();
    
    public interface BackHandlerInterface {
    	public void setSelectedFragment(IBackHandledFragment backHandledFragment);
        }
}
