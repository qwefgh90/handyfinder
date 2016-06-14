package io.github.qwefgh90.handyfinder.lucene.model;

public class Directory {
	String pathString;
	boolean used;
	boolean recursively;
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
	public boolean isRecursively() {
		return recursively;
	}
	public void setRecursively(boolean recusively) {
		this.recursively = recusively;
	}
	
	
}
