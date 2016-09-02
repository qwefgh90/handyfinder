package io.github.qwefgh90.handyfinder.lucene;

import io.github.qwefgh90.handyfinder.lucene.LuceneHandlerBasicOption.KEYWORD_MODE;
import io.github.qwefgh90.handyfinder.lucene.model.Directory;

import java.io.IOException;
import java.util.List;

public interface ILuceneHandlerBasicOptionView {
	int getMaximumDocumentMBSize();

	void setMaximumDocumentMBSize(int size);
	
	int getLimitCountOfResult();

	void setLimitCountOfResult(int limit);

	void deleteAppDataFromDisk() throws IOException;

	void writeAppDataToDisk();

	void deleteDirectories();

	void deleteDirectory(Directory d);

	void setDirectory(Directory d);

	void addDirectory(Directory d);

	List<Directory> getDirectoryList();
	
	KEYWORD_MODE getKeywordMode();
	
	void setKeywordMode(String keywordMode);
}
