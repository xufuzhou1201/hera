package com.dfire.core.netty.master;

import com.dfire.common.entity.vo.HeraHostGroupVo;
import com.dfire.common.service.*;
import com.dfire.common.util.NamedThreadFactory;
import com.dfire.common.vo.JobElement;
import com.dfire.config.HeraGlobalEnv;
import com.dfire.core.event.Dispatcher;
import com.dfire.core.quartz.QuartzSchedulerService;
import com.dfire.logs.ErrorLog;
import com.dfire.logs.HeraLog;
import com.dfire.monitor.service.AlarmCenter;
import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.*;

/**
 * @author: <a href="mailto:lingxiao@2dfire.com">凌霄</a>
 * @time: Created in 14:10 2018/1/12
 * @desc hera调度器执行上下文
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Component
@Order(2)
public class MasterContext {

    /**
     * todo 参数可配置
     */

    protected ScheduledThreadPoolExecutor masterSchedule;
    @Autowired
    private Master master;
    private Map<Channel, MasterWorkHolder> workMap = new ConcurrentHashMap<>();
    @Autowired
    private HeraHostGroupService heraHostGroupService;
    @Autowired
    @Qualifier("heraFileMemoryService")
    private HeraFileService heraFileService;
    @Autowired
    private QuartzSchedulerService quartzSchedulerService;
    @Autowired
    @Qualifier("heraGroupMemoryService")
    private HeraGroupService heraGroupService;
    @Autowired
    private HeraJobHistoryService heraJobHistoryService;
    @Autowired
    private HeraUserService heraUserService;
    @Autowired
    @Qualifier("heraJobMemoryService")
    private HeraJobService heraJobService;
    @Autowired
    private HeraAreaService heraAreaService;
    @Autowired
    private HeraDebugHistoryService heraDebugHistoryService;
    @Autowired
    private HeraJobActionService heraJobActionService;
    @Autowired
    private AlarmCenter alarmCenter;
    @Autowired
    private HeraJobMonitorService heraJobMonitorService;
    @Autowired
    private HeraSsoService heraSsoService;
    @Autowired
    @Getter
    private EmailService emailService;
    @Autowired
    private HeraRerunService heraRerunService;
    private Dispatcher dispatcher;
    private Map<Integer, HeraHostGroupVo> hostGroupCache;
    private BlockingQueue<JobElement> scheduleQueue = new PriorityBlockingQueue<>(10000, (o1, o2) -> {
        //优先根据任务优先级排列
        if (o1.getPriorityLevel() > o2.getPriorityLevel()) {
            return -1;
        } else if (o1.getPriorityLevel().equals(o2.getPriorityLevel())) {
            //如果任务优先级相等，根据版本的时间排序，一般来说版本时间越小越优先执行
            long firstId = o1.getJobId() / 1000000;
            long secondId = o2.getJobId() / 1000000;
            if (firstId < secondId) {
                return -1;
            } else if (firstId == secondId) {
                return 0;
            }
        }
        return 1;
    });
    private BlockingQueue<JobElement> debugQueue = new LinkedBlockingQueue<>(10000);
    private BlockingQueue<JobElement> manualQueue = new LinkedBlockingQueue<>(10000);
    private MasterHandler handler;
    private MasterServer masterServer;
    @Getter
    private ExecutorService threadPool;

    public void init() {
        threadPool = new ThreadPoolExecutor(
                0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), new NamedThreadFactory("master-wait-response"), new ThreadPoolExecutor.AbortPolicy());
        masterSchedule = new ScheduledThreadPoolExecutor(5, new NamedThreadFactory("master-schedule", false));
        masterSchedule.setKeepAliveTime(5, TimeUnit.MINUTES);
        masterSchedule.allowCoreThreadTimeOut(true);
        this.getQuartzSchedulerService().start();
        dispatcher = new Dispatcher();
        handler = new MasterHandler(this);
        masterServer = new MasterServer(handler);
        masterServer.start(HeraGlobalEnv.getConnectPort());
        master.init(this);
        HeraLog.info("end init master content success ");
    }

    public void destroy() {
        threadPool.shutdown();
        masterSchedule.shutdown();
        if (masterServer != null) {
            masterServer.shutdown();
        }
        if (quartzSchedulerService != null) {
            try {
                quartzSchedulerService.shutdown();
                HeraLog.info("quartz schedule shutdown success");
            } catch (Exception e) {
                ErrorLog.error("quartz schedule shutdown error", e);
            }
        }
        HeraLog.info("destroy master context success");
    }

    public synchronized Map<Integer, HeraHostGroupVo> getHostGroupCache() {
        return hostGroupCache;
    }


    public synchronized void refreshHostGroupCache() {
        try {
            hostGroupCache = getHeraHostGroupService().getAllHostGroupInfo();
        } catch (Exception e) {
            HeraLog.info("refresh host group error");
        }
    }


}
