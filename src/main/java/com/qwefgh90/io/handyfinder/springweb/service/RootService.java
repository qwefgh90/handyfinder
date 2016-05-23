package com.qwefgh90.io.handyfinder.springweb.service;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qwefgh90.io.handyfinder.gui.AppStartupConfig;
import com.qwefgh90.io.handyfinder.springweb.model.CommandDto;
import com.qwefgh90.io.handyfinder.springweb.model.CommandDto.COMMAND;
import com.qwefgh90.io.handyfinder.springweb.model.Directory;
import com.qwefgh90.io.handyfinder.springweb.model.DocumentDto;
import com.qwefgh90.io.handyfinder.springweb.repository.MetaRespository;
import com.qwefgh90.io.handyfinder.springweb.service.LuceneHandler.IndexException;
import com.qwefgh90.io.handyfinder.springweb.websocket.CommandInvoker;

@Service
public class RootService {

	private final static Logger LOG = LoggerFactory.getLogger(RootService.class);
	@Autowired
	CommandInvoker invokerForCommand;

	@Autowired
	MetaRespository indexProperty;


	/**
	 * Get directories to be indexed.
	 * 
	 * @return
	 * @throws SQLException
	 */
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
	
	public void closeAppLucene() throws IOException{
		LuceneHandler handler = LuceneHandler.getInstance(AppStartupConfig.pathForIndex, invokerForCommand);
		handler.close();
	}

	public Optional<List<DocumentDto>> search(String keyword) throws IndexException {
		List<DocumentDto> list = new ArrayList<>();
		LuceneHandler handler = LuceneHandler.getInstance(AppStartupConfig.pathForIndex, invokerForCommand);
		try {
			TopDocs docs = handler.search(keyword);
			for (int i = 0; i < docs.scoreDocs.length; i++) {
				Document document = handler.getDocument(docs.scoreDocs[i].doc);
				DocumentDto dto = new DocumentDto();
				String highlightTag;
				try {
					highlightTag = handler.highlight(docs.scoreDocs[i].doc, keyword);
				} catch (ParseException e) {
					LOG.info(e.toString());
					continue;
				} catch (InvalidTokenOffsetsException e) {
					LOG.info(e.toString());
					continue;
				}

				dto.setCreatedTime(document.getField("createdTime").numericValue().longValue());
				dto.setTitle(document.get("title"));
				dto.setContents(highlightTag);
				dto.setPathString(document.get("pathString"));
				dto.setParentPathString(Paths.get(document.get("pathString")).getParent().toAbsolutePath().toString());

				Path path = Paths.get(dto.getPathString());
				dto.setModifiedTime(Files.getLastModifiedTime(path).toMillis());
				list.add(dto);
			}
			return Optional.of(list);
		} catch (QueryNodeException e) {
			LOG.info(ExceptionUtils.getStackTrace(e));
		} catch (IOException e) {
			LOG.info(ExceptionUtils.getStackTrace(e));
		}
		return Optional.empty();
	}

	public void handleCommand(COMMAND command) {
		COMMAND inputCommand = command;
		switch (inputCommand) {
		case START_INDEXING: {
			try {
				List<Directory> list = indexProperty.selectDirectory();
				LuceneHandler handler = LuceneHandler.getInstance(AppStartupConfig.pathForIndex, invokerForCommand);
				handler.startIndex(list);
				return;
			} catch (SQLException e) {
				LOG.info(ExceptionUtils.getStackTrace(e));
				// terminate command
			} catch (IOException e) {
				LOG.info(ExceptionUtils.getStackTrace(e));
				// terminate command
			} catch (IndexException e) {
				LOG.info(ExceptionUtils.getStackTrace(e));
				// terminate command
			}
			break;
		}
		default: {
			break;
		}
		}
	}

	public void openDirectory(String pathStr) {
		Path path = Paths.get(pathStr);
		if (Files.exists(path) && Files.isDirectory(path)) {
			if (Desktop.isDesktopSupported()) {
				try {
					Desktop.getDesktop().open(path.toFile());
				} catch (IOException e) {
					LOG.info(e.toString());
				}
			}
		}
	}
}