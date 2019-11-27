package com.leyou.user.service;

import com.leyou.common.utils.NumberUtils;
import com.leyou.user.mapper.UserMapper;
import com.leyou.user.pojo.User;
import com.leyou.user.utils.CodecUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AmqpTemplate amqpTemplate;
    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "user:verify:";
    /**
     * 校验数据是否可用
     * @param data
     * @param type
     * @return
     */
    public Boolean checkUser(String data, Integer type) {
        User record = new User();
        if (type==1){
            record.setUsername(data);
        }else if (type==2){
            record.setPhone(data);
        }else {
            return null;
        }
        return userMapper.selectCount(record)==0;
    }

    public void sendVerifyCode(String phone) {
        if (StringUtils.isBlank(phone)){
            return;
        }
        //生成6位随机验证码
        String code = NumberUtils.generateCode(6);
        //发送消息到rabbitMQ(phone,code)
        Map<String,String> map = new HashMap<>();
        map.put("phone",phone);
        map.put("code",code);
        amqpTemplate.convertAndSend("LEYOU.SMS.EXCHANGE","verifycode.sms",map);
        //把验证码保存到redis中，以便校验
        redisTemplate.opsForValue().set(KEY_PREFIX+phone,code,5, TimeUnit.MINUTES);
    }

    public void register(User user, String code) {
        //1.校验验证码
        //查询redis中的验证码
        String redisCode = redisTemplate.opsForValue().get(KEY_PREFIX + user.getPhone());
        if (!StringUtils.equals(code,redisCode)){
            return;
        }
        //2.生成盐
        String salt = CodecUtils.generateSalt();
        user.setSalt(salt);
        //3.加盐加密(md5算法加密)
        user.setPassword(CodecUtils.md5Hex(user.getPassword(),salt));
        //4.新增用户
        user.setId(null);
        user.setCreated(new Date());
        userMapper.insertSelective(user);
    }

    public User queryUser(String username,String password) {
        User record = new User();
        record.setUsername(username);
        User user = userMapper.selectOne(record);
        //判断user是否为空
        if (user==null){
            return null;
        }
        //获取盐，对用户输入的密码加盐加密
        password = CodecUtils.md5Hex(password,user.getSalt());
        //和数据库密码比对
        if (StringUtils.equals(password,user.getPassword())){
            return user;
        }
        return null;
    }
}
