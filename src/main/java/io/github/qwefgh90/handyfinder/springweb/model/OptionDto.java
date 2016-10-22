package io.github.qwefgh90.handyfinder.springweb.model;

public class OptionDto {
	private int limitCountOfResult;
	private int maximumDocumentMBSize;
	private String keywordMode;
	private boolean firstStart;
	private boolean pathMode;
	
	public boolean isPathMode() {
		return pathMode;
	}
	public void setPathMode(boolean pathMode) {
		this.pathMode = pathMode;
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
