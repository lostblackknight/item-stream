package io.github.lostblackknight.item.stream;

/**
 * 状态
 *
 * @author chensixiang (chensixiang1234@gmail.com) 2023/3/23 10:58
 * @version 1.0.0
 * @see ItemStream
 */
public enum State {

    /**
     * 创建
     */
    NEW,

    /**
     * 运行
     */
    RUNNABLE,

    /**
     * 等待
     */
    WAITING,

    /**
     * 终止
     */
    TERMINATED,

    /**
     * 打断
     */
    INTERRUPTED,

    /**
     * 回滚
     */
    ROLLBACK
}
