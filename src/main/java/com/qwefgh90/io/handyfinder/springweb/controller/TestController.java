package com.qwefgh90.io.handyfinder.springweb.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.qwefgh90.io.handyfinder.springweb.service.TestService;

@Controller
public class TestController {

	@Autowired
	TestService service;
	
	@RequestMapping(path="/health")
	@ResponseBody
	public Health health(){
		return new Health();
	}
	
	class Health{
		boolean health = true;

		public boolean isHealth() {
			return health;
		}

		public void setHealth(boolean health) {
			this.health = health;
		}
		
	}
}
