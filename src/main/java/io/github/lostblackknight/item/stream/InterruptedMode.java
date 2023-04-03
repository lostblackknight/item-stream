package io.github.lostblackknight.item.stream;

/**
 * 打断模式
 *
 * @author chensixiang (chensixiang1234@gmail.com) 2023/3/26 12:36
 * @version 1.0.0
 * @see Item
 * @see ItemStream
 */
public enum InterruptedMode {

    /**
     * 所有
     */
    ALL,

    /**
     * 任意
     */
    ANY,

    /**
     * 必须
     */
    MUST,

    /**
     * 没有
     */
    NONE
}
