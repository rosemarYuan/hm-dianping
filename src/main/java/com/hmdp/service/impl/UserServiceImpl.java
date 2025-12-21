package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.MailClient;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import com.hmdp.utils.MailClient;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

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

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // TODO 登陆时要保存手机号，手机号-验证码均对应 -> code改为手机号+code

        String phone = loginForm.getPhone();
        String code = loginForm.getCode();
        Object cacheCode = session.getAttribute("code");

        // 检查手机号、验证码是否匹配
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号码格式不对，请重新输入");
        }
        if (code== null || !cacheCode.toString().equals(code)) {
            return Result.fail("验证码不对，请重新输入");
        }

        // 根据手机号在数据库查询
        // 对应 SQL: select * from tb_user where phone = #{phone}
        User user = lambdaQuery().eq(User::getPhone, phone).one();

        // 不存在：new一个新的
        if (user == null) {
            user = createUserWithPhone(phone);
        }

        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        session.setAttribute("user", userDTO);

        return Result.ok();
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomNumbers(10));
        // INSERT INTO tb_user ( id, phone, nick_name, create_time, update_time )
        // VALUES ( ?, ?, ?, ?, ? )
        save(user);
        return user;
    }
}
