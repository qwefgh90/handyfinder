package web;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
@EnableWebMvc
@ComponentScan(basePackages = {"web"})
public class ServletContext extends WebMvcConfigurerAdapter {

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
}
