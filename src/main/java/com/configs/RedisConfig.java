package com.configs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

import com.redis.lettucemod.api.StatefulRedisModulesConnection;
import com.redis.lettucemod.api.reactive.RedisModulesReactiveCommands;

@Configuration
public class RedisConfig {
	
    @Autowired
    StatefulRedisModulesConnection<String, String> connection;

    
    @Bean
    public RedisModulesReactiveCommands<String,String> redisSearch() {
    	return connection.reactive();
    }
    
	@Primary
	@Bean(value = "redis-connection-factory")
	public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
	    return new LettuceConnectionFactory("localhost",6379);
	}
	
	@Bean(value = "redis-template")
	public ReactiveStringRedisTemplate redisTemplate(
			@Qualifier(value = "redis-connection-factory")
			ReactiveRedisConnectionFactory connectionFactory) {
		return new ReactiveStringRedisTemplate(connectionFactory);
	}
	
}
