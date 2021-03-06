package io.github.qwefgh90.handyfinder.springweb.repository;

import java.sql.SQLException;
import java.util.List;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import io.github.qwefgh90.handyfinder.lucene.BasicOption;
import io.github.qwefgh90.handyfinder.lucene.model.Directory;

/**
 * CRUD of index properties
 * 
 * @author choechangwon
 *
 */
@Repository
public class MetaRespository {

	private final static Logger LOG = LoggerFactory.getLogger(MetaRespository.class);

	@Autowired
	BasicOption appData;

	public void save(List<Directory> list) throws SQLException {
		appData.deleteDirectories();
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

	public List<Directory> selectDirectory() {
		List<Directory> list = appData.getDirectoryList();
		return list;
	}

}
