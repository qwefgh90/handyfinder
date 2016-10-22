package io.github.qwefgh90.handyfinder.lucene;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.qwefgh90.handyfinder.gui.AppStartupConfig;
import io.github.qwefgh90.handyfinder.lucene.model.Directory;


@JsonIgnoreProperties(value = { "singleton", "om", "LOG"},ignoreUnknown = true)
class LuceneHandlerBasicOption {
	private final static Logger LOG = LoggerFactory
			.getLogger(LuceneHandlerBasicOption.class);

	private final int _limitCountOfResult = 100;
	private final int _maximumDocumentMBSize = 100;
	private final KEYWORD_MODE _keywordMode = KEYWORD_MODE.OR;
	private final boolean _pathMode = true;
	
	public enum KEYWORD_MODE{
		OR, AND
	};
	
	private Path appDataJsonPath;
	private static LuceneHandlerBasicOption singleton;
	private static ObjectMapper om = new ObjectMapper();

	private boolean pathMode;
	private List<Directory> directoryList;
	private int limitCountOfResult;
	private int maximumDocumentMBSize;
	private KEYWORD_MODE keywordMode;
	
	private LuceneHandlerBasicOption() throws JsonParseException, JsonMappingException,
			IOException {
		this.directoryList = new ArrayList<Directory>();
		this.limitCountOfResult = _limitCountOfResult;
		this.maximumDocumentMBSize = _maximumDocumentMBSize;
		this.keywordMode = _keywordMode;
		this.pathMode = _pathMode;
	}

	public KEYWORD_MODE getKeywordMode() {
		return keywordMode;
	}

	public void setKeywordMode(KEYWORD_MODE keywordMode) {
		this.keywordMode = keywordMode;
	}

	//for Object to JSON public visibility
	public List<Directory> getDirectoryList() {
		return directoryList;
	}

	public void setDirectoryList(List<Directory> directoryList) {
		this.directoryList = directoryList;
	}

	public boolean isPathMode() {
		return pathMode;
	}

	public void setPathMode(boolean pathMode) {
		this.pathMode = pathMode;
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

	void writeAppDataToDisk() {
		Path path = appDataJsonPath;
		if(path == null)
			return;
		try {
			om.writeValue(path.toFile(), this);
		} catch (IOException e) {
			LOG.error(ExceptionUtils.getStackTrace(e));
			new RuntimeException(e.toString());
		}
	}

	/**
	 * 
	 * @return if file is not exist, return null
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	private static LuceneHandlerBasicOption loadAppDataFromDisk(Path appDataJsonPath) throws JsonParseException,
			JsonMappingException, IOException {
		if (!Files.exists(appDataJsonPath))
			return null;
		LuceneHandlerBasicOption app = om.readValue(appDataJsonPath.toFile(), LuceneHandlerBasicOption.class);
		return app;
	}

	void deleteAppDataFromDisk() throws IOException {
		Path path = appDataJsonPath;
		if(path != null && Files.exists(path))
			Files.delete(path);
	}

	/**
	 * load from xml. if loaded value is null, apply default value.
	 * @return
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	static LuceneHandlerBasicOption getInstance(Path appDataJsonPath) throws JsonParseException,
			JsonMappingException, IOException {
		if (singleton == null) {
			singleton = new LuceneHandlerBasicOption();
			LuceneHandlerBasicOption loadData = LuceneHandlerBasicOption.loadAppDataFromDisk(appDataJsonPath);
			if (loadData != null) {
				singleton.directoryList = loadData.directoryList;
				singleton.limitCountOfResult = loadData.limitCountOfResult;
				singleton.maximumDocumentMBSize = loadData.maximumDocumentMBSize;
				singleton.keywordMode = loadData.keywordMode;
				singleton.pathMode = loadData.pathMode;
			}
		}
		singleton.appDataJsonPath = appDataJsonPath;
		return singleton;
	}
}
