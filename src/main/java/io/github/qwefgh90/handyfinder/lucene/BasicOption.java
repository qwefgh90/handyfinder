package io.github.qwefgh90.handyfinder.lucene;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.qwefgh90.handyfinder.lucene.BasicOption.BasicOptionModel.KEYWORD_MODE;
import io.github.qwefgh90.handyfinder.lucene.model.Directory;

/**
 * Service class of BasicOptionModel
 * @author qwefgh90
 *
 */
public class BasicOption {
	private final static Logger LOG = LoggerFactory
			.getLogger(BasicOption.class);

	private BasicOptionModel model;

	private BasicOption(Path appDataJsonPath) {
		try {
			model = new BasicOptionModel(appDataJsonPath);
		} catch (IOException e) {
			LOG.error(ExceptionUtils.getStackTrace(e));
			throw new RuntimeException(e.toString());
		}
	}

	private static BasicOption singleton = null;

	public static BasicOption getInstance(Path appDataJsonPath) {
		if (singleton == null)
			singleton = new BasicOption(appDataJsonPath);
		return singleton;
	}

	public List<Directory> getDirectoryList() {
		return Collections.unmodifiableList(model.getDirectoryList());
	}

	public KEYWORD_MODE getKeywordMode(){
		return model.getKeywordMode();
	}

	public void setKeywordMode(String keywordMode){
		for(KEYWORD_MODE mode : KEYWORD_MODE.values()){
			if(mode.name().equals(keywordMode)){
				model.setKeywordMode(KEYWORD_MODE.valueOf(keywordMode));
				break;
			}
		}
	}

	public void addDirectory(Directory d) {
		model.getDirectoryList().add(d);
	}

	public void setDirectory(Directory d) {
		deleteDirectory(d);
		model.getDirectoryList().add(d);
	}

	public void deleteDirectory(Directory d) {
		Iterator<Directory> iter = model.getDirectoryList().iterator();
		while (iter.hasNext()) {
			Directory dir = iter.next();
			if (dir.getPathString().equals(d.getPathString())) {
				iter.remove();
				break;
			}
		}
	}

	public void deleteDirectories() {
		model.getDirectoryList().clear();
	}

	public boolean isPathMode() {
		return model.isPathMode();
	}

	public void setPathMode(boolean pathMode) {
		model.setPathMode(pathMode);
	}

	public void writeAppDataToDisk() {
		model.writeAppDataToDisk();
	}

	public void deleteAppDataFromDisk() throws IOException {
		model.deleteAppDataFromDisk();
	}

	public void setMaximumDocumentMBSize(int size) {
		model.setMaximumDocumentMBSize(size);
	}

	public void setLimitCountOfResult(int limit) {
		model.setLimitCountOfResult(limit);
	}

	public int getMaximumDocumentMBSize() {
		return model.getMaximumDocumentMBSize();
	}

	public int getLimitCountOfResult() {
		return model.getLimitCountOfResult();
	}

	static class BasicOptionModel {
		private BasicOptionModel() {
			this.directoryList = new ArrayList<Directory>();
			this.limitCountOfResult = _limitCountOfResult;
			this.maximumDocumentMBSize = _maximumDocumentMBSize;
			this.keywordMode = _keywordMode;
			this.pathMode = _pathMode;
		}

		private final static Logger LOG = LoggerFactory
				.getLogger(BasicOptionModel.class);

		private final int _limitCountOfResult = 100;
		private final int _maximumDocumentMBSize = 100;
		private final KEYWORD_MODE _keywordMode = KEYWORD_MODE.OR;
		private final boolean _pathMode = true;

		public enum KEYWORD_MODE{
			OR, AND
		};

		private Path appDataJsonPath;
		private static BasicOptionModel singleton;
		private static ObjectMapper om = new ObjectMapper();

		private boolean pathMode;
		private List<Directory> directoryList;
		private int limitCountOfResult;
		private int maximumDocumentMBSize;
		private KEYWORD_MODE keywordMode;
		
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

		private void writeAppDataToDisk() {
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
		private static BasicOptionModel loadAppDataFromDisk(Path appDataJsonPath) throws JsonParseException,
		JsonMappingException, IOException {
			if (!Files.exists(appDataJsonPath))
				return null;
			BasicOptionModel app = om.readValue(appDataJsonPath.toFile(), BasicOptionModel.class);
			return app;
		}

		private void deleteAppDataFromDisk() throws IOException {
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
		private BasicOptionModel(Path appDataJsonPath) throws JsonParseException, JsonMappingException, IOException {
			this();
			this.appDataJsonPath = appDataJsonPath;
			BasicOptionModel loadData = BasicOptionModel.loadAppDataFromDisk(appDataJsonPath);
			if (loadData != null) {
				this.directoryList = loadData.directoryList;
				this.limitCountOfResult = loadData.limitCountOfResult;
				this.maximumDocumentMBSize = loadData.maximumDocumentMBSize;
				this.keywordMode = loadData.keywordMode;
				this.pathMode = loadData.pathMode;
			}
		}
	}
}
