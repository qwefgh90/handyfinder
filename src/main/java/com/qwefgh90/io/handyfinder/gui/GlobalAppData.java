package com.qwefgh90.io.handyfinder.gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.bouncycastle.crypto.RuntimeCryptoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qwefgh90.io.handyfinder.springweb.model.Directory;

@JsonIgnoreProperties(value = { "singleton", "om", "LOG" })
class GlobalAppData {
	private final static Logger LOG = LoggerFactory
			.getLogger(GlobalAppData.class);

	private static GlobalAppData singleton;
	private static ObjectMapper om = new ObjectMapper();

	private List<Directory> directoryList;
	private int limitOfSearch;

	private GlobalAppData() throws JsonParseException, JsonMappingException,
			IOException {
		this.directoryList = new ArrayList<Directory>();
		this.limitOfSearch = 100;
	}

	//for Object to JSON public visibility
	public List<Directory> getDirectoryList() {
		return directoryList;
	}

	public void setDirectoryList(List<Directory> directoryList) {
		this.directoryList = directoryList;
	}

	public int getLimitOfSearch() {
		return limitOfSearch;
	}

	public void setLimitOfSearch(int limitOfSearch) {
		this.limitOfSearch = limitOfSearch;
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
	GlobalAppData loadAppDataFromDisk() throws JsonParseException,
			JsonMappingException, IOException {
		Path path = AppStartupConfig.appDataJsonPath;
		if (!Files.exists(path))
			return null;
		GlobalAppData app = om.readValue(path.toFile(), GlobalAppData.class);
		return app;
	}

	void deleteAppDataFromDisk() throws IOException {
		Path path = AppStartupConfig.appDataJsonPath;
		if(Files.exists(path))
			Files.delete(path);
	}

	/**
	 * 
	 * @return
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	static GlobalAppData getInstance() throws JsonParseException,
			JsonMappingException, IOException {
		if (singleton == null) {
			singleton = new GlobalAppData();
			GlobalAppData appData = singleton.loadAppDataFromDisk();
			if (appData == null) {
			} else {
				singleton.directoryList = appData.directoryList;
				singleton.limitOfSearch = appData.limitOfSearch;
			}
		}
		return singleton;
	}
}
