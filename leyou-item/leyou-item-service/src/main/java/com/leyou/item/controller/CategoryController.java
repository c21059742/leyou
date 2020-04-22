package com.leyou.item.controller;

import com.leyou.item.pojo.Category;
import com.leyou.item.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("category")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;
    //查询分类，返回集合 Rest风格的写法，作为统一的返回类型(返回的数据，状态码等信息)

    /**
     * 根据父节点id查询子节点
     * @param pid
     * @return
     */
    @GetMapping("list")
    public ResponseEntity<List<Category>> queryCategoryByPid(@RequestParam(value = "pid",defaultValue = "0") Long pid){

        //1.判断传入的参数是否合法
        if (pid == null || pid < 0){
            return ResponseEntity.badRequest().build();
        }
        //查询  对返回的list进行判断
        //2.如果合法就调用service方法查询
        List<Category> categories = this.categoryService.queryCategoryByPid(pid);
        //3.判断查询结果，如果为空，返回404
        if (CollectionUtils.isEmpty(categories)){
            //404  服务器资源未找到
//                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            return ResponseEntity.notFound().build();
        }
        //200  查询成功，返回查询数据
        return ResponseEntity.ok(categories);

    }

    /**
     * 根据id查询分类名称
     * @param ids
     * @return
     */
    @GetMapping
    public ResponseEntity<List<String>> queryNamesByIds(@RequestParam("ids") List<Long> ids){
        List<String> names = this.categoryService.queryNameByIds(ids);
        if (CollectionUtils.isEmpty(names)){
            //404：服务资源未找到
            //404not found
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(names);
    }

}
