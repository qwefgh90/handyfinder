package web;

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

public class ContainerInitializer extends
		AbstractAnnotationConfigDispatcherServletInitializer {

	@Override
	protected Class<?>[] getRootConfigClasses() {
		//root-context.xml
		return new Class<?>[]{RootContext.class};
	}

	@Override
	protected Class<?>[] getServletConfigClasses() {
		//myservlet-context.xml
		return new Class<?>[]{ServletContext.class};
	}

	@Override
	protected String[] getServletMappings() {
		//root url
		return new String[]{"/"}; 
	}

}
