package com.complex.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling 仅为 T12 看板 ETL（DashboardAggregationJob）提供定时基础设施；
// 该 @Scheduled 任务默认通过 @ConditionalOnProperty 关闭，需显式开启配置才注册，故默认不会有调度运行。
@EnableScheduling
@SpringBootApplication
public class ComplexApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ComplexApiApplication.class, args);
    }
}
