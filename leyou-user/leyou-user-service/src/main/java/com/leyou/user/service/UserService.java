package com.leyou.user.service;



import com.leyou.common.utils.NumberUtils;
import com.leyou.user.mapper.UserMapper;
import com.leyou.user.pojo.User;
import com.leyou.user.utils.CodecUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    //key增加前缀
    private static  final String KEY_PREFIX="user:verify:";
    /**
     * 检验用户名和手机号是否唯一
     * @param data 要校验的数据
     * @param type  要校验的数据类型  1-用户名  2-手机
     * @return
     */
    public Boolean checkUser(String data, Integer type) {
        User record = new User();
        if(type==1){
            record.setUsername(data);
        }else if (type==2){
            record.setPhone(data);
        }else{
            return null;
        }
        return this.userMapper.selectCount(record)==0;

    }

    public void verifyCode(String phone) {
        //判断用户传入的手机号是否为空
        if(StringUtils.isBlank(phone)){
            return;
        }
        //生成验证码
        String code = NumberUtils.generateCode(6);
        //把验证码发送给消息队列
        HashMap<String, String> msg = new HashMap<>();
        msg.put("phone",phone);
        msg.put("code",code);

        this.amqpTemplate.convertAndSend("LEYOU.SMS.EXCHANGE","sms.verify",msg);
        //缓存验证码redis
        this.stringRedisTemplate.opsForValue().set(KEY_PREFIX+phone,code,5, TimeUnit.MINUTES);
    }
    /**
     * 注册用户接口
     * @param user
     * @param code
     * @return
     */
    public void register(User user, String code) {
        //1.检验验证码
            //从redis中获取验证码和用户输出的code做比较
        String redisCode = this.stringRedisTemplate.opsForValue().get(KEY_PREFIX + user.getPhone());
        if(!StringUtils.equals(code,redisCode)){
            return;
        }
        //2.生成随机码
        String salt = CodecUtils.generateSalt();
        //把盐保存下来
        user.setSalt(salt);
        //3.加盐加密储存
        user.setPassword(CodecUtils.md5Hex(user.getPassword(),salt));
        //4.新增用户
        user.setId(null);
        user.setCreated(new Date());
        this.userMapper.insertSelective(user);

        //把redis中的验证码删除，减少占用内存
        this.stringRedisTemplate.delete(KEY_PREFIX + user.getPhone());
    }

    public User queryUser(String username, String password) {
        //1.先根据用户名查询用户
        User record = new User();
        record.setUsername(username);
        User user = this.userMapper.selectOne(record);
        if(user == null){
            return null;//抛异常
        }
        //2.给用户输入的密码进行加盐加密并赋值给用户输入的password
        password = CodecUtils.md5Hex(password, user.getSalt());
        //3.判断用户输入的密码加盐加密后与数据库密码是否一致
        if(!StringUtils.equals(user.getPassword(),password)){
            return null;//抛异常
        }
        return user;
    }
}
