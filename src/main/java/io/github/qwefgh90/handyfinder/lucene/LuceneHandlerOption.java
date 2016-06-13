package io.github.qwefgh90.handyfinder.lucene;

/**
 * LuceneHandler Option class
 * read-only methods
 * @author choechangwon
 *
 */
public final class LuceneHandlerOption {
	final ILuceneHandlerBasicOption basicOption;
	final ILuceneHandlerMimeOption mimeOption;
	public LuceneHandlerOption(ILuceneHandlerBasicOption basicOption,
			ILuceneHandlerMimeOption mimeOption) {
		this.basicOption = basicOption;
		this.mimeOption = mimeOption;
	}
	
	
}
