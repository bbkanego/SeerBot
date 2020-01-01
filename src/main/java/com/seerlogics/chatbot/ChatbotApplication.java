package com.seerlogics.chatbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Arrays;

/**
 * READ THESE LINKS to understant @SpringBootApplication:
 * https://docs.spring.io/spring-boot/docs/current/reference/html/using-boot-using-springbootapplication-annotation.html#using-boot-using-springbootapplication-annotation
 * https://springbootdev.com/2017/11/13/what-are-the-uses-of-entityscan-and-enablejparepositories-annotations/
 * <p>
 * Since ALL the JPA repository and Entities and controllers are under 'com.seerlogics.botadmin',
 * the "@SpringBootApplication's" auto configuration is able to do component scans
 */
@SpringBootApplication // same as adding @Configuration @EnableAutoConfiguration @ComponentScan
@EnableAutoConfiguration(exclude = {
        CassandraDataAutoConfiguration.class,
        MongoDataAutoConfiguration.class,
        MongoRepositoriesAutoConfiguration.class,
        MongoAutoConfiguration.class,
        JmsAutoConfiguration.class,
        ActiveMQAutoConfiguration.class,
        SecurityAutoConfiguration.class,
        ManagementWebSecurityAutoConfiguration.class})
@EnableTransactionManagement
// ApplicationRunner: https://memorynotfound.com/spring-boot-passing-command-line-arguments-example/
public class ChatbotApplication extends SpringBootServletInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatbotApplication.class);
    
    public static void main(String[] args) {
        SpringApplication.run(ChatbotApplication.class, args);
    }

    // @Override
    public void run(ApplicationArguments args) throws Exception {
        LOGGER.info("Application started with command-line arguments: {}", Arrays.toString(args.getSourceArgs()));
        LOGGER.info("NonOptionArgs: {}", args.getNonOptionArgs());
        LOGGER.info("OptionNames: {}", args.getOptionNames());

        for (String name : args.getOptionNames()){
            LOGGER.info("arg-" + name + "=" + args.getOptionValues(name));
        }

        boolean containsOption = args.containsOption("seerchat.bottype");
        LOGGER.info("Contains seerchat.bottype: " + containsOption);
    }
}
