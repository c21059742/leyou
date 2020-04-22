package com.leyou.web.service;

import com.leyou.item.pojo.*;
import com.leyou.web.client.BrandClient;
import com.leyou.web.client.CategoryClient;
import com.leyou.web.client.GoodsClient;
import com.leyou.web.client.SpecificationClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GoodsService {
    @Autowired
    private BrandClient brandClient;
    @Autowired
    private CategoryClient categoryClient;
    @Autowired
    private GoodsClient goodsClient;
    @Autowired
    private SpecificationClient specificationClient;

    public Map<String,Object> loadData(Long spuId){
        Map<String, Object> model = new HashMap<>();
        //根据spuid查询spu
        Spu spu = this.goodsClient.querySpuById(spuId);
        //根据spuid查询spudetail
        SpuDetail spuDetail = this.goodsClient.querySpuDetailBySpuId(spuId);
        //查询分类集合map<String,Object>
        List<Long> cids = Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3());
        List<String> names = this.categoryClient.queryNamesByIds(cids);
        //初始化一个分类的map
        List<Map<String, Object>> categories = new ArrayList<>();
        //获取id
        for (int i = 0; i < cids.size(); i++) {
            Map<String, Object> map = new HashMap<>();
            map.put("id",cids.get(i));
            map.put("name",names.get(i));
            categories.add(map);
        }
        // 根据spuId查询品牌
        Brand brand = this.brandClient.queryBrandById(spu.getBrandId());
        //查询skus
        List<Sku> skus=this.goodsClient.querySkusBySpuId(spuId);
        //查询规格参数组
        List<SpecGroup> groups = this.specificationClient.queryGroupWithParam(spu.getCid3());
        //查询特殊的规格参数，处理一个map结构
        List<SpecParam> params = this.specificationClient.queryParams(null, spu.getCid3(), false, false);
        //初始化一个存放特殊参数的map
        Map<Long,String> paramMap = new HashMap<>();
        params.forEach(param -> {
            paramMap.put(param.getId(),param.getName());
        });

        model.put("spu",spu);
        model.put("spuDetail",spuDetail);
        model.put("categories",categories);
        model.put("skus",skus);
        model.put("brand",brand);
        model.put("groups",groups);
        model.put("paramMap",paramMap);
    return model;
    }

}
