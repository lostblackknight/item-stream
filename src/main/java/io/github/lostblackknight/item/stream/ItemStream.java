package io.github.lostblackknight.item.stream;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjUtil;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static io.github.lostblackknight.item.stream.State.*;

/**
 * 项目流
 *
 * @author chensixiang (chensixiang1234@gmail.com) 2023/3/23 10:58
 * @version 1.0.0
 */
public abstract class ItemStream {

    protected static final Logger log = LoggerFactory.getLogger(ItemStream.class);

    /**
     * 项目流的名称
     */
    private final String name;

    /**
     * 项目流的状态
     */
    private State state;

    /**
     * 依赖的其他项目流的状态
     */
    private final Map<Class<? extends ItemStream>, State> dependsOnState = new HashMap<>();

    /**
     * 被哪些项目流所依赖
     */
    private final List<Class<? extends ItemStream>> beDependsOn = new ArrayList<>();

    /**
     * 事件总线
     */
    private EventBus eventBus;

    /**
     * 是否为单独的项目流
     */
    private boolean single;

    /**
     * 锁
     */
    private final Lock lock = new ReentrantLock();

    /**
     * 打断的原因
     */
    private final Deque<Exception> interruptCause = new LinkedList<>();

    /**
     * 状态跟踪
     */
    private final Deque<State> stateTrace = new LinkedList<>();

    /**
     * 线程池
     */
    private Executor executor;

    /**
     * 运行模式
     */
    private RunnableMode runnableMode;

    /**
     * 运行模式必须依赖的项目流
     */
    private List<Class<? extends ItemStream>> runnableMust;

    /**
     * 打断模式
     */
    private InterruptedMode interruptedMode;

    /**
     * 打断模式必须依赖的项目流
     */
    private List<Class<? extends ItemStream>> interruptedMust;

    /**
     * stopWatch
     */
    private StopWatch stopWatch;

    /**
     * 是否打印 stopWatch
     */
    private boolean prettyPrint;

    /**
     * 是否开启事务
     */
    private boolean tx;

    public ItemStream() {
        name = ClassUtil.getClassName(this, true);
        setState(NEW);
    }

    /**
     * 添加依赖的其他项目流
     *
     * @param itemStream 项目流
     */
    public final void putDependsOn(Class<? extends ItemStream> itemStream) {
        dependsOnState.put(itemStream, NEW);
        single = false;
    }

    /**
     * 改变依赖的其他项目流的状态
     *
     * @param itemStream 项目流
     * @param state      状态
     */
    private void changeDependsOnState(Class<? extends ItemStream> itemStream, State state) {
        dependsOnState.replace(itemStream, state);
    }

    /**
     * 获取依赖的其他项目流的状态
     *
     * @param itemStream 项目流
     * @return 状态
     */
    public final State getDependsOnState(Class<? extends ItemStream> itemStream) {
        return dependsOnState.get(itemStream);
    }

    /**
     * 注册被哪些项目流依赖
     *
     * @param itemStream 项目流
     */
    public final void register(ItemStream itemStream) {
        eventBus.register(itemStream);
        beDependsOn.add(itemStream.getClass());
    }

    /**
     * 注册被哪些项目流依赖
     *
     * @param itemStreams 项目流
     */
    public final void register(List<ItemStream> itemStreams) {
        itemStreams.forEach(eventBus::register);
        beDependsOn.addAll(itemStreams.stream().map(ItemStream::getClass).collect(Collectors.toList()));
    }

    /**
     * 设置项目流的状态
     *
     * @param state 状态
     */
    public final void setState(State state) {
        this.state = state;
        stateTrace.offerLast(this.state);
        switch (this.state) {
            case NEW:
                doNew();
                break;
            case RUNNABLE:
                if (ObjUtil.isEmpty(executor)) {
                    throw new ItemStreamException("Executor must not empty.");
                } else {
                    CompletableFuture.runAsync(this::doRunnable, executor);
                }
                break;
            case WAITING:
                doWaiting();
                break;
            case TERMINATED:
                doTerminated();
                break;
            case INTERRUPTED:
                doInterrupt();
                break;
            case ROLLBACK:
                if (tx) {
                    if (ObjUtil.isEmpty(executor)) {
                        throw new ItemStreamException("Executor must not empty.");
                    } else {
                        CompletableFuture.runAsync(this::doRollback, executor);
                    }
                }
                break;
            default:
                break;
        }
    }

    /**
     * 创建
     */
    protected void doNew() {
        single = true;
        eventBus = new EventBus(name + "-" + IdUtil.fastSimpleUUID());
        stopWatch = new StopWatch(name);
        prettyPrint = false;
        runnableMode = RunnableMode.ALL;
        interruptedMode = InterruptedMode.ANY;
        tx = false;
    }

