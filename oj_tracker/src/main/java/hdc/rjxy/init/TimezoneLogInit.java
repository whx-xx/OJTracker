package hdc.rjxy.init;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import hdc.rjxy.task.SyncScheduler;

import java.time.ZoneId;
import java.util.TimeZone;

@Component
public class TimezoneLogInit implements InitializingBean {

    private final SyncScheduler scheduler;

    public TimezoneLogInit(SyncScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public void afterPropertiesSet() {
        System.out.println("[TZ] TimeZone.getDefault() = " + TimeZone.getDefault().getID());
        System.out.println("[TZ] ZoneId.systemDefault() = " + ZoneId.systemDefault());
        System.out.println("定时任务是否开启：" + scheduler.isEnabled());
    }
}
