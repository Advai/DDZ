package com.yourco.ddz.server.config;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/** Configuration to support React Router by forwarding all non-API routes to index.html */
@Configuration
public class SpaRoutingConfig implements WebMvcConfigurer {

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // Serve static resources (JS, CSS, images) from /web directory
    registry
        .addResourceHandler("/assets/**")
        .addResourceLocations("file:./web/assets/", "classpath:/static/assets/");

    // Forward all other requests to index.html for React Router to handle
    registry
        .addResourceHandler("/**")
        .addResourceLocations("file:./web/", "classpath:/static/")
        .resourceChain(true)
        .addResolver(
            new PathResourceResolver() {
              @Override
              protected Resource getResource(String resourcePath, Resource location)
                  throws IOException {
                Resource requestedResource = location.createRelative(resourcePath);

                // If the requested resource exists, serve it
                if (requestedResource.exists() && requestedResource.isReadable()) {
                  return requestedResource;
                }

                // Otherwise, return index.html for React Router to handle
                // This allows client-side routing to work
                Resource indexHtml = new ClassPathResource("/static/index.html");
                if (!indexHtml.exists()) {
                  // Fallback to file system
                  indexHtml = location.createRelative("index.html");
                }
                return indexHtml;
              }
            });
  }
}
