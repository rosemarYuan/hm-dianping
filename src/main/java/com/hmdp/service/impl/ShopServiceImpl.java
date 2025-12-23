package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Override
    public void queryShopById(Long id) {
        //  TODO 数据缓存实现
        //   1 Redis查询商铺缓存
        //   2 判断缓存是否命中
        //   (否)  3 未命中时，使用MBP根据id查询数据库
        //         4 判断商铺在数据库是否存在
        //              (否) 返回404
        //              (是) 商铺数据写入Redis
        //   5 返回商铺信息

    }
}
