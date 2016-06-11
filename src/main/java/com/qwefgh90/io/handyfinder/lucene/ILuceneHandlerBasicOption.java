package com.qwefgh90.io.handyfinder.lucene;

public interface ILuceneHandlerBasicOption {
	int limitCountOfResult = 100;
	int maximumDocumentMBSize = 10;
	
	int getMaximumDocumentMBSize();
	int getLimitCountOfResult();
}
