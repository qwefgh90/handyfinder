package com.qwefgh90.io.handyfinder.springweb.model;

public class OptionDto {
	private int limitCountOfResult;
	private int maximumDocumentMBSize;
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
