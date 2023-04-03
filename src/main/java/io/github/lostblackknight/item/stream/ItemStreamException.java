package io.github.lostblackknight.item.stream;

/**
 * 项目流异常
 *
 * @author chensixiang (chensixiang1234@gmail.com) 2023/3/25 11:48
 * @version 1.0.0
 */
public class ItemStreamException extends RuntimeException {

    public ItemStreamException() {
        super();
    }

    public ItemStreamException(String message) {
        super(message);
    }

    public ItemStreamException(String message, Throwable cause) {
        super(message, cause);
    }

    public ItemStreamException(Throwable cause) {
        super(cause);
    }
}
