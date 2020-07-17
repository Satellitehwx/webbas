package com.javalow.utils.idgenerate;

import java.io.IOException;


/**
 * @ClassName: BuildIdFactory
 * @Description: 全局ID生产工厂
 * @anthor huweixing
 * @date 2020-05-09-17:55
 */
public final class BuildIdFactory {

    /**
     * 序列
     */
    private final static String TAB_ORDER = "";

    private static volatile RedisIdGenerator idGenerator;
    private static volatile BuildIdFactory instance;

    private BuildIdFactory() {
    }

    public static BuildIdFactory getInstance() {
        if (idGenerator == null) {
            synchronized (RedisIdGenerator.LoadIdGeneratorConfig.class) {
                try {
                    idGenerator = RedisIdGenerator.LoadIdGeneratorConfig.loadConfig.buildIdGenerator();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (instance == null) {
            synchronized (BuildIdFactory.class) {
                instance = new BuildIdFactory();
            }
        }
        return instance;
    }

    public Long buildFactoryOrderId() {
        return idGenerator.next(TAB_ORDER);
    }
}
