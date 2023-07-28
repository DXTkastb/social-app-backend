package com.configs;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

import dev.miku.r2dbc.mysql.MySqlConnectionConfiguration;
import dev.miku.r2dbc.mysql.MySqlConnectionFactory;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.Option;

@Configuration
@EnableR2dbcRepositories
public class DatabaseConfig {

    @Value("${sql.host}")
    String SQL_HOST;

    @Value("${sql.db}")
    String SQL_DB;

    @Value("${sql.username}")
    String SQL_USER;

    @Value("${sql.password}")
    String SQL_PASSWORD;

    @Value("${sql.portnumber}")
    int SQL_PORT;

    @Bean("sql-connection-factory")
    ConnectionFactory mySqlConnectionFactory() {
    	
    	ConnectionFactoryOptions options = ConnectionFactoryOptions.builder()
    		    .option(ConnectionFactoryOptions.DRIVER, "pool")
    		    .option(ConnectionFactoryOptions.PROTOCOL, "mysql")
    		    .option(ConnectionFactoryOptions.HOST, SQL_HOST)
    		    .option(ConnectionFactoryOptions.USER, SQL_USER)
    		    .option(ConnectionFactoryOptions.PORT, SQL_PORT) 
    		    .option(ConnectionFactoryOptions.PASSWORD, SQL_PASSWORD)
    		    .option(ConnectionFactoryOptions.DATABASE,SQL_DB) 
    		    .option(Option.valueOf("tcpKeepAlive"), true)
      		    .build();
    	
    		ConnectionFactory connectionFactory = ConnectionFactories.get(options);
    		return connectionFactory;

    }

    @Bean
    // R2dbcEntityTemplate r2dbcTemplate(@Qualifier("sql-connection-factory") MySqlConnectionFactory factory) {  
    R2dbcEntityTemplate r2dbcTemplate(@Qualifier("sql-connection-factory") ConnectionFactory factory) {  
        return new R2dbcEntityTemplate(factory);
    }

}
