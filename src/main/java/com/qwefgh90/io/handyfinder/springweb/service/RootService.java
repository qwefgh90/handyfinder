package com.qwefgh90.io.handyfinder.springweb.service;

import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qwefgh90.io.handfinder.springweb.model.Directory;
import com.qwefgh90.io.handyfinder.springweb.repository.IndexProperty;

@Service
public class RootService {

	@Autowired
	IndexProperty indexProperty;

	public List<Directory> getDirectories() throws SQLException {
		return indexProperty.selectDirectory();
	}

	/**
	 * no transaction, no required.
	 * @param list
	 * @throws SQLException
	 */
	public void updateDirectories(List<Directory> list) throws SQLException {
		indexProperty.deleteDirectories();
		indexProperty.save(list);
	}
}
