package io.github.qwefgh90.handyfinder.springweb.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.JsonObject;

import io.github.qwefgh90.handyfinder.gui.AppStartup;
import io.github.qwefgh90.handyfinder.lucene.model.Directory;
import io.github.qwefgh90.handyfinder.springweb.model.Command;
import io.github.qwefgh90.handyfinder.springweb.model.DocumentDto;
import io.github.qwefgh90.handyfinder.springweb.model.OptionDto;
import io.github.qwefgh90.handyfinder.springweb.model.SupportTypeDto;
import io.github.qwefgh90.handyfinder.springweb.service.RootService;

@RestController
public class RootController {

	private final static Logger LOG = LoggerFactory.getLogger(RootController.class);

	@Autowired
	RootService rootService;
	@Autowired
	SimpMessagingTemplate messaging;

	@RequestMapping(value = "/directories", method = RequestMethod.GET)
	public ResponseEntity<List<Directory>> getDirectories() {
		try {
			return new ResponseEntity<List<Directory>>(rootService.getDirectories(), HttpStatus.OK);
		} catch (SQLException e) {
			LOG.error(ExceptionUtils.getStackTrace(e));
			return new ResponseEntity<List<Directory>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(value = "/directories", method = RequestMethod.POST)
	public ResponseEntity<String> updateDirectories(@RequestBody ArrayList<Directory> list) {
		try {
			rootService.updateDirectories(list);
			return new ResponseEntity<String>(HttpStatus.OK);
		} catch (SQLException e) {
			LOG.error(ExceptionUtils.getStackTrace(e));
			return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(value = "/documents", method = RequestMethod.GET)
	public ResponseEntity<List<String>> search() {
		List<String> result;
		try {
			result = rootService.getTempPathForAllDocumentList();
			return new ResponseEntity<List<String>>(result, HttpStatus.OK);
		} catch (IOException e) {
			LOG.error(ExceptionUtils.getStackTrace(e));
			return new ResponseEntity<List<String>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}	

	@RequestMapping(value = "/documents", method = RequestMethod.GET, params="keyword")
	public ResponseEntity<List<DocumentDto>> search(@RequestParam String keyword) {
		Optional<List<DocumentDto>> result = Optional.empty();
		result = rootService.search(keyword);
		if (result.isPresent()) {
			return new ResponseEntity<List<DocumentDto>>(result.get(), HttpStatus.OK);
		} else {
			return new ResponseEntity<List<DocumentDto>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(value = "/documents/count", method = RequestMethod.GET)
	public ResponseEntity<Integer> getDocumentCount() {
		int count = rootService.getDocumentCount();
		return new ResponseEntity<Integer>(count, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/document/content", method = RequestMethod.GET)
	public ResponseEntity<String> search(@RequestParam String pathString, @RequestParam String keyword){
		Optional<String> contentResult = rootService.search(keyword, pathString);
		if(contentResult.isPresent()){
			return new ResponseEntity<String>(contentResult.get(), HttpStatus.OK);
		}else{
			return new ResponseEntity<String>("can't load content on your disk", HttpStatus.OK);
		}
	}
	
	@RequestMapping(value = "/supportType", method = RequestMethod.POST)
	public ResponseEntity<String> updateIndexType(@RequestBody SupportTypeDto supportType) {
		try {
			rootService.updateSupportType(supportType);
			return new ResponseEntity<String>(HttpStatus.OK);
		} catch (IOException e) {
			LOG.warn(ExceptionUtils.getStackTrace(e));
			return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(value = "/supportTypes", method = RequestMethod.GET)
	public ResponseEntity<List<SupportTypeDto>> getSupportTypes() {
		return new ResponseEntity<List<SupportTypeDto>>(rootService.getSupportType(), HttpStatus.OK);
	}

	@RequestMapping(value = "/supportTypes", method = RequestMethod.POST)
	public ResponseEntity<String> updateIndexType(@RequestBody List<SupportTypeDto> supportTypeList) {
		try {
			rootService.updateSupportsType(supportTypeList);
			return new ResponseEntity<String>(HttpStatus.OK);
		} catch (IOException e) {
			LOG.warn(ExceptionUtils.getStackTrace(e));
			return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(value = "/auth", method = RequestMethod.POST)
	public ResponseEntity<String> auth(@RequestBody Map<String,String> info) {
		String key = info.getOrDefault("key", null);
		if(key == null){
			return new ResponseEntity<String>(HttpStatus.UNAUTHORIZED);
		}else{
			if(AppStartup.secretKey.equals(key))
				return new ResponseEntity<String>(HttpStatus.OK);
			else
				return new ResponseEntity<String>(HttpStatus.UNAUTHORIZED);
		}
	}

	@RequestMapping(value = "/version", method = RequestMethod.GET)
	public ResponseEntity<Map<String,String>> getVersion() {
		return new ResponseEntity<Map<String,String>>(rootService.getVersion(), HttpStatus.OK);
	}

	@RequestMapping(value = "/onlineVersion", method = RequestMethod.GET)
	public ResponseEntity<Map<String,String>> getOnlineVersion() {
		return new ResponseEntity<Map<String,String>>(rootService.getOnlineVersion(), HttpStatus.OK);
	}
	
	@RequestMapping(value = "/options", method = RequestMethod.GET)
	public ResponseEntity<OptionDto> getOption() {
		return new ResponseEntity<OptionDto>(rootService.getOption(), HttpStatus.OK);
	}

	@RequestMapping(value = "/options", method = RequestMethod.POST)
	public ResponseEntity<String> setOption(@RequestBody OptionDto optionDto) {
		rootService.setOption(optionDto);
		return new ResponseEntity<String>(HttpStatus.OK);
	}

	/**
	 * @param command
	 *            "start", "stop"
	 * @param accessor
	 */
	@MessageMapping("/command/index/{command}")
	public void command1(@DestinationVariable String command, SimpMessageHeaderAccessor accessor) {
		if (command == null)
			return;
		Command dto;
		if (command.equals("start"))
			dto = Command.START_INDEXING;
		else if (command.equals("stop"))
			dto = Command.STOP_INDEXING;
		else if (command.equals("update"))
			dto = Command.UPDATE_INDEXING;
		else if (command.equals("openAndSelectDirectory"))
			dto = Command.OPEN_AND_SEND_DIRECTORY;
		else
			return;

		rootService.handleCommand(dto);
	}

	/**
	 * 
	 * @param command
	 *            "open"
	 * @param accessor
	 */
	@MessageMapping("/command/gui/{command}")
	public void commandGui(@Payload OpenCommand path, @DestinationVariable String command) {
		if (command.equals("open"))
			rootService.openDirectory(path.getPath());
		if (command.equals("open-file"))
			rootService.openFile(path.getPath());
		if (command.equals("open-home"))
			rootService.openURL(Optional.empty());
	}

	private static class OpenCommand {
		String path;

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

	}

	@MessageMapping("/hello")
	@Deprecated
	public void hello(String hello, SimpMessageHeaderAccessor accessor) {
		messaging.convertAndSend("/test/hi", "hi");
	}
}
