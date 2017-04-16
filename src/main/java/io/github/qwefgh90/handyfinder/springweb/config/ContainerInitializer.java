package io.github.qwefgh90.handyfinder.springweb.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import io.github.qwefgh90.handyfinder.gui.AppStartup;
import io.github.qwefgh90.handyfinder.gui.Java2JavascriptUtils;

public class ContainerInitializer extends
		AbstractAnnotationConfigDispatcherServletInitializer {

	private final static Logger LOG = LoggerFactory.getLogger(ContainerInitializer.class);
	@Override
	protected Class<?>[] getRootConfigClasses() {
		//root-context.xml
		return new Class<?>[]{RootContext.class};
	}

	@Override
	protected Class<?>[] getServletConfigClasses() {
		//myservlet-context.xml
		return new Class<?>[]{ServletContext.class, RootWebSocketConfig.class, AppDataConfig.class};
	}

	@Override
	protected String[] getServletMappings() {
		//root url
		return new String[]{"/"}; 
	}

	@Override
	protected WebApplicationContext createRootApplicationContext() {
		// TODO Auto-generated method stub
		WebApplicationContext context = super.createRootApplicationContext();
		AppStartup.setRootAppContext(context);
		return context;
	}

	@Override
	protected WebApplicationContext createServletApplicationContext() {
		// TODO Auto-generated method stub
		WebApplicationContext context = super.createServletApplicationContext();
		AppStartup.setServletAppContext(context);
		return context;
	}

}
