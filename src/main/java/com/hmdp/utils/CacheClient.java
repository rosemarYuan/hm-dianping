package com.hmdp.utils;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.hmdp.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;

@Slf4j
@Configuration
public class CacheClient {
    // 利用构造函数注入stringRedisTemplate类
    private final StringRedisTemplate stringRedisTemplate;

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(value),time,unit);
    }

    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        // 写入Redis
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(redisData));
    }


    public <R,ID> R queryWithPassThrough(
            String keyPrefix, ID id, Class<R> type,
            Function<ID,R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        //   1 Redis查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        //   2 判断缓存是否命中，命中直接返回
        if (StringUtils.isNotBlank(json)) {
            return JSONUtil.toBean(json, type);
        }

        // 判断命中是否为空值
        if(json != null)  {
            return null;
        }

        //   3 未命中时，使用函数编程调用外在方法
        R r = dbFallback.apply(id);
        //   4 判断商铺在数据库是否存在，加空值过滤
        if (r == null) {
            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }

        //   5 商铺数据写入Redis，并设置过去时间
        this.set(key, r, time, unit);
        log.debug("已经完成向{}缓存数据",key);
        //   6 返回商铺信息
        return r;
    }


}
