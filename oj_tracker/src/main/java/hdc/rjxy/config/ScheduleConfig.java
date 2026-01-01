package hdc.rjxy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class ScheduleConfig {

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        // 设置线程池大小为 5
        // 这样即使 02:00 两个任务同时跑，或者有一个卡住，剩下的线程还能跑 04:00 的任务
        taskScheduler.setPoolSize(5);
        taskScheduler.setThreadNamePrefix("oj-scheduler-");
        return taskScheduler;
    }
}