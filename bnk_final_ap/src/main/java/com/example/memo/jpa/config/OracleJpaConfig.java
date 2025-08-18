package com.example.memo.jpa.config;

import java.util.Properties;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;

@Configuration
@EnableTransactionManagement //Transactional 트랜잭션 관리 기능
@EnableJpaRepositories(
    basePackages = "com.example.memo",          // Oracle용 JPA repository 패키지
    entityManagerFactoryRef = "oracleEmf",
    transactionManagerRef = "txManagerOracle"
)
//다중 DB환경에서 적용
public class OracleJpaConfig {
	//Oracle DB를 JPA로 사용할 수 있도록 수동 설정
	
	//Oracle DB에 접속하기 위한 JDBC 설정->JPA에서만 사용되도록 지정
	//Oracle 연결 설정
    @Bean(name = "oracleDs")
    public DataSource oracleDs(@Value("${spring.datasource.url}") String url,
                               @Value("${spring.datasource.username}") String username,
                               @Value("${spring.datasource.password}") String password,
                               @Value("${spring.datasource.driver-class-name}") String driverClassName) {
        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName(driverClassName)
                .build();
    }

    //Oracle DB를 위한 JPA 핵심 객체 EntityManagerFactory
    //이 메서드가 내가 만든 Bean->엔티티매니저팩토리(스프링이 여기서 가져감)->엔티티매니저(JPA 리포지토리가 사용)->SQL실행(Hibernate->JDBC->Oracle)
    //Hibernate는 자바에서 SQL없이 DB다루는 프레임워크
    @Bean(name = "oracleEmf")
    public LocalContainerEntityManagerFactoryBean oracleEmf(@Qualifier("oracleDs") DataSource oracleDs) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(oracleDs);
        //어떤 Entity 인식할지 지정
        emf.setPackagesToScan("com.example.memo"); 

        //Hibernate 사용할 수 있도록
        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        emf.setJpaVendorAdapter(vendorAdapter);

        Properties properties = new Properties();
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.OracleDialect"); // ✅ Oracle용 Dialect
        properties.setProperty("hibernate.hbm2ddl.auto", "update");                            // schema 자동 갱신
        emf.setJpaProperties(properties);

        return emf;
    }

    //Oracle JPA 트랜잭션 관리
    @Bean(name = "txManagerOracle")
    public PlatformTransactionManager txManagerOracle(
            @Qualifier("oracleEmf") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
    
    // 예외 번역기 (선택)
    @Bean
    public PersistenceExceptionTranslationPostProcessor exceptionTranslation() {
        return new PersistenceExceptionTranslationPostProcessor();
    }
}
