package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.UserInfo;
import com.hmdp.mapper.UserInfoMapper;
import com.hmdp.service.IUserInfoService;
import com.hmdp.utils.MailClient;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-24
 */
@Service
@Slf4j
public class  UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

    @Resource // 也可以Autowired，Re是java自带、Au是Spring写的
    private MailClient mailClient;

    @Override
    public Result sentCode(String phone, HttpSession session) {
        // 1. 验证提交的手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号码格式不对，请重新输入");
        }

        // 2. 生成验证码
        String code = RandomUtil.randomNumbers(6);
        log.debug("邮箱验证码生成成功，验证码：{}", code);


        // 3. 保存验证码到Session
        session.setAttribute("code", code);

        // 4. 异步发送邮件 (主线程直接走下去，不等待邮件发送完成)
        String email = "1973198783@qq.com";
        mailClient.sendMailAsync(email, "【黑马点评】登录验证码", "您的验证码是：" + code + "，有效期2分钟。");

        // 5. 结束
        return Result.ok("发送验证码成功");
    }
}
