package com.javalow.config.redis;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @description: redis集群配置
 * @author: huweixing
 * @ClassName: ClusterRedisConfig
 * @Date: 2020-07-17
 * @Time: 10:30
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "spring.redis.cluster")
@ConditionalOnProperty(name = {"spring.redis.type"}, havingValue = "cluster")
public class ClusterRedisConfig {

    /**
     * 集群ip和端口节点
     */
    private String nodes;

    /**
     * 集群密码
     */
    private String password;

    /**
     * 集群最大转发的数量
     */
    private Integer maxRedirects;

}
