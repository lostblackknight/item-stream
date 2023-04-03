package io.github.lostblackknight.item.stream;

import java.util.concurrent.TimeUnit;

import static io.github.lostblackknight.item.stream.State.ROLLBACK;

/**
 * 项目流 Mapper
 *
 * @author chensixiang (chensixiang1234@gmail.com) 2023/3/23 16:44
 * @version 1.0.0
 * @see ItemStream
 */
public abstract class ItemStreamMapper<I, O> extends ItemStream {

    /**
     * 上下文
     */
    private Context<I, O> context;

    @Override
    protected void doRunnableInterval() throws Exception {
        map(context.getInput(), context.getOutput(), context);
    }

    @Override
    protected void doRunnableFinally() {
        cleanup(context);
    }

    @Override
    protected void doInterrupt() {
        super.doInterrupt();
        ex(getInterruptCause().peekLast(), context);
        if (isPrettyPrint()) {
            getContext().getStopWatches().offer(getStopWatch());
        }
        if (isTx()) {
            getContext().getTxEventBus().register(this);
            getContext().getTxEventBus().post(new ItemStreamEvent(getName(), this.getClass(), ROLLBACK, getInterruptCause().peekLast()));
        }
    }

    @Override
    protected void doTerminated() {
        super.doTerminated();
        if (isPrettyPrint()) {
            getContext().getStopWatches().offer(getStopWatch());
        }
        if (isTx()) {
            getContext().getTxEventBus().register(this);
        }
    }

    @Override
    protected void doRollback() {
        getStopWatch().start(getName() + "-" + ROLLBACK);
        rollback(context);
        getStopWatch().stop();
        if (isPrettyPrint()) {
            log.info(getStopWatch().shortSummary(TimeUnit.MILLISECONDS));
        }
    }

    /**
     * 初始化
     *
     * @param context 上下文
     */
    public void setup(Context<I, O> context) {
    }

    /**
     * 转化
     *
     * @param input   输入
     * @param output  输出
     * @param context 上下文
     */
    public abstract void map(I input, O output, Context<I, O> context) throws Exception;

    /**
     * 异常处理
     *
     * @param ex      异常
     * @param context 上下文
     */
    public void ex(Exception ex, Context<I, O> context) {
    }

    /**
     * 清理
     *
     * @param context 上下文
     */
    public void cleanup(Context<I, O> context) {
    }

    /**
     * 事务回滚
     *
     * @param context 上下文
     */
    public void rollback(Context<I, O> context) {
    }

    public Context<I, O> getContext() {
        return context;
    }

    public void setContext(Context<I, O> context) {
        this.context = context;
    }
}
