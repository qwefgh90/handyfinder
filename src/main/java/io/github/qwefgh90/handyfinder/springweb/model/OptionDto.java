package io.github.qwefgh90.handyfinder.springweb.model;

public class OptionDto {
	private int limitCountOfResult;
	private int maximumDocumentMBSize;
	private String keywordMode;
	private boolean firstStart;
	private boolean pathTarget;
	private boolean contentTarget;
	private int diskUseLimit;
	
	
	public int getDiskUseLimit() {
		return diskUseLimit;
	}
	public void setDiskUseLimit(int diskUseLimit) {
		this.diskUseLimit = diskUseLimit;
	}
	public boolean isPathTarget() {
		return pathTarget;
	}
	public void setPathTarget(boolean pathTarget) {
		this.pathTarget = pathTarget;
	}
	public boolean isContentTarget() {
		return contentTarget;
	}
	public void setContentTarget(boolean contentTarget) {
		this.contentTarget = contentTarget;
	}
	public boolean isFirstStart() {
		return firstStart;
	}
	public void setFirstStart(boolean firstStart) {
		this.firstStart = firstStart;
	}
	public String getKeywordMode() {
		return keywordMode;
	}
	public void setKeywordMode(String keywordMode) {
		this.keywordMode = keywordMode;
	}
	public int getLimitCountOfResult() {
		return limitCountOfResult;
	}
	public void setLimitCountOfResult(int limitCountOfResult) {
		this.limitCountOfResult = limitCountOfResult;
	}
	public int getMaximumDocumentMBSize() {
		return maximumDocumentMBSize;
	}
	public void setMaximumDocumentMBSize(int maximumDocumentMBSize) {
		this.maximumDocumentMBSize = maximumDocumentMBSize;
	}

}
