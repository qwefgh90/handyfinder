package com.qwefgh90.io.handyfinder.springweb.controller;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.qwefgh90.io.handfinder.springweb.model.Directory;
import com.qwefgh90.io.handyfinder.springweb.service.RootService;

@RestController
public class RootController {

	Log log = LogFactory.getLog(RootController.class);

	@Autowired
	RootService rootService;

	@RequestMapping(value = "/getDirectories", method = RequestMethod.GET)
	public ResponseEntity<List<Directory>> getDirectories() {
		try {
			return new ResponseEntity<List<Directory>>(rootService.getDirectories(), HttpStatus.OK);
		} catch (SQLException e) {
			log.error(ExceptionUtils.getStackTrace(e));
			return new ResponseEntity<List<Directory>>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(value = "/updateDirectories", method = RequestMethod.POST)
	public ResponseEntity<String> updateDirectories(@RequestBody ArrayList<Directory> list) {
		try {
			rootService.updateDirectories(list);
			return new ResponseEntity<String>(HttpStatus.OK);
		} catch (SQLException e) {
			log.error(ExceptionUtils.getStackTrace(e));
			return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	
}
