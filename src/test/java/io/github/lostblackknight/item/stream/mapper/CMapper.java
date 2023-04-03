package io.github.lostblackknight.item.stream.mapper;

import cn.hutool.core.util.RandomUtil;
import io.github.lostblackknight.item.stream.Context;
import io.github.lostblackknight.item.stream.Item;
import io.github.lostblackknight.item.stream.ItemStreamMapper;
import io.github.lostblackknight.item.stream.model.NameModel;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @author chensixiang (chensixiang1234@gmail.com) 2023/3/24 14:56
 * @version 1.1.0
 */
@Slf4j
@Item
public class CMapper extends ItemStreamMapper<NameModel, NameModel> {

    @Override
    public void map(NameModel input, NameModel output, Context<NameModel, NameModel> context) throws Exception {
        // log.info("开始处理 CMapper");
        TimeUnit.SECONDS.sleep(2);
        output.setNameC(input.getNameC() + "-" + RandomUtil.randomString(5));
        // log.info("CMapper 处理完成，结果为：{}", output);
        // throw new IllegalArgumentException("测试");
    }

    @Override
    public void ex(Exception ex, Context<NameModel, NameModel> context) {
        log.error("AMapper 异常了...{}", ex.getMessage(), ex);
    }

    @Override
    public void rollback(Context<NameModel, NameModel> context) {
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.warn("CMapper 回滚了...");
    }
}
