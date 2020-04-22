package com.leyou.web.contorller;

import com.leyou.web.service.GoodHtmlService;
import com.leyou.web.service.GoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@Controller
public class GoodsController {
    @Autowired
    private GoodsService goodsService;
    @Autowired
    private GoodHtmlService goodHtmlService;

    /**
     * 通过spuId获取模板需要的数据
     * @param id
     * @param model
     * @return
     */
    @GetMapping("item/{id}.html")
    public String toItemPage(@PathVariable("id")Long id, Model model){
        Map<String, Object> map = goodsService.loadData(id);
        model.addAllAttributes(map);
        //调用静态生成html的页面
        this.goodHtmlService.createHtml(id);
        return "item";
    }
}
