package com.xajiusuo.busi.file.contain;

import com.xajiusuo.utils.ColorUtil;
import com.xajiusuo.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Map;

/**
 * Created by hadoop on 19-7-15.
 */
@Component
public class FileUploadInit implements ApplicationListener<ApplicationEnvironmentPreparedEvent>{

    private boolean cycle;

    private void init(String[] path) {
        //文件系统容器
        FileSysAgent.cycle = cycle;
        for(String s:path){
            try {
                if(StringUtils.isBlank(s)) continue;
                if(!s.endsWith("/") && !s.endsWith("\\")){
                    s += "/";
                }
                FileSysAgent.loadPath(s);
                System.out.println(ColorUtil.blue("加载成功:\t") + ColorUtil.green(s));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //文件系统初始化
        FileSysAgent.init();
        System.out.println(ColorUtil.blue("文件服务器初始完成."));
    }

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        System.out.println("init...");
        SpringApplication app = event.getSpringApplication();
        cycle = event.getEnvironment().getProperty("file.write.cycle",Boolean.class,false);
        init(event.getEnvironment().getProperty("file.path").split(";"));
    }
}
