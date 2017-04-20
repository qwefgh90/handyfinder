package io.github.qwefgh90.handyfinder.lucene;

import io.github.qwefgh90.handyfinder.lucene.BasicOptionModel.KEYWORD_MODE;
import io.github.qwefgh90.handyfinder.lucene.BasicOptionModel.TARGET_MODE;
import io.github.qwefgh90.handyfinder.lucene.model.Directory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Function class for option
 * @author qwefgh90
 *
 */
public class BasicOption {
	private final static Logger LOG = LoggerFactory
			.getLogger(BasicOption.class);

	private static BasicOption singleton = null;
	private BasicOptionModel model;
	private Path appDataJsonPath;
	private static ObjectMapper om = new ObjectMapper();

	private BasicOption(Path appDataJsonPath) {
		try {
			this.appDataJsonPath = appDataJsonPath;
			this.model = loadAppDataFromDisk(appDataJsonPath);
		} catch (IOException e) {
			LOG.error(ExceptionUtils.getStackTrace(e));
			throw new RuntimeException(e.toString());
		}
	}

	/**
	 * 
	 * @return if file is not exist, return default object
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	static BasicOptionModel loadAppDataFromDisk(Path appDataJsonPath) throws JsonParseException,
	JsonMappingException, IOException {
		if (!Files.exists(appDataJsonPath))
			return new BasicOptionModel();
		else 
			return om.readValue(appDataJsonPath.toFile(), BasicOptionModel.class);
	}

	/**
	 * Write all option fields to json file
	 */
	public void writeAppDataToDisk() {
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


	public void deleteAppDataFromDisk() throws IOException {
		Path path = appDataJsonPath;
		if(path != null && Files.exists(path))
			Files.delete(path);
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

	public int getDiskUseLimit() {
		return model.getDiskUseLimit();
	}

	public void setDiskUseLimit(int diskUseLimit) {
		model.setDiskUseLimit(diskUseLimit);
	}
	
	public Path getAppDataJsonPath() {
		return appDataJsonPath;
	}
}
