package io.github.qwefgh90.handyfinder.springweb.service;

import io.github.qwefgh90.handyfinder.gui.AppStartupConfig;
import io.github.qwefgh90.handyfinder.lucene.BasicOption;
import io.github.qwefgh90.handyfinder.lucene.BasicOption.BasicOptionModel.TARGET_MODE;
import io.github.qwefgh90.handyfinder.lucene.LuceneHandler;
import io.github.qwefgh90.handyfinder.lucene.MimeOption;
import io.github.qwefgh90.handyfinder.lucene.model.Directory;
import io.github.qwefgh90.handyfinder.springweb.model.Command;
import io.github.qwefgh90.handyfinder.springweb.model.DocumentDto;
import io.github.qwefgh90.handyfinder.springweb.model.OptionDto;
import io.github.qwefgh90.handyfinder.springweb.model.SupportTypeDto;
import io.github.qwefgh90.handyfinder.springweb.repository.MetaRespository;
import io.github.qwefgh90.handyfinder.springweb.websocket.CommandInvoker;
import io.github.qwefgh90.jsearch.JSearch;

import java.awt.Desktop;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumSet;
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

@Service
public class RootService {

	private final static Logger LOG = LoggerFactory
			.getLogger(RootService.class);
	@Autowired
	CommandInvoker invokerForCommand;

	@Autowired
	MetaRespository indexProperty;

	@Autowired
	LuceneHandler handler;

	@Autowired
	MimeOption tikaMimeObject;

	@Autowired
	BasicOption globalAppData;

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
	 * 
	 * @param item
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void updateSupportType(SupportTypeDto typeDto)
			throws FileNotFoundException, IOException {
		tikaMimeObject.setGlob(typeDto.getType(), typeDto.isUsed());
		tikaMimeObject.updateGlobPropertiesFile();
	}

