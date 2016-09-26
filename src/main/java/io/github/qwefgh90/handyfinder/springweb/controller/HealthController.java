package io.github.qwefgh90.handyfinder.springweb.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import io.github.qwefgh90.handyfinder.springweb.config.ServletContextTest;

@Controller
public class HealthController {

	private final static Logger LOG = LoggerFactory.getLogger(HealthController.class);
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
