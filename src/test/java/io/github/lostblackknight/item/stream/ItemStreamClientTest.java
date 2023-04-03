package io.github.lostblackknight.item.stream;

import cn.hutool.core.util.RandomUtil;
import io.github.lostblackknight.item.stream.mapper.DefaultCollector;
import io.github.lostblackknight.item.stream.model.NameModel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.parallel.Execution;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

/**
 * @author chensixiang (chensixiang1234@gmail.com) 2023/3/25 22:31
 * @version 1.1.0
 */
@Slf4j
public class ItemStreamClientTest {

    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    private final ItemStreamClient<NameModel, NameModel> executorClient = ItemStreamClient.create(() -> {
        final GlobalSetup globalSetup = new GlobalSetup();
        globalSetup.setPrettyPrint(true);
        globalSetup.setExecutor(executor);
        globalSetup.setTx(true);
        return globalSetup;
    });

    @RepeatedTest(1)
    @Execution(CONCURRENT)
    public void testExecutor() throws InterruptedException {
        try {
            executorClient
                    .init()
                    .input(new NameModel(RandomUtil.randomString(3), RandomUtil.randomString(3), RandomUtil.randomString(3), null))
                    .output(NameModel::new)
                    .mapperClass("io.github.lostblackknight.item.stream.mapper")
                    .collectorClass(DefaultCollector.class);
            final NameModel nameModel = executorClient.run();
            log.info("获取到结果为: {}", nameModel);
            TimeUnit.SECONDS.sleep(10);
        } finally {
            executorClient.clear();
        }
    }
}
