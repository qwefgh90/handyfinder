package io.github.qwefgh90.handyfinder.lucene;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.qwefgh90.handyfinder.lucene.model.*;

@JsonIgnoreProperties(ignoreUnknown=true)
public class BasicOptionModel {
		public BasicOptionModel() {
			this.directoryList = new ArrayList<Directory>();
			this.limitCountOfResult = _limitCountOfResult;
			this.maximumDocumentMBSize = _maximumDocumentMBSize;
			this.keywordMode = _keywordMode;
			this.diskUseLimit = _diskUseLimit;
			this.targetMode = EnumSet.of(TARGET_MODE.PATH, TARGET_MODE.CONTENT);
		}

		//default option values
		private final int _limitCountOfResult = 100;
		private final int _maximumDocumentMBSize = 20;
		private final KEYWORD_MODE _keywordMode = KEYWORD_MODE.OR;
		private final int _diskUseLimit = 100;

		/**
		 * json fields
		 */

		public enum TARGET_MODE{
			PATH, CONTENT
		}
		
		public enum KEYWORD_MODE{
			OR, AND
		};

		private EnumSet<TARGET_MODE> targetMode;
		private List<Directory> directoryList;
		private int limitCountOfResult;
		private int maximumDocumentMBSize;
		private KEYWORD_MODE keywordMode;
		private int diskUseLimit;
		

		public int getDiskUseLimit() {
			return diskUseLimit;
		}

		public void setDiskUseLimit(int diskUseLimit) {
			this.diskUseLimit = diskUseLimit;
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
}
