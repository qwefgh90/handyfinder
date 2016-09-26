package io.github.qwefgh90.handyfinder.springweb.config;

import java.util.List;

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


@Configuration
@EnableWebMvc
@ComponentScan(basePackages = {"io.github.qwefgh90.handyfinder.springweb.config","cio.github.qwefgh90.handyfinder.springweb.service","io.github.qwefgh90.handyfinder.springweb.controller"})
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
	
	@Bean(name="objectMapper")
	public ObjectMapper opjectMapper(){
		ObjectMapper om = new ObjectMapper();
		return om;
	}
	
	
}
