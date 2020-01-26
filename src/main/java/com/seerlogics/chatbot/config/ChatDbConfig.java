package com.seerlogics.chatbot.config;

import com.seerlogics.commons.config.HibernateConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by bkane on 3/11/19.
 * https://medium.com/@joeclever/using-multiple-datasources-with-spring-boot-and-spring-data-6430b00c02e7
 */
@Configuration
@ComponentScan(basePackages = {"com.seerlogics.commons.config"})
@EnableTransactionManagement
@EnableJpaRepositories(
        entityManagerFactoryRef = "chatBotEntityManagerFactory",
        basePackages = {"com.seerlogics.chatbot.repository"}
)
public class ChatDbConfig {

    @Autowired
    private HibernateConfig hibernateConfig;

    @Primary
    @Bean(name = "chatBotDataSource")
    @ConfigurationProperties(prefix = "chatbot.datasource")
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }

    @Primary
    @Bean(name = "chatBotEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean
    entityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("chatBotDataSource") DataSource dataSource
    ) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", this.hibernateConfig.getChatBotHibernateHbm2ddlValue());
        properties.put("hibernate.jdbc.time_zone", this.hibernateConfig.getChatBotHibernateJDBCTimezone());
        properties.put("hibernate.show_sql", this.hibernateConfig.getChatBotHibernateShowSQL());
        properties.put("hibernate.physical_naming_strategy", this.hibernateConfig.getChatBotNamingStrategy());
        properties.put("hibernate.dialect", this.hibernateConfig.getChatBotHibernateDialect());

        return builder
                .dataSource(dataSource)
                .packages("com.seerlogics.chatbot")
                .persistenceUnit("chatbot")
                .properties(properties)
                .build();
    }

    @Primary
    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(
            @Qualifier("chatBotEntityManagerFactory") EntityManagerFactory
                    entityManagerFactory
    ) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