    /**
     * 运行
     */
    protected void doRunnable() {
        try {
            stopWatch.start(name + "-" + RUNNABLE);
            doRunnableInterval();
            setState(TERMINATED);
        } catch (Exception e) {
            interruptCause.offerLast(e);
            setState(INTERRUPTED);
        } finally {
            doRunnableFinally();
        }
    }

    /**
     * 运行
     */
    protected void doRunnableInterval() throws Exception {
    }

    /**
     * 运行最终执行
     */
    protected void doRunnableFinally() {
    }

    /**
     * 等待
     */
    protected void doWaiting() {
    }

    /**
     * 终止
     */
    protected void doTerminated() {
        if (stopWatch.isRunning()) {
            stopWatch.stop();
        } else {
            stopWatch.start(name + "-" + TERMINATED);
            stopWatch.stop();
        }
        if (prettyPrint) {
            log.info(stopWatch.shortSummary(TimeUnit.MILLISECONDS));
        }
        final ItemStreamEvent itemEvent = new ItemStreamEvent(name, this.getClass(), TERMINATED, null);
        eventBus.post(itemEvent);
    }

    /**
     * 打断
     */
    protected void doInterrupt() {
        if (stopWatch.isRunning()) {
            stopWatch.stop();
        } else {
            stopWatch.start(name + "-" + INTERRUPTED);
            stopWatch.stop();
        }
        if (prettyPrint) {
            log.info(stopWatch.shortSummary(TimeUnit.MILLISECONDS));
        }
        final ItemStreamEvent itemEvent = new ItemStreamEvent(name, this.getClass(), INTERRUPTED, interruptCause.peekLast());
        eventBus.post(itemEvent);
    }

    /**
     * 回滚
     */
    protected void doRollback() {
    }

    /**
     * 订阅事件
     *
     * @param event 事件
     */
    @Subscribe
    private void subscribe(ItemStreamEvent event) {
        lock.lock();
        try {
            State oldState = state;
            log.debug("[{}]: [old state] = {}", event.getName() + "->" + name, oldState);
            log.debug("[{}]: [old dependsOnState] = {}", event.getName() + "->" + name, dependsOnState);
            if (tx && ROLLBACK.equals(state)) {
                return;
            }
            if (tx && ROLLBACK.equals(event.getState())) {
                setState(ROLLBACK);
                log.debug("[{}]: [new state] = [{} => {}]", event.getName() + "->" + name, oldState, ROLLBACK);
                return;
            }
            if (INTERRUPTED.equals(state) || TERMINATED.equals(state) || RUNNABLE.equals(state)) {
                return;
            }
            changeDependsOnState(event.getItemStream(), event.getState());
            log.debug("[{}]: [new dependsOnState] = {}", event.getName() + "->" + name, dependsOnState);
            int terminatedCount = 0;
            int interruptedCount = 0;
            for (Map.Entry<Class<? extends ItemStream>, State> entry : dependsOnState.entrySet()) {
                Class<? extends ItemStream> itemStream = entry.getKey();
                State itemState = entry.getValue();
                if (TERMINATED.equals(itemState)) {
                    if (RunnableMode.ALL.equals(runnableMode) || RunnableMode.ANY.equals(runnableMode)) {
                        terminatedCount++;
                    } else if (RunnableMode.MUST.equals(runnableMode)) {
                        if (runnableMust.contains(itemStream)) {
                            terminatedCount++;
                        }
                    }
                } else if (INTERRUPTED.equals(itemState)) {
                    if (InterruptedMode.ALL.equals(interruptedMode) || InterruptedMode.ANY.equals(interruptedMode)) {
                        interruptedCount++;
                    } else if (InterruptedMode.MUST.equals(interruptedMode)) {
                        if (interruptedMust.contains(itemStream)) {
                            interruptedCount++;
                        } else {
                            if (interruptedMustIsTerminated()) {
                                terminatedCount++;
                            }
                        }
                    } else if (InterruptedMode.NONE.equals(interruptedMode)) {
                        terminatedCount++;
                    }
                }
            }
            log.debug("[{}]: [interruptedCount] = {}, [terminatedCount] = {}", event.getName() + "->" + name, interruptedCount, terminatedCount);
            final State nextState = getNextStateByInterruptedMode(interruptedCount, terminatedCount, event);
            log.debug("[{}]: [new state] = [{} => {}]", event.getName() + "->" + name, oldState, nextState);
            if (!state.equals(nextState)) {
                setState(nextState);
            }
        } finally {
            lock.unlock();
        }
    }

