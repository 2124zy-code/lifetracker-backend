package com.lifetracker.modules.auth.controller;

import com.lifetracker.common.Result;
import com.lifetracker.modules.auth.dto.LoginDTO;
import com.lifetracker.modules.auth.dto.RegisterDTO;
import com.lifetracker.modules.auth.service.AuthService;
import com.lifetracker.modules.auth.vo.LoginVO;
import com.lifetracker.modules.auth.vo.UserInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterDTO registerDTO) {
        log.info("[AuthModule] 接收到用户注册请求: username={}", registerDTO.getUsername());
        LoginVO result = authService.register(registerDTO);
        return Result.success(result, "注册成功并已自动登录");
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        log.info("[AuthModule] 接收到用户登录请求: username={}", loginDTO.getUsername());
        LoginVO result = authService.login(loginDTO);
        return Result.success(result, "登录成功");
    }

    @GetMapping("/me")
    public Result<UserInfoVO> getCurrentUserInfo() {
        UserInfoVO result = authService.getCurrentUserInfo();
        return Result.success(result);
    }
}
