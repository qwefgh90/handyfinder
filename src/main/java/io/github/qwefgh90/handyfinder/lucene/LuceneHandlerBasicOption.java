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
import com.qwefgh90.io.handyfinder.gui.AppStartupConfig;

import io.github.qwefgh90.handyfinder.lucene.model.Directory;

@JsonIgnoreProperties(value = { "singleton", "om", "LOG" },ignoreUnknown = true)
class LuceneHandlerBasicOption {
	private final static Logger LOG = LoggerFactory
			.getLogger(LuceneHandlerBasicOption.class);

	private static LuceneHandlerBasicOption singleton;
	private static ObjectMapper om = new ObjectMapper();

	private List<Directory> directoryList;
	private int limitCountOfResult;
	private int maximumDocumentMBSize;

	private LuceneHandlerBasicOption() throws JsonParseException, JsonMappingException,
			IOException {
		this.directoryList = new ArrayList<Directory>();
		this.limitCountOfResult = ILuceneHandlerBasicOption.limitCountOfResult;
		this.maximumDocumentMBSize = ILuceneHandlerBasicOption.maximumDocumentMBSize;
	}

	//for Object to JSON public visibility
	public List<Directory> getDirectoryList() {
		return directoryList;
	}

	public void setDirectoryList(List<Directory> directoryList) {
		this.directoryList = directoryList;
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
		Path path = AppStartupConfig.appDataJsonPath;
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
	LuceneHandlerBasicOption loadAppDataFromDisk() throws JsonParseException,
			JsonMappingException, IOException {
		Path path = AppStartupConfig.appDataJsonPath;
		if (!Files.exists(path))
			return null;
		LuceneHandlerBasicOption app = om.readValue(path.toFile(), LuceneHandlerBasicOption.class);
		return app;
	}

	void deleteAppDataFromDisk() throws IOException {
		Path path = AppStartupConfig.appDataJsonPath;
		if(Files.exists(path))
			Files.delete(path);
	}

	/**
	 * load from xml. if loaded value is null, apply default value.
	 * @return
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	static LuceneHandlerBasicOption getInstance() throws JsonParseException,
			JsonMappingException, IOException {
		if (singleton == null) {
			singleton = new LuceneHandlerBasicOption();
			LuceneHandlerBasicOption appData = singleton.loadAppDataFromDisk();
			if (appData != null) {
				singleton.directoryList = appData.directoryList;
				singleton.limitCountOfResult = appData.limitCountOfResult;
				singleton.maximumDocumentMBSize = appData.maximumDocumentMBSize;
			}
		}
		return singleton;
	}
}