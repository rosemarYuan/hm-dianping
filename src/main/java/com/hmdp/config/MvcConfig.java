package com.hmdp.config;

import com.hmdp.utils.LoginInterCeptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 🔴 关键点2：注册你的 LoginInterceptor
        registry.addInterceptor(new LoginInterCeptor())
                // 1. 拦截哪些路径？ (/** 代表所有路径)
                .addPathPatterns("/**")
                // 2. 放行哪些路径？ (不需要登录也能看的)
                .excludePathPatterns(
                        "/shop/**",       // 店铺详情
                        "/voucher/**",    // 优惠券
                        "/shop-type/**",  // 店铺类型
                        "/upload/**",     // 上传资源
                        "/blog/hot",      // 热门博客
                        "/user/code",     // 发送验证码 (如果不放行，没人能拿到验证码)
                        "/user/login"     // 登录接口 (如果不放行，没人能登录)
                );
    }
}
