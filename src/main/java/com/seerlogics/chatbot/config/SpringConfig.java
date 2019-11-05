package com.seerlogics.chatbot.config;

import com.lingoace.exception.config.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Properties;

/**
 * Created by bkane on 5/6/18.
 * https://www.thomasvitale.com/spring-data-jpa-hibernate-java-configuration/
 * https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/servlet/config/annotation/EnableWebMvc.html
 */
@Configuration
@ComponentScan(basePackages = "com.seerlogics")
@EnableWebMvc
public class SpringConfig implements WebMvcConfigurer {

    private final StartUpConfiguration startUpConfiguration;

    public SpringConfig(StartUpConfiguration startUpConfiguration) {
        this.startUpConfiguration = startUpConfiguration;
    }

    @Bean
    public PersistenceExceptionTranslationPostProcessor exceptionTranslation() {
        return new PersistenceExceptionTranslationPostProcessor();
    }

    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("bundle/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setCacheMillis(10);
        return messageSource;
    }

    private ClientHttpRequestFactory createRequestFactory() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(20);
        connectionManager.setDefaultMaxPerRoute(20);

        RequestConfig config = RequestConfig.custom().setConnectTimeout(100000).build();
        CloseableHttpClient httpClient = HttpClientBuilder.create().setConnectionManager(connectionManager)
                .setDefaultRequestConfig(config).build();
        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }

    @Bean
    public VelocityEngine velocityEngine() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("input.encoding", "UTF-8");
        properties.setProperty("output.encoding", "UTF-8");
        properties.setProperty("resource.loader", "class");
        properties.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        return new VelocityEngine(properties);
    }

    /**
     * https://spring.io/blog/2015/06/08/cors-support-in-spring-framework#javaconfig
     * The below will configure ALL the headers and info that the pre-flight call or OPTIONS call will return.
     *
     * @param registry
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String allowedOriginsStr = startUpConfiguration.getAllowedOrigins();
        if (allowedOriginsStr == null) {
            throw new ConfigurationException("AllowedOrigins missing. Please provide allowed " +
                    "origins as a spring arg as follows: --seerchat.allowedOrigins=http://localhost:4300,http://localhost:4320");
        }

        /**
         * This will be triggered for ALL URLs.
         */
        registry.addMapping("/**")
                /**
                 * This will return allowed origins in "Access-Control-Allow-Origin"
                 */
                .allowedOrigins(StringUtils.split(allowedOriginsStr, ","))
                /**
                 * This will expose header "Access-Control-Allow-Credentials" telling browser that the server
                 * is ready to accept cookies. The UI then will set "withCredentials=true" in JS to send
                 * cookie with request
                 * https://stackoverflow.com/questions/24687313/what-exactly-does-the-access-control-allow-credentials-header-do
                 */
                .allowCredentials(true)
                /**
                 * This will return allowed methods in "Access-Control-Allow-Methods"
                 */
                .allowedMethods("PUT", "DELETE", "POST", "GET");
    }

    @Bean
    public RestTemplate createRestTemplate() {
        return new RestTemplate(createRequestFactory());
    }
}
