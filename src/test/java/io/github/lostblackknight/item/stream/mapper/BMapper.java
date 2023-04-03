package io.github.lostblackknight.item.stream.mapper;

import io.github.lostblackknight.item.stream.Context;
import io.github.lostblackknight.item.stream.Item;
import io.github.lostblackknight.item.stream.ItemStreamMapper;
import io.github.lostblackknight.item.stream.model.NameModel;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @author chensixiang (chensixiang1234@gmail.com) 2023/3/24 14:55
 * @version 1.1.0
 */
@Slf4j
@Item(dependsOn = {AMapper.class, CMapper.class, DMapper.class})
public class BMapper extends ItemStreamMapper<NameModel, NameModel> {

    @Override
    public void map(NameModel input, NameModel output, Context<NameModel, NameModel> context) throws Exception {
        // log.info("开始处理 BMapper");
        // output.setNameAC(output.getNameA() + ":" + output.getNameC());
        // log.info("BMapper 处理完成，结果为：{}", output);
        throw new IllegalArgumentException("测试");
    }

    @Override
    public void ex(Exception ex, Context<NameModel, NameModel> context) {
        log.error("BMapper 异常了...{}", ex.getMessage(), ex);
    }

    @Override
    public void rollback(Context<NameModel, NameModel> context) {
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.warn("BMapper 回滚了...");
    }
}
