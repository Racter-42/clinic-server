package com.xiaoyu.clinic.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池配置。
 *
 * <p>缓存延迟双删需要一个「等一会儿再执行」的任务：更新完数据库，隔 500ms 再删一次缓存。
 * 原写法是每次更新都 new Thread + Thread.sleep(500)，有两个问题：
 * <ol>
 *   <li>每个更新请求都新建一个线程，并发一高线程数就失控，而且线程创建/销毁本身有开销；</li>
 *   <li>裸线程没人管：异常被吞掉查不到，应用关闭时也不会等它跑完。</li>
 * </ol>
 * 改为交给一个共享的调度线程池，线程复用、数量固定、异常能进日志。
 */
@Configuration
public class ThreadPoolConfig {

    /**
     * 缓存延迟双删专用调度线程池。
     *
     * <p>核心线程 2 个足够：这类任务只是「等 500ms 后删一个 Redis key」，几乎不占 CPU，
     * 只有数据库更新这种低频操作才会提交，不需要更多。
     * 线程名统一前缀 {@code cache-delay-}，出问题时看线程栈能一眼定位。
     *
     * <p>设为守护线程（daemon）：延迟双删只是「让缓存更快变新鲜」的优化，
     * 最坏情况是缓存旧一点、下次查询自动回源重建，不值得为它阻塞整个应用退出。
     *
     * <p>{@code destroyMethod = "shutdown"}：容器关闭时不再接收新任务。
     */
    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService cacheDelayExecutor() {
        AtomicInteger seq = new AtomicInteger(1);
        ThreadFactory factory = r -> {
            Thread t = new Thread(r, "cache-delay-" + seq.getAndIncrement());
            t.setDaemon(true);
            return t;
        };
        return new ScheduledThreadPoolExecutor(2, factory);
    }
}
