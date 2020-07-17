package com.javalow.utils;

import com.javalow.utils.idgenerate.BuildIdFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * @description: 测试类
 * @author: huweixing
 * @ClassName: TestUtil
 * @Date: 2020-07-17
 * @Time: 18:13
 */
@Slf4j
public class TestUtil {

    public static void main(String[] args) {
        Long ID = BuildIdFactory.getInstance().buildFactoryOrderId();
        log.info("========ID========>{}", ID);
    }

}
