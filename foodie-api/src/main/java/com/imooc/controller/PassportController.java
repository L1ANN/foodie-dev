package com.imooc.controller;

import com.imooc.exception.BizException;
import com.imooc.pojo.dto.param.UserDTO;
import com.imooc.pojo.entity.Users;
import com.imooc.service.UserService;
import com.imooc.utils.CookieUtils;
import com.imooc.utils.JSONResult;
import com.imooc.utils.JsonUtils;
import com.imooc.utils.MD5Utils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Api(value = "注册登录", tags = {"用于注册登录的相关接口"})
@RestController
@RequestMapping("passport")
public class PassportController {

    @Autowired
    private UserService userService;

    @ApiOperation(value = "用户名是否存在", notes = "用户名是否存在", httpMethod = "GET")
    @GetMapping("usernameIsExist")
    public JSONResult usernameIsExist(@RequestParam String username) {
        if (StringUtils.isBlank(username)) {
            return JSONResult.errorMsg("用户名不能为空");
        }

        boolean isExist = userService.queryUsernameIsExist(username);
        if (isExist) {
            return JSONResult.errorMsg("用户名已经存在");
        }

        return JSONResult.ok();
    }

    @ApiOperation(value = "用户注册", notes = "用户注册", httpMethod = "POST")
    @PostMapping("regist")
    public JSONResult regist(@RequestBody UserDTO registerUser,
                             HttpServletRequest request,
                             HttpServletResponse response) {
        String username = registerUser.getUsername();
        String password = registerUser.getPassword();
        String confirmPwd = registerUser.getConfirmPassword();

        //1.判断用户名密码不能为空
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password) || StringUtils.isBlank(confirmPwd)) {
            return JSONResult.errorMsg("用户名或密码不能为空");
        }
        //2.判断用户名是否存在
        boolean isExist = userService.queryUsernameIsExist(username);
        if (isExist) {
            return JSONResult.errorMsg("用户名已经存在");
        }
        //3.密码长度不能小于6
        if (password.length() < 6) {
            return JSONResult.errorMsg("密码长度不能小于6");
        }
        //4.判断两次密码是否一致
        if (!password.equals(confirmPwd)) {
            return JSONResult.errorMsg("两次密码输入不一致");
        }

        Users userResult = userService.createUser(registerUser);

        //3.将一些关键属性设置为null
        this.setNullProperty(userResult);
        //4.将用户信息添加到cookie中
        CookieUtils.setCookie(request, response, "user", JsonUtils.objectToJson(userResult), true);
        return JSONResult.ok();
    }

    @ApiOperation(value = "用户登录", notes = "用户登录", httpMethod = "POST")
    @PostMapping("login")
    public JSONResult login(@RequestBody UserDTO userDTO,
                            HttpServletRequest request,
                            HttpServletResponse response) {

        String username = userDTO.getUsername();
        String password = userDTO.getPassword();
        //1.判断用户名密码不能为空
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            return JSONResult.errorMsg("用户名或密码不能为空");
        }

        //2.用户名和密码校验
        Users userResult = null;
        try {
            userResult = userService.queryUserForLogin(username, MD5Utils.getMD5Str(password));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (userResult == null) {
            return JSONResult.errorMsg("用户名或密码不正确");
        }
        //3.将一些关键属性设置为null
        this.setNullProperty(userResult);
        //4.将用户信息添加到cookie中
        CookieUtils.setCookie(request, response, "user", JsonUtils.objectToJson(userResult), true);
        return JSONResult.ok(userResult);
    }

    @ApiOperation(value = "用户退出登录", notes = "用户退出登录", httpMethod = "POST")
    @PostMapping("logout")
    public JSONResult logout(@RequestParam String userId,
                             HttpServletRequest request,
                             HttpServletResponse response) {
        if (StringUtils.isBlank(userId)) {
            throw new BizException("用户退出登录时userId不能为空");
        }

        CookieUtils.deleteCookie(request,response,"user");

        //TODO 清空购物车
        //TODO 清空session
        return JSONResult.ok();
    }

    private void setNullProperty(Users userResult) {
        userResult.setPassword(null);
        userResult.setMobile(null);
        userResult.setEmail(null);
        userResult.setCreatedTime(null);
        userResult.setUpdatedTime(null);
        userResult.setBirthday(null);
    }

}
