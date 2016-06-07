package com.qwefgh90.io.handyfinder.gui;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qwefgh90.io.handyfinder.springweb.model.Directory;

public class GlobalAppDataView {
	private final static Logger LOG = LoggerFactory
			.getLogger(GlobalAppDataView.class);

	private GlobalAppData app;

	private GlobalAppDataView(){
		try {
			app = GlobalAppData.getInstance();
		} catch (IOException e) {
			LOG.error(ExceptionUtils.getStackTrace(e));
			throw new RuntimeException(e.toString());
		}
	}
	
	private static GlobalAppDataView singleton = null;
	
	public static GlobalAppDataView getInstance(){
		if(singleton==null)
			singleton = new GlobalAppDataView();
		return singleton;
	}

	public int getLimitOfSearch() {
		return app.getLimitOfSearch();
	}

	public List<Directory> getDirectoryList() {
		return Collections.unmodifiableList(app.getDirectoryList());
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
	
	public void deleteDirectories(){
		app.getDirectoryList().clear();
	}

	public void writeAppDataToDisk() {
		app.writeAppDataToDisk();
	}

	public void deleteAppDataFromDisk() throws IOException {
		app.deleteAppDataFromDisk();
	}
}
