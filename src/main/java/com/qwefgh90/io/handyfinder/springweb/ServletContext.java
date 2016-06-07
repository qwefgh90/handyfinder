package com.qwefgh90.io.handyfinder.springweb;

import java.util.List;

import javax.sql.DataSource;

import org.apache.derby.jdbc.EmbeddedDataSource;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qwefgh90.io.handyfinder.gui.AppStartupConfig;
import com.qwefgh90.io.handyfinder.springweb.repository.Query;

@Configuration
@EnableWebMvc
@ComponentScan(basePackages = {"com.qwefgh90.io.handyfinder.springweb","com.qwefgh90.io.handyfinder.springweb.service","com.qwefgh90.io.handyfinder.springweb.controller"})
public class ServletContext extends WebMvcConfigurerAdapter {

	private final static Logger LOG = LoggerFactory.getLogger(ServletContext.class);
	
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/app/**").addResourceLocations("/app/");	//classpath 기준으로 정적 파일들을 등록해주는 함수
	}

	@Override
	public void configureMessageConverters( List<HttpMessageConverter<?>> converters ) {
		converters.add(jsonMessageConverter());
	}
	
	MappingJackson2HttpMessageConverter jsonMessageConverter() {

		return new MappingJackson2HttpMessageConverter();
	}
	
	@Bean(name="dataSource")
	public DataSource dataSource(){
		EmbeddedDataSource ds = new EmbeddedDataSource();
		ds.setDatabaseName(AppStartupConfig.pathForDatabase.toString());
		ds.setCreateDatabase("create");
		LOG.info(ds.toString());
		return ds;
	}
	
	@Bean(name="objectMapper")
	public ObjectMapper opjectMapper(){
		ObjectMapper om = new ObjectMapper();
		return om;
	}
	
	
}
