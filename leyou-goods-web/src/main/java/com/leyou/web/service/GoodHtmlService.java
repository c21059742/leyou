package com.leyou.web.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;



@Service
public class GoodHtmlService {
    //模板引擎
    @Autowired
    private TemplateEngine templateEngine;
    @Autowired
    private GoodsService goodsService;
    //静态化方法
    public void createHtml(Long spuId){
        //初始化运行上下文,获取页面数据
        Context context = new Context();
        //设置数据模板
        context.setVariables(this.goodsService.loadData(spuId));
        PrintWriter printWriter = null;


        try {
            //将静态化的页面保存到服务器上
            File file = new File("D:\\feiqiu\\leyou\\nginx-1.16.1\\html\\item\\"+spuId+".html");
            printWriter = new PrintWriter(file);


            this.templateEngine.process("item",context,printWriter);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }finally {
            if(printWriter != null){
                printWriter.close();
            }
        }

    }


    /**
     * 根据spuId删除页面
     * @param spuId
     */
    public void deleteHtml(Long spuId) {
        File file=new File("D:\\feiqiu\\leyou\\nginx-1.16.1\\html\\item\\"+spuId+".html");
        file.deleteOnExit();

    }
}
