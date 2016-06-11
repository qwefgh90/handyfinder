package com.qwefgh90.io.handyfinder.springweb.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang.exception.ExceptionUtils;
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

import com.qwefgh90.io.handyfinder.springweb.model.CommandDto;
import com.qwefgh90.io.handyfinder.springweb.model.CommandDto.COMMAND;
import com.qwefgh90.io.handyfinder.springweb.model.Directory;
import com.qwefgh90.io.handyfinder.springweb.model.DocumentDto;
import com.qwefgh90.io.handyfinder.springweb.model.SupportTypeDto;
import com.qwefgh90.io.handyfinder.springweb.service.RootService;

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
	public ResponseEntity<List<DocumentDto>> search(@RequestParam String keyword) {
		Optional<List<DocumentDto>> result = Optional.empty();
		result = rootService.search(keyword);
		if (result.isPresent()) {
			return new ResponseEntity<List<DocumentDto>>(result.get(), HttpStatus.OK);
		} else {
			return new ResponseEntity<List<DocumentDto>>(HttpStatus.INTERNAL_SERVER_ERROR);
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

	/**
	 * @param command
	 *            "start", "stop"
	 * @param accessor
	 */
	@MessageMapping("/command/index/{command}")
	public void command1(@DestinationVariable String command, SimpMessageHeaderAccessor accessor) {
		if (command == null)
			return;
		COMMAND dto;
		if (command.equals("start"))
			dto = CommandDto.COMMAND.START_INDEXING;
		else if (command.equals("stop"))
			dto = CommandDto.COMMAND.STOP_INDEXING;
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
