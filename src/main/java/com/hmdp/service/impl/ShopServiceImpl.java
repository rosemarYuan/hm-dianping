package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static java.lang.Thread.sleep;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    private final StringRedisTemplate stringRedisTemplate;

    public ShopServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    //  1. 提取缓存穿透代码为新函数queryWithPassThrough
    //  2.用互斥锁解决缓存击穿，在上面基础上修改为queryWithMutex
    @Override
    public Result queryShopById(Long id) {
        return queryWithMutex(id);
    }

    @NotNull
    private Result queryWithMutex(Long id) {
        String key = CACHE_SHOP_KEY + id;
        //   1 Redis查询商铺缓存
        String shopJsonCache = stringRedisTemplate.opsForValue().get(key);
        //   2 判断缓存是否命中，命中直接返回
        if (StringUtils.isNotBlank(shopJsonCache)) {
            Shop shop = JSONUtil.toBean(shopJsonCache, Shop.class);
            return Result.ok(shop);
        }
        // 判断命中是否为空值
        if(shopJsonCache != null)  {
            return Result.ok("店铺不存在");
        }
        //   ---  实现缓存重建  ---
        //   3 未命中时，先获取互斥锁,未能获得则等待10s再重来
        boolean lock = tryLock(String.valueOf(id));
        if (!lock) {
            try {
                sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return queryWithMutex(id);
        }
        try {
            //   获得时候，先再次检查缓存是否存在。
            //   1 Redis查询商铺缓存
            shopJsonCache = stringRedisTemplate.opsForValue().get(key);
            //   2 判断缓存是否命中，命中直接返回
            if (StringUtils.isNotBlank(shopJsonCache)) {
                return Result.ok(JSONUtil.toBean(shopJsonCache, Shop.class));
            }
            // 判断命中是否为空值
            if(shopJsonCache != null)  {
                return Result.ok("店铺不存在");
            }

            //   获得时，使用MBP根据id查询数据库
            Shop shop = getById(id);
            //   4 判断商铺在数据库是否存在，加空值过滤
            if (shop == null) {
                stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
                return Result.fail("店铺不存在");
            }
            //   5 商铺数据写入Redis
            String jsonShop = JSONUtil.toJsonStr(shop);
            stringRedisTemplate.opsForValue().set(key, jsonShop,CACHE_SHOP_TTL, TimeUnit.MINUTES);
            log.debug("已经完成向{}缓存数据：{}",key,jsonShop);
            //   6 返回商铺
            return Result.ok(shop);
        }catch (Exception e) {
            throw new RuntimeException(e);
        }finally {
            //   5.2 释放锁：
            unlock(String.valueOf(id));
        }
    }

    @NotNull
    private Result queryWithPassThrough(Long id) {
        String key = CACHE_SHOP_KEY + id;
        //   1 Redis查询商铺缓存
        String shopJsonCache = stringRedisTemplate.opsForValue().get(key);
        //   2 判断缓存是否命中，命中直接返回
        if (StringUtils.isNotBlank(shopJsonCache)) {
            Shop shop = JSONUtil.toBean(shopJsonCache, Shop.class);
            return Result.ok(shop);
        }

        // 判断命中是否为空值
        if(shopJsonCache != null)  {
            return Result.ok("店铺不存在");
        }

        //   3 未命中时，使用MBP根据id查询数据库
        Shop shop = getById(id);
        //   4 判断商铺在数据库是否存在，加空值过滤
        if (shop == null) {
            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
            return Result.fail("店铺不存在");
        }

        //   5 商铺数据写入Redis
        String jsonShop = JSONUtil.toJsonStr(shop);
        stringRedisTemplate.opsForValue().set(key, jsonShop,CACHE_SHOP_TTL, TimeUnit.MINUTES);
        log.debug("已经完成向{}缓存数据：{}",key,jsonShop);
        //   6 返回商铺信息
        return Result.ok(shop);
    }

    @Override
    @Transactional()
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("404");
        }
        // 1. 更新数据库
        updateById(shop);
        // 2. 删除缓存
        String key = CACHE_SHOP_KEY + shop.getId();
        stringRedisTemplate.delete(key);

        // 3. 返回结果
        return Result.ok();
    }

    private boolean tryLock(String key) {
        // 获得锁,返回结果。
        String lockKey = LOCK_SHOP_KEY + key;
        Boolean ifAbsent = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(ifAbsent);
    }

    private void unlock(String key) {
        // 删除锁
        String lockKey = LOCK_SHOP_KEY + key;
        stringRedisTemplate.delete(lockKey);
    }
}
