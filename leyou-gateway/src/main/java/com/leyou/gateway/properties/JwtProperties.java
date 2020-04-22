package com.leyou.gateway.properties;

import com.leyou.auth.utils.RsaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;

@ConfigurationProperties(prefix = "leyou.jwt")
public class JwtProperties {


    private String pubKeyPath;// 公钥


    private int expire;// token过期时间

    private PublicKey publicKey; // 公钥


    private static final Logger logger = LoggerFactory.getLogger(JwtProperties.class);

    private String cookieName;

    public String getCookieName() {
        return cookieName;
    }

    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }

    /**
     * @PostContruct：在构造方法执行之后执行该方法
     */
    @PostConstruct
    public void init(){
        try {
            // 获取公钥和私钥
            this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        } catch (Exception e) {
            logger.error("初始化公钥和私钥失败！", e);
            throw new RuntimeException();
        }
    }



    public void setPubKeyPath(String pubKeyPath) {
        this.pubKeyPath = pubKeyPath;
    }



    public void setExpire(int expire) {
        this.expire = expire;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }



    public int getExpire() {
        return expire;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

}