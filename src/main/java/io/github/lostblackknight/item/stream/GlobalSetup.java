package io.github.lostblackknight.item.stream;

import java.util.concurrent.Executor;

/**
 * 全局配置
 *
 * @author chensixiang (chensixiang1234@gmail.com) 2023/3/26 23:25
 * @version 1.0.0
 * @see ItemStreamClient
 */
public class GlobalSetup {

    /**
     * 线程池
     */
    private Executor executor = null;

    /**
     * 是否打印耗时
     */
    private boolean prettyPrint = false;

    /**
     * 是否开启事务
     */
    private boolean tx = false;

    public Executor getExecutor() {
        return executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public boolean isPrettyPrint() {
        return prettyPrint;
    }

    public void setPrettyPrint(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }

    public boolean isTx() {
        return tx;
    }

    public void setTx(boolean tx) {
        this.tx = tx;
    }
}
