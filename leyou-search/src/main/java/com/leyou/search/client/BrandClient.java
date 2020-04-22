package com.leyou.search.client;

import com.leyou.item.api.BrandApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient("item-service")
//ctrl+shitf+t创建测试类
public interface BrandClient extends BrandApi{

}
