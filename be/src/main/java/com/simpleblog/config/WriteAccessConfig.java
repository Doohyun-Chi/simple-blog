package com.simpleblog.config;

import com.simpleblog.component.WriteAccessInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Puts the write guard in front of the post endpoints, when it is enabled; see WriteAccessInterceptor. */
@Configuration
@ConditionalOnProperty(name = "simpleblog.write.access.enabled", havingValue = "true")
public class WriteAccessConfig implements WebMvcConfigurer {

	private final WriteAccessInterceptor writeAccessInterceptor;

	public WriteAccessConfig(WriteAccessInterceptor writeAccessInterceptor) {
		this.writeAccessInterceptor = writeAccessInterceptor;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(this.writeAccessInterceptor).addPathPatterns("/public/api/v1/post", "/public/api/v1/post/**");
	}

}
