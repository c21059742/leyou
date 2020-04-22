package com.leyou.cart.intercaptor;

import com.leyou.auth.pojo.UserInfo;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.cart.properties.JwtProperties;
import com.leyou.common.utils.CookieUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
@EnableConfigurationProperties(JwtProperties.class)
@Component
public class LoginInterceptor extends HandlerInterceptorAdapter {
    @Autowired
    private JwtProperties jwtProperties;

    private static final ThreadLocal<UserInfo> THREAD_LOCAL= new ThreadLocal<>();
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //获取jwt类型的token
        String token = CookieUtils.getCookieValue(request, this.jwtProperties.getCookieName());
        //判断是否为空
//        if (StringUtils.isBlank(token)){
//            return false;
//            }
        //解析，获取用户信息
        UserInfo info = JwtUtils.getInfoFromToken(token, this.jwtProperties.getPublicKey());
        //将用户信息放到request中，这样在controller中的request对象中的获取
        //将用户信息保存到THREAD_LOCAL
        THREAD_LOCAL.set(info);
        return true;
    }
    //对外提供有一个获取用户信息的方法
    public static UserInfo get(){
        return THREAD_LOCAL.get();
    }

    /**
     * 必须操作，因为我们使用的是tomcat线程池，该线程执行完成以后必须结束
     * @param request
     * @param response
     * @param handler
     * @param ex
     * @throws Exception
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        THREAD_LOCAL.remove();//清除线程变量
    }
}
