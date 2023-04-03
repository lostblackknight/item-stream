package io.github.lostblackknight.item.stream.mapper;

import io.github.lostblackknight.item.stream.Context;
import io.github.lostblackknight.item.stream.Item;
import io.github.lostblackknight.item.stream.ItemStreamMapper;
import io.github.lostblackknight.item.stream.model.NameModel;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @author chensixiang (chensixiang1234@gmail.com) 2023/3/27 22:26
 * @version 1.0.0
 */
@Item
@Slf4j
public class DMapper extends ItemStreamMapper<NameModel, NameModel> {

    @Override
    public void map(NameModel input, NameModel output, Context<NameModel, NameModel> context) throws Exception {
        output.setNameD("D");
    }

    @Override
    public void ex(Exception ex, Context<NameModel, NameModel> context) {
        log.error("DMapper 异常了...{}", ex.getMessage(), ex);
    }

    @Override
    public void rollback(Context<NameModel, NameModel> context) {
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.warn("DMapper 回滚了....");
    }
}
