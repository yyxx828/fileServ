package com.xajiusuo;

import com.xajiusuo.busi.file.contain.FileUploadInit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.concurrent.Executors;


/**
 * Created by zlm on 2018/1/17.
 * @author zlm
 */
@EnableScheduling
@SpringBootApplication
@ServletComponentScan
public class FileMainApp extends SpringBootServletInitializer{

    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor(){
        return new MethodValidationPostProcessor();
    }

    public static void main(String[] args) throws UnknownHostException{
        SpringApplication app = new SpringApplication(FileMainApp.class);
        app.addListeners(new FileUploadInit());
        Environment env = app.run(args).getEnvironment();
        String protocol = "http";
        System.out.println(MessageFormat.format(
                "\n----------------------------------------------------------\n"
                + "\tApplication ''{0}'' is running! \n\tAccess URLs:\n"
                + "\tLocal: \t\t{1}://localhost:{2}\n"
                + "\tExternal: \t{1}://{3}:{2}\n"
                + "----------------------------------------------------------",
                env.getProperty("spring.application.name"), protocol, env.getProperty("server.port"),InetAddress.getLocalHost().getHostAddress()));
        System.out.println( "文件服务系统已经正常启动!>-_-<" );
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder){
        return builder.sources(this.getClass());
    }
}