	/**
	 * update support type and save to disk
	 * 
	 * @param item
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void updateSupportsType(List<SupportTypeDto> typeDtoList)
			throws FileNotFoundException, IOException {
		for (SupportTypeDto typeDto : typeDtoList) {
			tikaMimeObject.setGlob(typeDto.getType(), typeDto.isUsed());
		}
		tikaMimeObject.updateGlobPropertiesFile();
	}

	/**
	 * 
	 * @return list of support type
	 */
	public List<SupportTypeDto> getSupportType() {
		Map<String, Boolean> map = tikaMimeObject.getImmutableGlobMap();
		List<SupportTypeDto> result = new ArrayList<>();
		Iterator<Entry<String, Boolean>> iter = map.entrySet().iterator();
		while (iter.hasNext()) {
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
	public OptionDto getOption() {
		OptionDto dto = new OptionDto();
		dto.setLimitCountOfResult(globalAppData.getLimitCountOfResult());
		dto.setMaximumDocumentMBSize(globalAppData.getMaximumDocumentMBSize());
		dto.setKeywordMode(globalAppData.getKeywordMode().name());
		dto.setFirstStart(globalAppData.getDirectoryList().size() == 0 ? true : false);
		//dto.setPathMode(globalAppData.getTargetMode());
		dto.setPathTarget(globalAppData.getTargetMode().contains(TARGET_MODE.PATH));
		dto.setContentTarget(globalAppData.getTargetMode().contains(TARGET_MODE.CONTENT));
		
		return dto;
	}

	/**
	 * 
	 * @param option will be applied
	 */
	public void setOption(OptionDto dto) {
		if (dto.getLimitCountOfResult() > 0)
			globalAppData.setLimitCountOfResult(dto.getLimitCountOfResult());
		if (dto.getMaximumDocumentMBSize() > 0)
			globalAppData.setMaximumDocumentMBSize(dto
					.getMaximumDocumentMBSize());
		globalAppData.setKeywordMode(dto.getKeywordMode());
		EnumSet<TARGET_MODE> targetMode = EnumSet.noneOf(TARGET_MODE.class);
		if(dto.isPathTarget())
			targetMode.add(TARGET_MODE.PATH);
		if(dto.isContentTarget())
			targetMode.add(TARGET_MODE.CONTENT);
		globalAppData.setTargetMode(targetMode);
		globalAppData.writeAppDataToDisk();
	}

	public void closeAppLucene() throws IOException {
		handler.close();
	}

	public int getDocumentCount() {
		return handler.getDocumentCount();
	}
	
	public List<String> getTempPathForAllDocumentList() throws IOException{
		return handler.getDocumentPathList();
	}

	public Optional<String> search(String keyword, String pathString) {
		try {
			Optional<Map.Entry<String, String>> result = handler.highlight(
					pathString, keyword).call();
			if (!result.isPresent())
				return Optional.empty();
			return Optional.of(result.get().getValue());
		} catch (ParseException | IOException | InvalidTokenOffsetsException
				| QueryNodeException e) {
			LOG.warn(ExceptionUtils.getStackTrace(e));
		} catch (Exception e) {
			LOG.warn(ExceptionUtils.getStackTrace(e));
		}
		return Optional.empty();
	}

	public Optional<List<DocumentDto>> search(String keyword) {
		// HashMap<String, DocumentDto> docMap = new HashMap<>();
		List<DocumentDto> list = new ArrayList<>();
		// List<Callable<Optional<Map.Entry<String, String>>>> functionList =
		// new ArrayList<>();
		try {
			TopDocs docs = handler.search(keyword);
			// ExecutorService executor = Executors.newWorkStealingPool();
			for (int i = 0; i < docs.scoreDocs.length; i++) {
				Document document = handler.getDocument(docs.scoreDocs[i].doc);
				DocumentDto dto = new DocumentDto();
				String pathString = document.get("pathString");
				Path path = Paths.get(pathString);
				// Callable<Optional<Map.Entry<String, String>>>
				// getHightlightContent;
				if (Files.exists(path)) {
					dto.setExist(true);
					dto.setModifiedTime(Files.getLastModifiedTime(path)
							.toMillis());
					dto.setFileSize(Files.size(path));
					/*
					 * try { getHightlightContent =
					 * handler.highlight(docs.scoreDocs[i].doc, keyword);
					 * functionList.add(getHightlightContent); } catch
					 * (ParseException e) { LOG.warn(e.toString()); continue; }
					 * catch (InvalidTokenOffsetsException e) {
					 * LOG.warn(e.toString()); continue; } catch
					 * (com.qwefgh90.io.jsearch.JSearch.ParseException e) {
					 * LOG.warn(e.toString()); continue; } catch (IOException e)
					 * { LOG.warn(e.toString()); continue; }
					 */
				} else {
					dto.setExist(false);
					dto.setModifiedTime(document.getField("lastModifiedTime")
							.numericValue().longValue());
					dto.setFileSize(-1);
				}

				dto.setCreatedTime(document.getField("createdTime")
						.numericValue().longValue());
				dto.setTitle(document.get("title"));
				dto.setContents("<br> content is loading...</br>");
				dto.setPathString(document.get("pathString"));
				dto.setParentPathString(Paths.get(document.get("pathString"))
						.getParent().toAbsolutePath().toString());
				dto.setMimeType(document.get("mimeType"));
				// docMap.put(dto.getPathString(), dto);
				list.add(dto);
			}
			/*
			 * try { List<Future<Optional<Map.Entry<String, String>>>>
			 * futureList = executor.invokeAll(functionList);
			 * futureList.parallelStream().forEach(future -> { try {
			 * Optional<Map.Entry<String, String>> result = future.get();
			 * if(!result.isPresent()){ return; } String pathString =
			 * result.get().getKey(); String contents = result.get().getValue();
			 * docMap.get(pathString).setContents(contents); } catch (Exception
			 * e) { LOG.warn(e.toString()); }
			 * 
			 * }); } catch (InterruptedException e) { LOG.warn(e.toString()); }
			 * executor.shutdown();
			 */
			return Optional.of(list);
		} catch (QueryNodeException e) {
			LOG.info(ExceptionUtils.getStackTrace(e));
		} catch (IOException e) {
			LOG.info(ExceptionUtils.getStackTrace(e));
		}
		return Optional.empty();
	}

	public void handleCommand(Command command) {
		Command inputCommand = command;
		switch (inputCommand) {
		case START_INDEXING: {
			try {
				List<Directory> list = indexProperty.selectDirectory();
				if (handler.isReady())
					handler.startIndex(list);
				return;
			} catch (SQLException e) {
				LOG.warn(ExceptionUtils.getStackTrace(e));
			} catch (IOException e) {
				LOG.warn(ExceptionUtils.getStackTrace(e));
			}
			break;
		}
		case UPDATE_INDEXING: {
			List<Directory> list;
			try {
				list = indexProperty.selectDirectory();
				if (handler.isReady())
					handler.updateIndexedDocuments(list);
			} catch (SQLException e) {
				LOG.warn(ExceptionUtils.getStackTrace(e));
			}
			break;
		}
		case STOP_INDEXING: {
			handler.stopIndex();
			break;
		}
		case OPEN_AND_SEND_DIRECTORY: {
			this.openAndSendDirectory(); // async
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

	public void openHomeURL() {
		Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop()
				: null;
		if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
			try {
				desktop.browse(URI.create(AppStartupConfig.homeUrl));
			} catch (Exception e) {
				LOG.warn(e.toString());
			}
		}
	}

	public void openFile(String pathStr) {
		Path path = Paths.get(pathStr);
		if (Files.exists(path) && Files.isRegularFile(path)) {
			try {
				MediaType mime = JSearch.getContentType(path.toFile(),
						path.getFileName().toString());

				// if ok, run program
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

	private void openAndSendDirectory() {
		invokerForCommand.openAndSendSelectedDirectory();
	}
}
