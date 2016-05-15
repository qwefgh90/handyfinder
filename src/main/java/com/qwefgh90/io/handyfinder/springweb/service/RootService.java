package com.qwefgh90.io.handyfinder.springweb.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.search.TopDocs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qwefgh90.io.handyfinder.gui.AppStartupConfig;
import com.qwefgh90.io.handyfinder.springweb.model.CommandDto;
import com.qwefgh90.io.handyfinder.springweb.model.CommandDto.COMMAND;
import com.qwefgh90.io.handyfinder.springweb.model.Directory;
import com.qwefgh90.io.handyfinder.springweb.model.DocumentDto;
import com.qwefgh90.io.handyfinder.springweb.repository.MetaRespository;
import com.qwefgh90.io.handyfinder.springweb.service.LuceneHandler.IndexException;
import com.qwefgh90.io.handyfinder.springweb.websocket.InteractionInvoker;

@Service
public class RootService {

	@Autowired
	InteractionInvoker invokerForCommand;

	@Autowired
	MetaRespository indexProperty;

	Log log = LogFactory.getLog(RootService.class);

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

	public Optional<List<DocumentDto>> search(String keyword) throws IndexException {
		List<DocumentDto> list = new ArrayList<>();
		LuceneHandler handler = LuceneHandler.getInstance(AppStartupConfig.pathForIndex, invokerForCommand);
		try {
			TopDocs docs = handler.search(keyword);
			for (int i = 0; i < docs.scoreDocs.length; i++) {
				Document document = handler.getDocument(docs.scoreDocs[i].doc);
				DocumentDto dto = new DocumentDto();

				dto.setContents(document.get("contents"));
				dto.setPathString(document.get("pathString"));
				list.add(dto);
			}
			return Optional.of(list);
		} catch (QueryNodeException e) {
			log.info(ExceptionUtils.getStackTrace(e));
		} catch (IOException e) {
			log.info(ExceptionUtils.getStackTrace(e));
		} 
		return Optional.empty();
	}

	public void handleCommand(CommandDto command) {
		if (COMMAND.values().length <= command.getCommand())
			throw new IllegalArgumentException();
		else {
			COMMAND inputCommand = COMMAND.values()[command.getCommand()];
			switch (inputCommand) {
			case START_INDEXING: {
				try {
					List<Directory> list = indexProperty.selectDirectory();
					LuceneHandler handler = LuceneHandler.getInstance(AppStartupConfig.pathForIndex, invokerForCommand);
					handler.indexDirectories(list);
					return;
				} catch (SQLException e) {
					log.info(ExceptionUtils.getStackTrace(e));
					//terminate command
				} catch (IOException e) {
					log.info(ExceptionUtils.getStackTrace(e));
					//terminate command
				} catch (IndexException e) {
					log.info(ExceptionUtils.getStackTrace(e));
					//terminate command
				}
				break;
			}
			default: {
				break;
			}
			}
		}
	}

}
