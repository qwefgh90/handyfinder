package com.qwefgh90.io.handyfinder.lucene;

public final class LuceneHandlerOption {
	ILuceneHandlerBasicOption basicOption;
	ILuceneHandlerMimeOption mimeOption;
	public LuceneHandlerOption(ILuceneHandlerBasicOption basicOption,
			ILuceneHandlerMimeOption mimeOption) {
		this.basicOption = basicOption;
		this.mimeOption = mimeOption;
	}
	
	
}
