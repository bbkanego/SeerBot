package com.seerlogics.chatbot.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
@EnableTransactionManagement
@EnableJpaRepositories(
        entityManagerFactoryRef = "botAdminEntityManagerFactory",
        basePackages = {"com.seerlogics.commons.repository"}
)
public class BotAdminDbConfig {

    @Value("${botadmin.datasource.hibernate.ddl-auto:update}")
    private String hibernateHbm2ddlValue;

    @Value("${botadmin.datasource.hibernate.jdbc.time_zone:UTC}")
    private String hibernateJDBCTimezone;

    @Value("${botadmin.datasource.hibernate.show_sql:false}")
    private Boolean hibernateShowSQL;

    @Bean(name = "botAdminDataSource")
    @ConfigurationProperties(prefix = "botadmin.datasource")
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }

    @Value("${botadmin.datasource.hibernate.naming.physical-strategy:com.seerlogics.commons.naming.CustomPhysicalNamingStrategy}")
    private String namingStrategy;

    @Bean(name = "botAdminEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean
    entityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("botAdminDataSource") DataSource dataSource
    ) {

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", this.hibernateHbm2ddlValue);
        properties.put("hibernate.jdbc.time_zone", this.hibernateJDBCTimezone);
        properties.put("hibernate.show_sql", this.hibernateShowSQL);
        properties.put("hibernate.physical_naming_strategy", this.namingStrategy);

        return builder
                .dataSource(dataSource)
                .packages("com.seerlogics.commons.model")
                .persistenceUnit("botAdmin")
                .properties(properties)
                .build();
    }

    @Bean(name = "botAdminTransactionManager")
    public PlatformTransactionManager transactionManager(
            @Qualifier("botAdminEntityManagerFactory") EntityManagerFactory
                    entityManagerFactory
    ) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
