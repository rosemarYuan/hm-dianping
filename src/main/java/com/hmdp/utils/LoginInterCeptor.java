package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

/**
 * 二号拦截器，通过线程ThreadLocal判断用户是否存在？不存在时拦截关键操作路径。
 *  --> 查询ThreadLocal中的用户
 *  --> 不存在时，拦截
 *  --> 存在时，放行
 */
public class LoginInterCeptor implements HandlerInterceptor{

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //  TODO 1. 从线程中取出Redis用户，并判断是否需要拦截
        if (UserHolder.getUser() == null) {
            response.setStatus(UNAUTHORIZED.value());
            return false;
        }

        // 3. 放行
        return true;
    }


}
