package io.github.lostblackknight.item.stream;

import java.lang.annotation.*;

/**
 * 项目注解，配合 {@link ItemStream} 使用
 *
 * @author chensixiang (chensixiang1234@gmail.com) 2023/3/23 11:16
 * @version 1.0.0
 * @see ItemStreamMapper
 * @see ItemStreamCollector
 * @see ItemStreamClient
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Item {

    /**
     * 依赖的项目流
     */
    Class<? extends ItemStream>[] dependsOn() default {};

    /**
     * 运行模式
     */
    RunnableMode runnableMode() default RunnableMode.ALL;

    /**
     * 运行模式必须依赖的项目流，配合 {@link #runnableMode()} 使用
     */
    Class<? extends ItemStream>[] runnableMust() default {};

    /**
     * 打断模式
     */
    InterruptedMode interruptedMode() default InterruptedMode.ANY;

    /**
     * 打断模式必须依赖的项目流，配合 {@link #interruptedMode()} 使用
     */
    Class<? extends ItemStream>[] interruptedMust() default {};

    /**
     * 是否打印耗时
     * <p>为 false 关闭打印
     * <p>为 true 开启打印
     * <p>为 "" 采取 {@link GlobalSetup#isPrettyPrint()} 的配置
     * <h3>注意：注解的优先级大于 {@link GlobalSetup#isPrettyPrint()}
     */
    String prettyPrint() default "";

    /**
     * 是否开启事务
     * <p>为 false 不会回滚
     * <p>为 true 会回滚
     * <p>为 "" 采取 {@link GlobalSetup#isTx()} 的配置
     * <h3>注意：注解的优先级大于 {@link GlobalSetup#isTx()}
     */
    String tx() default "";
}
