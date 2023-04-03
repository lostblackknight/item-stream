package io.github.lostblackknight.item.stream;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjUtil;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.github.lostblackknight.item.stream.State.ROLLBACK;

/**
 * 项目流 Collector
 *
 * @author chensixiang (chensixiang1234@gmail.com) 2023/3/25 22:28
 * @version 1.0.0
 * @see ItemStream
 */
public abstract class ItemStreamCollector<I, O> extends ItemStream {

    /**
     * 上下文
     */
    private Context<I, O> context;

    @Override
    protected void doRunnableInterval() throws Exception {
        collect(context.getOutput(), context);
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
        context.getCountDownLatch().countDown();
        if (isPrettyPrint()) {
            log.info(prettyPrint());
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
        context.getCountDownLatch().countDown();
        if (isPrettyPrint()) {
            log.info(prettyPrint());
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

    protected String prettyPrint() {
        final Queue<StopWatch> stopWatches = getContext().getStopWatches();
        final TimeUnit unit = TimeUnit.MILLISECONDS;
        final StringBuilder sb = new StringBuilder("Total Task Summary");
        sb.append(FileUtil.getLineSeparator());
        if (ObjUtil.isEmpty(stopWatches)) {
            sb.append("No task info kept");
        } else {
            sb.append("---------------------------------------------------------").append(FileUtil.getLineSeparator());
            sb.append(DateUtil.getShotName(unit)).append("       \t  %\t\t  Task").append(FileUtil.getLineSeparator());
            sb.append("---------------------------------------------------------").append(FileUtil.getLineSeparator());

            final NumberFormat nf = NumberFormat.getNumberInstance();
            nf.setMinimumIntegerDigits(9);
            nf.setGroupingUsed(false);

            final NumberFormat pf = NumberFormat.getPercentInstance();
            pf.setMinimumIntegerDigits(1);
            pf.setMaximumIntegerDigits(3);
            pf.setGroupingUsed(false);

            final List<StopWatch.TaskInfo> taskInfos = stopWatches.stream()
                    .flatMap(stopWatch -> Arrays.stream(stopWatch.getTaskInfo()))
                    .collect(Collectors.toList());

            long totalTimeNanos = stopWatches.stream()
                    .map(StopWatch::getTotalTimeNanos)
                    .reduce(Long::sum)
                    .orElse(0L);

            long totalTime = 0;
            for (StopWatch.TaskInfo task : taskInfos) {
                sb.append(nf.format(task.getTime(unit))).append("\t  ");
                sb.append(pf.format((double) task.getTimeNanos() / totalTimeNanos)).append("\t  ");
                sb.append(task.getTaskName()).append(FileUtil.getLineSeparator());
                totalTime += task.getTime(unit);
            }
            sb.append("---------------------------------------------------------").append(FileUtil.getLineSeparator());
            sb.append(nf.format(totalTime)).append("\t  ");
            sb.append("100%").append("\t  ");
            sb.append("Total").append(FileUtil.getLineSeparator());
            sb.append("---------------------------------------------------------").append(FileUtil.getLineSeparator());
        }
        return sb.toString();
    }

    /**
     * 初始化
     *
     * @param context 上下文
     */
    public void setup(Context<I, O> context) {
    }

    /**
     * 收集
     *
     * @param output  输出
     * @param context 上下文
     */
    public abstract void collect(O output, Context<I, O> context) throws Exception;

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
