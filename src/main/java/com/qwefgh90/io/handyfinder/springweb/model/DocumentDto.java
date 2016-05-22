package com.qwefgh90.io.handyfinder.springweb.model;

public class DocumentDto {
	private long createdTime;
	private long modifiedTime;
	private String title;
	private String pathString;
	private String contents;
	private String parentPathString;
	public String getContents() {
		return contents;
	}
	public void setContents(String contents) {
		this.contents = contents;
	}
	public String getPathString() {
		return pathString;
	}
	public void setPathString(String pathString) {
		this.pathString = pathString;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public long getCreatedTime() {
		return createdTime;
	}
	public void setCreatedTime(long createdTime) {
		this.createdTime = createdTime;
	}
	public long getModifiedTime() {
		return modifiedTime;
	}
	public void setModifiedTime(long modifiedTime) {
		this.modifiedTime = modifiedTime;
	}
	public String getParentPathString() {
		return parentPathString;
	}
	public void setParentPathString(String parentPathString) {
		this.parentPathString = parentPathString;
	}
	
	
}
