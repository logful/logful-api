package com.getui.logful.server.system;

import com.getui.logful.server.parse.GraylogClientService;
import com.getui.logful.server.system.jvm.JvmStats;
import com.getui.logful.server.system.process.ProcessStats;
import com.getui.logful.server.weed.WeedFSClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StatsService {

    @Autowired
    private GraylogClientService graylogClientService;

    @Autowired
    private WeedFSClientService weedFSClientService;

    public SystemStats systemStats() {
        return SystemStats.create(JvmStats.INSTANCE, null,
                ProcessStats.create(),
                SystemStats.WeedFSStats.create(weedFSClientService.isConnected(), null),
                SystemStats.GraylogStats.create(graylogClientService.isConnected()));
    }

}
