package com.leyou.auth.service;

import com.leyou.auth.client.UserClient;
import com.leyou.auth.config.JwtProperties;
import com.leyou.auth.pojo.UserInfo;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.user.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

@Service
@EnableConfigurationProperties(JwtProperties.class)
public class AuthService {

    @Autowired
    private UserClient userClient;

    @Autowired
    private JwtProperties jwtProperties;

    public String login(String username, String password) {
        //远程的调用用户中心查询用户接口
        User user = userClient.queryUser(username, password);
        //判断用户是否为空
        if(user==null){
            return null;
        }
        //初始化一个userInfo
        UserInfo userInfo = new UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());

        //生成jwt类型的token
        try {
            return JwtUtils.generateToken(userInfo,this.jwtProperties.getPrivateKey(),this.jwtProperties.getExpire());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public UserInfo verify(String token) {
        try {
            //通过工具类解析jwt类型的token，获取用户信息
            return  JwtUtils.getInfoFromToken(token, this.jwtProperties.getPublicKey());

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
