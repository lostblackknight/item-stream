package io.github.lostblackknight.item.stream;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjUtil;
import com.google.common.eventbus.EventBus;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 上下文
 *
 * @author chensixiang (chensixiang1234@gmail.com) 2023/3/24 23:03
 * @version 1.0.0
 * @see ItemStreamMapper
 * @see ItemStreamCollector
 * @see ItemStreamClient
 */
public class Context<I, O> {

    /**
     * 输入
     */
    private I input;

    /**
     * 输出
     */
    private O output;

    /**
     * mappers
     */
    private final Map<Class<?>, ItemStreamMapper<?, ?>> mappers = new ConcurrentHashMap<>(64);

    /**
     * beDependsOn
     */
    private final Map<Class<?>, Set<Class<?>>> beDependsOn = new ConcurrentHashMap<>(64);

    /**
     * collector
     */
    private ItemStreamCollector<?, ?> collector;

    /**
     * countDownLatch
     */
    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    /**
     * stopWatch
     */
    private final Queue<StopWatch> stopWatches = new LinkedBlockingQueue<>();

    /**
     * 事务事件总线
     */
    private final EventBus txEventBus;

    public Context() {
        txEventBus = new EventBus(IdUtil.fastSimpleUUID());
    }

    public Map<String, ItemStream> getItemStreams() {
        final HashMap<String, ItemStream> itemStreams = new HashMap<>();
        mappers.forEach((clazz, itemStreamMapper) -> {
            final String name = ClassUtil.getClassName(clazz, true);
            itemStreams.put(name, itemStreamMapper);
        });
        if (ObjUtil.isNotEmpty(collector)) {
            final String name = ClassUtil.getClassName(collector, true);
            itemStreams.put(name, collector);
        }
        return itemStreams;
    }

    public I getInput() {
        return input;
    }

    public void setInput(I input) {
        this.input = input;
    }

    public O getOutput() {
        return output;
    }

    public void setOutput(O output) {
        this.output = output;
    }

    public Map<Class<?>, ItemStreamMapper<?, ?>> getMappers() {
        return mappers;
    }

    public Map<Class<?>, Set<Class<?>>> getBeDependsOn() {
        return beDependsOn;
    }

    public ItemStreamCollector<?, ?> getCollector() {
        return collector;
    }

    public void setCollector(ItemStreamCollector<?, ?> collector) {
        this.collector = collector;
    }

    public CountDownLatch getCountDownLatch() {
        return countDownLatch;
    }

    public EventBus getTxEventBus() {
        return txEventBus;
    }

    public Queue<StopWatch> getStopWatches() {
        return stopWatches;
    }
}
