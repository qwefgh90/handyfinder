package com.qwefgh90.io.handfinder.springweb.model;

public class Directory {
	String pathString;
	boolean used;
	boolean recusively;
	public String getPathString() {
		return pathString;
	}
	public void setPathString(String path) {
		this.pathString = path;
	}
	public boolean isUsed() {
		return used;
	}
	public void setUsed(boolean used) {
		this.used = used;
	}
	public boolean isRecusively() {
		return recusively;
	}
	public void setRecusively(boolean recusively) {
		this.recusively = recusively;
	}
	
	
}
