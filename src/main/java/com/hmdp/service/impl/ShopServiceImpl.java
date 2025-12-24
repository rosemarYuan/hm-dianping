package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

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

    @Override
    public Result queryShopById(Long id) {
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
}
