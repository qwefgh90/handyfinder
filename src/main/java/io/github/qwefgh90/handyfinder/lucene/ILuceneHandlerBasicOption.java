package io.github.qwefgh90.handyfinder.lucene;

public interface ILuceneHandlerBasicOption {
	int limitCountOfResult = 100;
	int maximumDocumentMBSize = 100;
	
	int getMaximumDocumentMBSize();
	int getLimitCountOfResult();
}
