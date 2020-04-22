package com.leyou.web.listener;


import com.leyou.web.service.GoodHtmlService;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GoodListener {
    @Autowired
    private GoodHtmlService goodHtmlService;
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "LEYOU.ITEM.SAVE.QUEUE",durable = "true"),
            exchange = @Exchange(name="LEYOU.ITEM.EXCHANGE",ignoreDeclarationExceptions = "true",type= ExchangeTypes.TOPIC),
            key = {"item.insert","item.update"}
    ))
    public void saveListenner(Long spuId){
        if(spuId == null){
            return;
        }
        //重新生成静态页面数据
        this.goodHtmlService.createHtml(spuId);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "LEYOU.ITEM.DELETE.QUEUE",durable = "true"),
            exchange = @Exchange(name="LEYOU.ITEM.EXCHANGE",ignoreDeclarationExceptions = "true",type= ExchangeTypes.TOPIC),
            key = {"itme.delete"}
    ))
    public void deleteListenner(Long spuId){
        if(spuId == null){
            return;
        }
        this.goodHtmlService.deleteHtml(spuId);
    }
}
