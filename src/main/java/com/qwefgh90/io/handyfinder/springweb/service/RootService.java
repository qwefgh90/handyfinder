package com.qwefgh90.io.handyfinder.springweb.service;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qwefgh90.io.handfinder.springweb.model.Directory;
import com.qwefgh90.io.handyfinder.gui.AppStartupConfig;
import com.qwefgh90.io.handyfinder.springweb.repository.IndexProperty;
import com.qwefgh90.io.jsearch.JSearch;
import com.qwefgh90.io.jsearch.JSearch.ParseException;

@Service
public class RootService {

	@Autowired
	IndexProperty indexProperty;

	private LuceneHandler handler;

	@PostConstruct
	private void after() {
		handler = LuceneHandler.getInstance(AppStartupConfig.pathForAppdata.resolve("index"));
	}
	public List<Directory> getDirectories() throws SQLException {
		return indexProperty.selectDirectory();
	}

	/**
	 * no transaction, no required.
	 * 
	 * @param list
	 * @throws SQLException
	 */
	public void updateDirectories(List<Directory> list) throws SQLException {
		indexProperty.deleteDirectories();
		indexProperty.save(list);
	}
	

}
