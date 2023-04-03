package io.github.lostblackknight.item.stream.mapper;

import io.github.lostblackknight.item.stream.Context;
import io.github.lostblackknight.item.stream.InterruptedMode;
import io.github.lostblackknight.item.stream.Item;
import io.github.lostblackknight.item.stream.ItemStreamCollector;
import io.github.lostblackknight.item.stream.model.NameModel;
import lombok.extern.slf4j.Slf4j;

/**
 * @author chensixiang (chensixiang1234@gmail.com) 2023/3/26 1:26
 * @version 1.0.0
 */
@Slf4j
@Item(dependsOn = {BMapper.class}, interruptedMode = InterruptedMode.NONE)
public class DefaultCollector extends ItemStreamCollector<NameModel, NameModel> {

    @Override
    public void collect(NameModel output, Context<NameModel, NameModel> context) throws Exception {
        output.setNameAC(output.getNameAC() + "收集成功");
        // throw new IllegalArgumentException("测试");
    }

    @Override
    public void ex(Exception ex, Context<NameModel, NameModel> context) {
        log.error("DefaultCollector 异常了...{}", ex.getMessage(), ex);
    }

    @Override
    public void rollback(Context<NameModel, NameModel> context) {
        log.warn("DefaultCollector 回滚了...");
    }
}
