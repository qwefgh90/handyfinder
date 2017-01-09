package io.github.qwefgh90.handyfinder.lucene;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.qwefgh90.handyfinder.lucene.BasicOptionModel.KEYWORD_MODE;
import io.github.qwefgh90.handyfinder.lucene.BasicOptionModel.TARGET_MODE;
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


	public EnumSet<TARGET_MODE> getTargetMode() {
		return model.getTargetMode();
	}

	public void setTargetMode(EnumSet<TARGET_MODE> targetMode) {
		model.setTargetMode(targetMode);
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

	public int getMaximumCapacityPercent() {
		return model.getMaximumCapacityPercent();
	}

	public void setMaximumCapacityPercent(int maximumCapacityPercent) {
		model.setMaximumCapacityPercent(maximumCapacityPercent);
	}

	/*public static class BasicOptionModel {

	}*/
}
