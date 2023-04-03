package io.github.lostblackknight.item.stream;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ObjUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static io.github.lostblackknight.item.stream.State.RUNNABLE;

/**
 * 项目流客户端
 *
 * @author chensixiang (chensixiang1234@gmail.com) 2023/3/23 16:55
 * @version 1.0.0
 */
public class ItemStreamClient<I, O> {

    /**
     * Context Holder
     */
    private final ThreadLocal<Context<I, O>> contextHolder = new InheritableThreadLocal<>();

    /**
     * mapperClass Cache
     */
    private final Map<String, Set<Class<?>>> mapperClassCache = new ConcurrentHashMap<>(256);

    /**
     * 全局配置
     */
    private final GlobalSetup globalSetup;

    private ItemStreamClient(GlobalSetup globalSetup) {
        this.globalSetup = globalSetup;
    }

    /**
     * 创建项目流客户端
     *
     * @param globalSetup 全局配置
     * @return 项目流客户端
     */
    public static <I, O> ItemStreamClient<I, O> create(GlobalSetup globalSetup) {
        return new ItemStreamClient<>(globalSetup);
    }

    /**
     * 创建项目流客户端
     *
     * @param globalSetupSupplier 全局配置提供者
     * @return 项目流客户端
     */
    public static <I, O> ItemStreamClient<I, O> create(Supplier<GlobalSetup> globalSetupSupplier) {
        return new ItemStreamClient<>(globalSetupSupplier.get());
    }

    /**
     * 初始化
     *
     * @return this
     */
    public ItemStreamClient<I, O> init() {
        putContext(new Context<>());
        return this;
    }

    /**
     * 设置输入
     *
     * @param input 输入
     * @return this
     */
    public ItemStreamClient<I, O> input(I input) {
        getContext().setInput(input);
        return this;
    }

    /**
     * 设置输入
     *
     * @param inputSupplier 输入提供者
     * @return this
     */
    public ItemStreamClient<I, O> input(Supplier<I> inputSupplier) {
        getContext().setInput(inputSupplier.get());
        return this;
    }

    /**
     * 设置输出
     *
     * @param output 输出
     * @return this
     */
    public ItemStreamClient<I, O> output(O output) {
        getContext().setOutput(output);
        return this;
    }

    /**
     * 设置输出
     *
     * @param outputSupplier 输出提供者
     * @return this
     */
    public ItemStreamClient<I, O> output(Supplier<O> outputSupplier) {
        getContext().setOutput(outputSupplier.get());
        return this;
    }

    /**
     * 设置 mapperClass
     *
     * @param mapperClasses mapperClasses
     * @return this
     */
    @SuppressWarnings("unchecked")
    public ItemStreamClient<I, O> mapperClass(List<Class<?>> mapperClasses) throws ItemStreamException {
        for (Class<?> mapperClass : mapperClasses) {
            final Item item = AnnotationUtil.getAnnotation(mapperClass, Item.class);
            final Class<? extends ItemStream>[] dependsOn = item.dependsOn();
            final ItemStreamMapper<I, O> mapper;
            try {
                mapper = (ItemStreamMapper<I, O>) mapperClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new ItemStreamException("Mapper creation failed.");
            }
            for (Class<? extends ItemStream> dependsOnClass : dependsOn) {
                mapper.putDependsOn(dependsOnClass);
                if (ObjUtil.isNotEmpty(getContext().getBeDependsOn().get(dependsOnClass))) {
                    getContext().getBeDependsOn().get(dependsOnClass).add(mapperClass);
                } else {
                    getContext().getBeDependsOn().put(dependsOnClass, new HashSet<>(Collections.singleton(mapperClass)));
                }
            }
            mapper.setContext(getContext());
            setup(item, mapper);
            mapper.setup(getContext());
            getContext().getMappers().put(mapperClass, mapper);
        }
        getContext().getMappers().forEach((clazz, mapper) -> {
            final Set<Class<?>> beDependsOnClasses = getContext().getBeDependsOn().get(clazz);
            if (ObjUtil.isNotEmpty(beDependsOnClasses)) {
                final List<ItemStream> itemStreams = new ArrayList<>();
                beDependsOnClasses.forEach(beDependsOnClass -> itemStreams.add(getContext().getMappers().get(beDependsOnClass)));
                mapper.register(itemStreams);
            }
            if (ObjUtil.isNotEmpty(getContext().getCollector())) {
                final Item item = AnnotationUtil.getAnnotation(getContext().getCollector().getClass(), Item.class);
                final Class<? extends ItemStream>[] dependsOn = item.dependsOn();
                if (ArrayUtil.contains(dependsOn, clazz)) {
                    mapper.register(getContext().getCollector());
                }
            }
        });
        return this;
    }

