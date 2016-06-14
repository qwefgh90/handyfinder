package com.qwefgh90.io.handyfinder.springweb.repository;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Repository;

import io.github.qwefgh90.handyfinder.lucene.LuceneHandlerBasicOptionView;
import io.github.qwefgh90.handyfinder.lucene.model.Directory;

/**
 * CRUD of index properties
 * 
 * @author choechangwon
 *
 */
@Repository
@DependsOn({ "dataSource" })
public class MetaRespository {

	private final static Logger LOG = LoggerFactory.getLogger(MetaRespository.class);
	@Autowired
	@Qualifier("dataSource")
	DataSource dataSource;

	@Autowired
	LuceneHandlerBasicOptionView appData;

	public void save(List<Directory> list) throws SQLException {
		for (Directory dir : list) {
			appData.setDirectory(dir);
		}
		appData.writeAppDataToDisk();
	}

	public void saveOne(Directory dir) throws SQLException {
		appData.setDirectory(dir);
		appData.writeAppDataToDisk();
	}

	public void deleteOne(Directory dir) throws SQLException {
		appData.deleteDirectory(dir);
		appData.writeAppDataToDisk();
	}

	public void deleteDirectories() throws SQLException {
		appData.deleteDirectories();
		appData.writeAppDataToDisk();
	}

	public List<Directory> selectDirectory() throws SQLException {
		List<Directory> list = appData.getDirectoryList();
		return list;
	}

}