    private boolean interruptedMustIsTerminated() {
        return interruptedMust.stream()
                .map(dependsOnState::get)
                .filter(state -> state.equals(TERMINATED))
                .count() == interruptedMust.size();
    }

    private State getNextStateByInterruptedMode(int interruptedCount, int terminatedCount, ItemStreamEvent event) {
        log.debug("[{}]: [interruptedMode] = {}", event.getName() + "->" + name, interruptedMode);
        if (InterruptedMode.ALL.equals(interruptedMode)) {
            if (interruptedCount == dependsOnState.size()) {
                if (ObjUtil.isNotEmpty(event.getEx())) {
                    handleInterruptCause(event.getName(), event.getEx());
                }
                return INTERRUPTED;
            } else {
                return getNextStateByRunnableMode(interruptedCount, terminatedCount, event);
            }
        } else if (InterruptedMode.ANY.equals(interruptedMode)) {
            if (interruptedCount > 0) {
                if (ObjUtil.isNotEmpty(event.getEx())) {
                    handleInterruptCause(event.getName(), event.getEx());
                }
                return INTERRUPTED;
            } else {
                return getNextStateByRunnableMode(interruptedCount, terminatedCount, event);
            }
        } else if (InterruptedMode.MUST.equals(interruptedMode)) {
            if (interruptedCount == interruptedMust.size()) {
                if (ObjUtil.isNotEmpty(event.getEx())) {
                    handleInterruptCause(event.getName(), event.getEx());
                }
                return INTERRUPTED;
            } else {
                return getNextStateByRunnableMode(interruptedCount, terminatedCount, event);
            }
        } else if (InterruptedMode.NONE.equals(interruptedMode)) {
            return getNextStateByRunnableMode(interruptedCount, terminatedCount, event);
        }
        return null;
    }

    private void handleInterruptCause(String name, Exception ex) {
        interruptCause.offerLast(new ItemStreamException("Exception from " + name + "; " + ex.getMessage(), ex));
    }

    private State getNextStateByRunnableMode(int interruptedCount, int terminatedCount, ItemStreamEvent event) {
        log.debug("[{}]: [runnableMode] = {}", event.getName() + "->" + name, runnableMode);
        if (RunnableMode.ALL.equals(runnableMode)) {
            if (terminatedCount == dependsOnState.size() - interruptedCount) {
                return RUNNABLE;
            } else {
                return WAITING;
            }
        } else if (RunnableMode.ANY.equals(runnableMode)) {
            if (terminatedCount > 0) {
                return RUNNABLE;
            } else {
                return WAITING;
            }
        } else if (RunnableMode.MUST.equals(runnableMode)) {
            if (terminatedCount == runnableMust.size() - interruptedCount) {
                return RUNNABLE;
            } else {
                return WAITING;
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public State getState() {
        return state;
    }

    public Map<Class<? extends ItemStream>, State> getDependsOnState() {
        return dependsOnState;
    }

    public List<Class<? extends ItemStream>> getBeDependsOn() {
        return beDependsOn;
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public boolean isSingle() {
        return single;
    }

    public Lock getLock() {
        return lock;
    }

    public Deque<Exception> getInterruptCause() {
        return interruptCause;
    }

    public Deque<State> getStateTrace() {
        return stateTrace;
    }

    public Executor getExecutor() {
        return executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public RunnableMode getRunnableMode() {
        return runnableMode;
    }

    public void setRunnableMode(RunnableMode runnableMode) {
        this.runnableMode = runnableMode;
    }

    public List<Class<? extends ItemStream>> getRunnableMust() {
        return runnableMust;
    }

    public InterruptedMode getInterruptedMode() {
        return interruptedMode;
    }

    public void setInterruptedMode(InterruptedMode interruptedMode) {
        this.interruptedMode = interruptedMode;
    }

    public List<Class<? extends ItemStream>> getInterruptedMust() {
        return interruptedMust;
    }

    public void setInterruptedMust(List<Class<? extends ItemStream>> interruptedMust) {
        this.interruptedMust = interruptedMust;
    }

    public void setRunnableMust(List<Class<? extends ItemStream>> runnableMust) {
        this.runnableMust = runnableMust;
    }

    public boolean isPrettyPrint() {
        return prettyPrint;
    }

    public void setPrettyPrint(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }

    public StopWatch getStopWatch() {
        return stopWatch;
    }

    public boolean isTx() {
        return tx;
    }

    public void setTx(boolean tx) {
        this.tx = tx;
    }
}
