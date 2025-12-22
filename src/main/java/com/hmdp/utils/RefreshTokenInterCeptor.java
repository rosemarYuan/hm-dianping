package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;

/**
 * 一号拦截器，拦截所有路径。作用为更新token有效期
 *  --> 获取token
 *  --> 查询Redis中是否存在用户
 *  --> 保存至ThreadLocal
 *  --> 更新有效期
 *  --> 放行
 */
public class RefreshTokenInterCeptor implements HandlerInterceptor{
    // 注入参数。这里大类没有加“@Configutation”，因此不能直接@依赖注入，要自己写进去
    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterCeptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //  1. 从请求头中token获取Redis中的用户,为空时不拦截（直接返回true）
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
            return true;
        }

        //  2. 基于token获得redis中的用户
        String key = LOGIN_USER_KEY + token;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);
        //  3. 判断用户是否存在
        if (userMap.isEmpty()) {
            return true;
        }
        //  4. 将查询到的数据转为返回的安全DTO
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);

        //  5. 存在，保存信息至线程中
        UserHolder.saveUser(userDTO);
        //  6. 刷新token有效期
        stringRedisTemplate.expire(key, LOGIN_USER_TTL, TimeUnit.MINUTES);
        //  7. 放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 移除用户
        UserHolder.removeUser();
    }

}
