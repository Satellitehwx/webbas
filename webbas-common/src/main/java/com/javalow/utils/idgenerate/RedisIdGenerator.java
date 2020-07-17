package com.javalow.utils.idgenerate;

import cn.hutool.core.date.DateUtil;
import com.javalow.exception.BaseException;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author huweixing
 * RedisIdGenerator
 * 基于redis ID生成类
 * @date 2020-05-09-17:55
 **/
@Component
@AllArgsConstructor
public class RedisIdGenerator {

    static final Logger logger = LoggerFactory.getLogger(RedisIdGenerator.class);

    private static String host;
    private static String password;
    private static String port;


    public static String getPassword() {
        return password;
    }

    @Value("${spring.redis.password}")
    public void setPassword(String password) {
        RedisIdGenerator.password = password;
    }


    public static String getPort() {
        return port;
    }

    @Value("${spring.redis.port}")
    public void setPort(String port) {
        RedisIdGenerator.port = port;
    }

    public static String getHost() {
        return host;
    }

    @Value("${spring.redis.host}")
    public void setHost(String host) {
        RedisIdGenerator.host = host;
    }


    List<Pair<JedisPool, String>> jedisPoolList;
    int retryTimes;
    int index = 0;

    private RedisIdGenerator() {
    }

    private RedisIdGenerator(List<Pair<JedisPool, String>> jedisPoolList, int retryTimes) {
        this.jedisPoolList = jedisPoolList;
        this.retryTimes = retryTimes;
    }

    static public IdGeneratorBuilder builder() {
        return new IdGeneratorBuilder();
    }

    static class IdGeneratorBuilder {

        List<Pair<JedisPool, String>> jedisPoolList = new ArrayList<>();
        int retryTimes = 5;

        public IdGeneratorBuilder addHost(String host, int port, String pass, String luaSha) {
            JedisPoolConfig config = new JedisPoolConfig();
            //最大空闲连接数, 应用自己评估，不要超过ApsaraDB for Redis每个实例最大的连接数
            config.setMaxIdle(200);
            //最大连接数, 应用自己评估，不要超过ApsaraDB for Redis每个实例最大的连接数
            config.setMaxTotal(300);
            config.setTestOnBorrow(false);
            config.setTestOnReturn(false);
            config.setLifo(true);
            config.setMinIdle(30);
            jedisPoolList.add(Pair.of(StringUtils.isEmpty(pass) ? new JedisPool(config, host, port, 1000) : new JedisPool(config, host, port, 1000, pass), luaSha));
            return this;
        }

        public IdGeneratorBuilder retryTimes(int retryTimes) {
            this.retryTimes = retryTimes;
            return this;
        }

        public RedisIdGenerator build() {
            return new RedisIdGenerator(jedisPoolList, retryTimes);
        }
    }

    public long next(String tab) {
        for (int i = 0; i < retryTimes; ++i) {
            Long id = innerNext(tab);
            if (id != null) {
                return id;
            }
        }
        throw new RuntimeException("Can not generate id!");
    }

    private Long innerNext(String tab) {
        /*Calendar cal = Calendar.getInstance();
        //获取年份后两位
        String year = String.valueOf(cal.get(Calendar.YEAR)).substring(2);
        //获取今天在今年是第多少天
        String day = String.valueOf(cal.get(Calendar.DAY_OF_YEAR));*/
        String nowStr = DateUtil.now();
        String year = nowStr.substring(2, 4) + nowStr.substring(5, 7);
        System.out.println("==========year=========" + year);
        String day = nowStr.substring(8, 10);
        System.out.println("==========day=========" + day);
        if (index == jedisPoolList.size()) {
            index = 0;
        }
        Pair<JedisPool, String> pair = jedisPoolList.get(index++ % jedisPoolList.size());
        JedisPool jedisPool = pair.getLeft();
        String luaSha = pair.getRight();
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            return Long.valueOf(jedis.evalsha(luaSha, 3, tab, year, day).toString());
        } catch (JedisException e) {
            logger.error("generate id error!", e);
            return null;
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    /**
     * 加载服务端redis配置文件
     */
    static class LoadIdGeneratorConfig {
        static List<RedisScriptConfig> scriptConf = new ArrayList<>();
        static LoadIdGeneratorConfig loadConfig = new LoadIdGeneratorConfig();

        static {
            /*Properties pro = new Properties();
            try {
                //读取redis连接配置文件
                pro.load(LoadIdGeneratorConfig.class.getResourceAsStream("/cluster_redis_config.properties"));
                for (int i = 1; i <= 3; i++) {
                    String host = pro.getProperty("redis_cluster" + i + "_host");
                    String pass = pro.getProperty("redis_cluster" + i + "_pass");
                    if (StringUtils.isEmpty(host)) {
                        continue;
                    }
                    scriptConf.add(new RedisScriptConfig(host, Integer.valueOf(pro.getProperty("redis_cluster" + i + "_port", "6379")), pass));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }*/

            //设置ip和端口
            /*String[] passArray = password.split(",");*/
            String[] hostArray = host.split(",");
            String[] portArray = port.split(",");
            String[] passArray = password.split(",");
            if (hostArray.length != portArray.length) {
                throw new BaseException(new Throwable("REDIS CONFIG ERROR"));
            }
            for (int i = 0; i <= hostArray.length - 1; i++) {
                String ip = hostArray[i];
                String port = portArray[i];
                String pass = passArray[i];
                scriptConf.add(new RedisScriptConfig(ip.trim(), (Integer.parseInt(port) == 0 ? 6379 : Integer.parseInt(port)), pass));
            }
        }

        public RedisIdGenerator buildIdGenerator()
                throws IOException {
            loadConfig.loadScript();
            IdGeneratorBuilder idGenerator = RedisIdGenerator.builder();
            for (RedisScriptConfig conf : scriptConf) {
                idGenerator = idGenerator.addHost(conf.getHost(), conf.getPort(), conf.getPass(), conf.getScriptSha());
            }
            return idGenerator.build();
        }

        /**
         * 加载服生成ID脚本文件
         *
         * @throws IOException
         */
        public void loadScript()
                throws IOException {
            int index = 1;
            for (RedisScriptConfig conf : scriptConf) {
                Jedis jedis = new Jedis(conf.getHost(), conf.getPort());
                if (!StringUtils.isEmpty(conf.getPass())) {
                    jedis.auth(conf.getPass());
                }
                InputStream is = LoadIdGeneratorConfig.class.getResourceAsStream("/script/redis-script-node" + index++ + ".lua");
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String readLine = null;
                StringBuilder sb = new StringBuilder();
                while ((readLine = br.readLine()) != null) {
                    sb.append(readLine);
                }
                br.close();
                is.close();
                conf.setScriptSha(jedis.scriptLoad(sb.toString()));
                jedis.close();
            }
        }
    }

    static class RedisScriptConfig {

        private String host;
        private Integer port;
        private String pass;
        private String scriptSha;

        public RedisScriptConfig(String host, Integer port, String pass) {
            super();
            this.host = host;
            this.port = port;
            this.pass = pass;
        }

        public void setScriptSha(String scriptSha) {
            this.scriptSha = scriptSha;
        }

        public String getHost() {
            return host;
        }

        public Integer getPort() {
            return port;
        }

        public String getPass() {
            return pass;
        }

        public String getScriptSha() {
            return scriptSha;
        }
    }

}
