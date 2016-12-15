package io.github.qwefgh90.handyfinder.lucene;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.qwefgh90.handyfinder.lucene.model.*;



@JsonIgnoreProperties(ignoreUnknown=true)
public class BasicOptionModel {
		private BasicOptionModel() {
			this.directoryList = new ArrayList<Directory>();
			this.limitCountOfResult = _limitCountOfResult;
			this.maximumDocumentMBSize = _maximumDocumentMBSize;
			this.keywordMode = _keywordMode;
			this.targetMode = EnumSet.of(TARGET_MODE.PATH, TARGET_MODE.CONTENT);
		}

		private final static Logger LOG = LoggerFactory
				.getLogger(BasicOptionModel.class);

		private final int _limitCountOfResult = 100;
		private final int _maximumDocumentMBSize = 100;
		private final KEYWORD_MODE _keywordMode = KEYWORD_MODE.OR;
		private final boolean _pathMode = true;

		public enum TARGET_MODE{
			PATH, CONTENT
		}
		
		public enum KEYWORD_MODE{
			OR, AND
		};

		private Path appDataJsonPath;
		private static BasicOptionModel singleton;
		private static ObjectMapper om = new ObjectMapper();

		private EnumSet<TARGET_MODE> targetMode;
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


		public EnumSet<TARGET_MODE> getTargetMode() {
			return targetMode;
		}

		public void setTargetMode(EnumSet<TARGET_MODE> targetMode) {
			this.targetMode = targetMode;
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
		static BasicOptionModel loadAppDataFromDisk(Path appDataJsonPath) throws JsonParseException,
		JsonMappingException, IOException {
			if (!Files.exists(appDataJsonPath))
				return null;
			BasicOptionModel app = om.readValue(appDataJsonPath.toFile(), BasicOptionModel.class);
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
		BasicOptionModel(Path appDataJsonPath) throws JsonParseException, JsonMappingException, IOException {
			this();
			this.appDataJsonPath = appDataJsonPath;
			BasicOptionModel loadData = BasicOptionModel.loadAppDataFromDisk(appDataJsonPath);
			if (loadData != null) {
				this.directoryList = loadData.directoryList;
				this.limitCountOfResult = loadData.limitCountOfResult;
				this.maximumDocumentMBSize = loadData.maximumDocumentMBSize;
				this.keywordMode = loadData.keywordMode;
				this.targetMode = loadData.targetMode;
			}
		}
}
