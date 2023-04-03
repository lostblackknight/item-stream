package io.github.lostblackknight.item.stream;

/**
 * 项目流事件
 *
 * @author chensixiang (chensixiang1234@gmail.com) 2023/3/23 14:02
 * @version 1.0.0
 * @see ItemStream
 */
public class ItemStreamEvent {

    /**
     * 项目流的名称
     */
    private String name;

    /**
     * 项目流
     */
    private Class<? extends ItemStream> itemStream;

    /**
     * 项目流的状态
     */
    private State state;

    /**
     * 异常
     */
    private Exception ex;

    public ItemStreamEvent() {
    }

    public ItemStreamEvent(String name, Class<? extends ItemStream> itemStream, State state, Exception ex) {
        this.name = name;
        this.itemStream = itemStream;
        this.state = state;
        this.ex = ex;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Class<? extends ItemStream> getItemStream() {
        return itemStream;
    }

    public void setItemStream(Class<? extends ItemStream> itemStream) {
        this.itemStream = itemStream;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Exception getEx() {
        return ex;
    }

    public void setEx(Exception ex) {
        this.ex = ex;
    }
}
