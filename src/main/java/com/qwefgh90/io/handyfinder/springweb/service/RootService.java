package com.qwefgh90.io.handyfinder.springweb.service;

import java.awt.Desktop;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.tika.mime.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qwefgh90.io.handyfinder.springweb.model.CommandDto.COMMAND;
import com.qwefgh90.io.handyfinder.springweb.model.DocumentDto;
import com.qwefgh90.io.handyfinder.springweb.model.OptionDto;
import com.qwefgh90.io.handyfinder.springweb.model.SupportTypeDto;
import com.qwefgh90.io.handyfinder.springweb.repository.MetaRespository;
import com.qwefgh90.io.handyfinder.springweb.websocket.CommandInvoker;
import com.qwefgh90.io.jsearch.FileExtension;

import io.github.qwefgh90.handyfinder.lucene.LuceneHandlerBasicOptionView;
import io.github.qwefgh90.handyfinder.lucene.LuceneHandler;
import io.github.qwefgh90.handyfinder.lucene.TikaMimeXmlObject;
import io.github.qwefgh90.handyfinder.lucene.LuceneHandler.IndexException;
import io.github.qwefgh90.handyfinder.lucene.model.Directory;

@Service
public class RootService {

	private final static Logger LOG = LoggerFactory.getLogger(RootService.class);
	@Autowired
	CommandInvoker invokerForCommand;

	@Autowired
	MetaRespository indexProperty;

	@Autowired
	LuceneHandler handler;

	@Autowired
	TikaMimeXmlObject tikaMimeObject;
	
	@Autowired
	LuceneHandlerBasicOptionView globalAppData;
	
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
	 * 
	 * @param list
	 * @throws SQLException
	 */
	public void updateDirectories(List<Directory> list) throws SQLException {
		indexProperty.save(list);
	}
	
	/**
	 * update support type and save to disk
	 * @param item
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void updateSupportType(SupportTypeDto item) throws FileNotFoundException, IOException{
		tikaMimeObject.setGlob(item.getType(), item.isUsed());
		tikaMimeObject.updateGlobPropertiesFile();
	}
	
	/**
	 * 
	 * @return list of support type
	 */
	public List<SupportTypeDto> getSupportType(){
		Map<String, Boolean> map = tikaMimeObject.getGlobMap();
		List<SupportTypeDto> result = new ArrayList<>();
		Iterator<Entry<String, Boolean>> iter = map.entrySet().iterator();
		while(iter.hasNext()){
			Entry<String, Boolean> entry = iter.next();
			SupportTypeDto dto = new SupportTypeDto();
			dto.setType(entry.getKey());
			dto.setUsed(entry.getValue());
			result.add(dto);
		}
		return result;
	}

	/**
	 * 
	 * @return lucene handler option dto
	 */
	public OptionDto getOption(){
		OptionDto dto = new OptionDto();
		dto.setLimitCountOfResult(globalAppData.getLimitCountOfResult());
		dto.setMaximumDocumentMBSize(globalAppData.getMaximumDocumentMBSize());
		return dto;
	}
	
	/**
	 * 
	 * @param dto option which will be applied
	 */
	public void setOption(OptionDto dto){
		globalAppData.setLimitCountOfResult(dto.getLimitCountOfResult());
		globalAppData.setLimitCountOfResult(dto.getMaximumDocumentMBSize());
	}
	
	public void closeAppLucene() throws IOException{
		handler.close();
	}

	public Optional<List<DocumentDto>> search(String keyword) {
		List<DocumentDto> list = new ArrayList<>();
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
				} catch (com.qwefgh90.io.jsearch.JSearch.ParseException e) {
					LOG.info(e.toString());
					continue;
				} catch (Exception e){
					LOG.warn(ExceptionUtils.getStackTrace(e));
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
					LOG.warn(e.toString());
				}
			}
		}
	}
	
	public void openFile(String pathStr){
		Path path = Paths.get(pathStr);
		if (Files.exists(path) && Files.isRegularFile(path) && !Files.isExecutable(path)) {
			try {
				MediaType mime = FileExtension.getContentType(path.toFile(), path.getFileName().toString());
				
				//if ok, run program
				if (Desktop.isDesktopSupported()) {
					try {
						Desktop.getDesktop().open(path.toFile());
					} catch (IOException e) {
						LOG.warn(e.toString());
					}
				}
			} catch (IOException e) {
				LOG.warn(ExceptionUtils.getStackTrace(e));
			}
			
		}
	}
}
