package com.javalow.config.redis;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @description: 单机redis配置类
 * @author: huweixing
 * @ClassName: StandaloneRedisConfig
 * @Date: 2020-07-17
 * @Time: 10:27
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "spring.redis")
@ConditionalOnProperty(name = {"spring.redis.type"}, havingValue = "single")
public class StandaloneRedisConfig {

    /**
     * redis ip地址
     */
    private String host;

    /**
     * redis端口
     */
    private Integer port;

    /**
     * redis密码
     */
    private String password;

}
