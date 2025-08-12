package com.example.memo.jpa.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class PgDataSourceConfig {

    @Value("${pg.datasource.url}")
    private String url;

    @Value("${pg.datasource.username}")
    private String username;

    @Value("${pg.datasource.password}")
    private String password;

    @Value("${pg.datasource.driver-class-name}")
    private String driverClassName;
    
    //PostgreSQL용 DataSource(PostgreSQL 전용 연결 발급기)
    @Bean(name = "pgDataSource")
    public DataSource pgDataSource() {
        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName(driverClassName)
                .build();
    }
    //위 DataSource JDBCTemplate(DB 안내원)
    //JDBC->자바에서 DB랑 대화하는 표준 규격
    @Bean(name = "pgJdbcTemplate")
    public JdbcTemplate pgJdbcTemplate(@Qualifier("pgDataSource") DataSource pgDataSource) {
        return new JdbcTemplate(pgDataSource);
    }
    
    //DataSource->기차 승차권 판매소
    //JDBC-> 기차길 규격
    //JdbcTemplate->기차 안내원
}
