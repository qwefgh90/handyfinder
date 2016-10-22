package io.github.qwefgh90.handyfinder.lucene;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.qwefgh90.handyfinder.lucene.LuceneHandlerBasicOption.KEYWORD_MODE;
import io.github.qwefgh90.handyfinder.lucene.model.Directory;

public class LuceneHandlerBasicOptionView implements
		ILuceneHandlerBasicOptionView {
	private final static Logger LOG = LoggerFactory
			.getLogger(LuceneHandlerBasicOptionView.class);

	private LuceneHandlerBasicOption app;

	private LuceneHandlerBasicOptionView(Path appDataJsonPath) {
		try {
			app = LuceneHandlerBasicOption.getInstance(appDataJsonPath);
		} catch (IOException e) {
			LOG.error(ExceptionUtils.getStackTrace(e));
			throw new RuntimeException(e.toString());
		}
	}

	private static LuceneHandlerBasicOptionView singleton = null;

	public static LuceneHandlerBasicOptionView getInstance(Path appDataJsonPath) {
		if (singleton == null)
			singleton = new LuceneHandlerBasicOptionView(appDataJsonPath);
		return singleton;
	}

	public List<Directory> getDirectoryList() {
		return Collections.unmodifiableList(app.getDirectoryList());
	}

	public KEYWORD_MODE getKeywordMode(){
		return app.getKeywordMode();
	}
	
	public void setKeywordMode(String keywordMode){
		for(KEYWORD_MODE mode : KEYWORD_MODE.values()){
			if(mode.name().equals(keywordMode)){
				app.setKeywordMode(KEYWORD_MODE.valueOf(keywordMode));
				break;
			}
		}
	}
	
	public void addDirectory(Directory d) {
		app.getDirectoryList().add(d);
	}

	public void setDirectory(Directory d) {
		deleteDirectory(d);
		app.getDirectoryList().add(d);
	}
	
	public void deleteDirectory(Directory d) {
		Iterator<Directory> iter = app.getDirectoryList().iterator();
		while (iter.hasNext()) {
			Directory dir = iter.next();
			if (dir.getPathString().equals(d.getPathString())) {
				iter.remove();
				break;
			}
		}
	}

	public void deleteDirectories() {
		app.getDirectoryList().clear();
	}

	public boolean isPathMode() {
		return app.isPathMode();
	}

	public void setPathMode(boolean pathMode) {
		app.setPathMode(pathMode);
	}

	public void writeAppDataToDisk() {
		app.writeAppDataToDisk();
	}

	public void deleteAppDataFromDisk() throws IOException {
		app.deleteAppDataFromDisk();
	}

	public void setMaximumDocumentMBSize(int size) {
		app.setMaximumDocumentMBSize(size);
	}

	public void setLimitCountOfResult(int limit) {
		app.setLimitCountOfResult(limit);
	}

	@Override
	public int getMaximumDocumentMBSize() {
		return app.getMaximumDocumentMBSize();
	}

	@Override
	public int getLimitCountOfResult() {
		return app.getLimitCountOfResult();
	}
}
