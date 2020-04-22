package com.leyou;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;

/**
 * date: 2020/1/9 18:29
 * author: lh
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableZuulProxy
public class LeyouApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(LeyouApiGatewayApplication.class,args);
    }
}