    /**
     * 设置 mapperClass
     *
     * @param packageName mapperClass 所在的包名
     * @return this
     */
    public ItemStreamClient<I, O> mapperClass(String packageName) throws ItemStreamException {
        if (ObjUtil.isNotEmpty(mapperClassCache.get(packageName))) {
            return mapperClass(new ArrayList<>(mapperClassCache.get(packageName)));
        } else {
            final Set<Class<?>> mapperClasses = ClassUtil.scanPackageBySuper(packageName, ItemStreamMapper.class);
            mapperClassCache.put(packageName, mapperClasses);
            return mapperClass(new ArrayList<>(mapperClasses));
        }
    }

    /**
     * 设置 collectorClass
     *
     * @param collectorClass collectorClass
     * @return this
     */
    public ItemStreamClient<I, O> collectorClass(Class<? extends ItemStreamCollector<I, O>> collectorClass) throws ItemStreamException {
        final Item item = AnnotationUtil.getAnnotation(collectorClass, Item.class);
        final Class<? extends ItemStream>[] dependsOn = item.dependsOn();
        final ItemStreamCollector<I, O> collector;
        try {
            collector = collectorClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ItemStreamException("Collector creation failed.");
        }
        for (Class<? extends ItemStream> dependsOnClass : dependsOn) {
            collector.putDependsOn(dependsOnClass);
        }
        collector.setContext(getContext());
        setup(item, collector);
        collector.setup(getContext());
        getContext().setCollector(collector);
        for (Class<? extends ItemStream> dependsOnClass : dependsOn) {
            final ItemStreamMapper<?, ?> itemStreamMapper = getContext().getMappers().get(dependsOnClass);
            if (ObjUtil.isNotEmpty(itemStreamMapper)) {
                itemStreamMapper.register(collector);
            }
        }
        return this;
    }

    private void setup(Item item, ItemStream itemStream) {
        itemStream.setRunnableMode(item.runnableMode());
        itemStream.setRunnableMust(Arrays.asList(item.runnableMust()));
        itemStream.setInterruptedMode(item.interruptedMode());
        itemStream.setInterruptedMust(Arrays.asList(item.interruptedMust()));
        if (ObjUtil.isNotEmpty(item.prettyPrint())) {
            itemStream.setPrettyPrint(Boolean.parseBoolean(item.prettyPrint()));
        } else {
            itemStream.setPrettyPrint(globalSetup.isPrettyPrint());
        }
        if (ObjUtil.isNotEmpty(item.tx())) {
            itemStream.setTx(Boolean.parseBoolean(item.tx()));
        } else {
            itemStream.setTx(globalSetup.isTx());
        }
        itemStream.setExecutor(globalSetup.getExecutor());
    }

    /**
     * 运行
     *
     * @return 输出
     */
    public O run() throws InterruptedException {
        getContext().getMappers().values().stream()
                .filter(ItemStream::isSingle)
                .forEach(mapper -> mapper.setState(RUNNABLE));
        if (ObjUtil.isNotEmpty(getContext().getCollector()) && ObjUtil.isNotEmpty(getContext().getMappers())) {
            getContext().getCountDownLatch().await();
        }
        return getContext().getOutput();
    }

    /**
     * 清理
     */
    public void clear() {
        clearContext();
    }

    private void putContext(Context<I, O> context) {
        contextHolder.set(context);
    }

    public Context<I, O> getContext() {
        return contextHolder.get();
    }

    private void clearContext() {
        contextHolder.remove();
    }
}
