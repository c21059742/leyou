package com.leyou.cart.service;


import com.leyou.auth.pojo.UserInfo;
import com.leyou.cart.client.GoodsClient;
import com.leyou.cart.intercaptor.LoginInterceptor;
import com.leyou.cart.pojo.Cart;
import com.leyou.common.utils.JsonUtils;
import com.leyou.item.pojo.Sku;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private GoodsClient goodsClient;

    public void addCart(Cart cart) {
        UserInfo userInfo = LoginInterceptor.get();
        //是否查询redis中是否已有要添加的数据
        BoundHashOperations<String,Object,Object> hashOperations = redisTemplate.boundHashOps(userInfo.getId().toString());

        String skuId = cart.getSkuId().toString();
        //页面传递的数量
        Integer num = cart.getNum();
        //判断是否有
        if(hashOperations.hasKey(skuId)){
            String cartJson = hashOperations.get(skuId).toString();
            cart = JsonUtils.parse(cartJson,Cart.class);
            //更新数量
            cart.setNum(num+cart.getNum());

        }else{
            //没有就新增
            cart.setUserId(userInfo.getId());
            //查询商品信息
            Sku sku = this.goodsClient.querySkuById(cart.getSkuId());
            cart.setPrice(sku.getPrice());
            cart.setImage(StringUtils.isBlank(sku.getImages()) ? "" : StringUtils.split(sku.getImages(),",")[0]);
            cart.setOwnSpec(sku.getOwnSpec());
            cart.setTitle(sku.getTitle());

        }
        //保存到redis
        hashOperations.put(skuId,JsonUtils.serialize(cart));
    }

    public List<Cart> queryCarts() {
        //获取用户信息
        UserInfo userInfo = LoginInterceptor.get();
        //判断redis中是否存在该用户的购物车数据
        if(!this.redisTemplate.hasKey(userInfo.getId().toString())){
            return null;
        };
        //先查询redis中是否已有添加的数据
        BoundHashOperations<String,Object,Object> hashOperations = redisTemplate.boundHashOps(userInfo.getId().toString());
        List<Object> cartJsons = hashOperations.values();


        return cartJsons.stream().map(cartJson -> JsonUtils.parse(cartJson.toString(),Cart.class)
        ).collect(Collectors.toList());
    }

    public void updateCart(Cart cart) {
        //获取用户信息
        UserInfo userInfo = LoginInterceptor.get();
        //先查询redis中是否已有添加的数据
        BoundHashOperations<String,Object,Object> hashOperations = redisTemplate.boundHashOps(userInfo.getId().toString());
        // 获取购物车信息
        String cartJson = hashOperations.get(cart.getSkuId().toString()).toString();
        Cart cart1 = JsonUtils.parse(cartJson, Cart.class);
        // 更新数量
        cart1.setNum(cart.getNum());
        // 写入购物车
        hashOperations.put(cart.getSkuId().toString(), JsonUtils.serialize(cart1));


    }

    public void deleteCart(String skuId) {
        //获取用户信息
        UserInfo userInfo = LoginInterceptor.get();
        //先查询redis中是否已有添加的数据
        BoundHashOperations<String,Object,Object> hashOperations = redisTemplate.boundHashOps(userInfo.getId().toString());
        hashOperations.delete(skuId);
    }
}
