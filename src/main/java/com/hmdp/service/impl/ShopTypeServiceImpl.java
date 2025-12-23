package com.hmdp.service.impl;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TTL;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    private final StringRedisTemplate stringRedisTemplate;

    public ShopTypeServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public Result quereTypeList() {
        // 1 缓存中查询是否存在？
        String key = CACHE_SHOP_KEY + "type:list" ;
        String shopType = stringRedisTemplate.opsForValue().get(key);
        // 2 存在时，直接返回
        if (StrUtil.isNotBlank(shopType)) {
            List<ShopType> typeList = JSONUtil.toList(shopType, ShopType.class);
            return Result.ok(typeList);
        }
        // 3 不存在时，搜索MySQL数据库
        List<ShopType> typeList = query().orderByAsc("sort").list();
        if (Objects.isNull(typeList)) {
            return Result.fail("404");
        }

        // 4 将数据库中结果保存到Redis
        String shopTypeStr = JSONUtil.toJsonStr(typeList);
        stringRedisTemplate.opsForValue().set(key, shopTypeStr,CACHE_SHOP_TTL, TimeUnit.MINUTES);
        // 5 返回结果
        return Result.ok(typeList);
    }
}
