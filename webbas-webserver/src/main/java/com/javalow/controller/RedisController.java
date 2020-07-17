package com.javalow.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @description: redis测试接口控制器
 * @author: huweixing
 * @ClassName: RedisController
 * @Date: 2020-07-17
 * @Time: 10:12
 */
@RestController
@RequestMapping("/api/redis")
public class RedisController {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @GetMapping("/set")
    public String setValue(String value) {
        stringRedisTemplate.opsForValue().set(value, value);
        return "成功";
    }

    @GetMapping("/get")
    public Object getValue(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

}
