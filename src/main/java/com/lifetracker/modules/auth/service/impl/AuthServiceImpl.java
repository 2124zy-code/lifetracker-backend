package com.lifetracker.modules.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lifetracker.common.BusinessException;
import com.lifetracker.common.ErrorCodeEnum;
import com.lifetracker.common.UserContext;
import com.lifetracker.modules.auth.dto.LoginDTO;
import com.lifetracker.modules.auth.dto.RegisterDTO;
import com.lifetracker.modules.auth.entity.SysUser;
import com.lifetracker.modules.auth.mapper.SysUserMapper;
import com.lifetracker.modules.auth.service.AuthService;
import com.lifetracker.modules.auth.vo.LoginVO;
import com.lifetracker.modules.auth.vo.UserInfoVO;
import com.lifetracker.modules.habit.entity.UserHabit;
import com.lifetracker.modules.habit.mapper.UserHabitMapper;
import com.lifetracker.utils.JwtUtils;
import com.lifetracker.utils.PasswordUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper userMapper;
    private final UserHabitMapper habitMapper;
    private final JwtUtils jwtUtils;

    public AuthServiceImpl(SysUserMapper userMapper, UserHabitMapper habitMapper, JwtUtils jwtUtils) {
        this.userMapper = userMapper;
        this.habitMapper = habitMapper;
        this.jwtUtils = jwtUtils;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO register(RegisterDTO registerDTO) {
        String username = registerDTO.getUsername().trim();
        log.info("[AuthModule] 开始注册用户: username={}", username);

        // 校验用户名是否已存在
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (count > 0) {
            log.warn("[AuthModule] 注册失败，用户名已存在: username={}", username);
            throw new BusinessException(ErrorCodeEnum.USER_ALREADY_EXISTS);
        }

        String encodedPassword = PasswordUtils.encode(registerDTO.getPassword());
        String nickname = StringUtils.hasText(registerDTO.getNickname()) ? registerDTO.getNickname().trim() : username;
        String avatar = StringUtils.hasText(registerDTO.getAvatar()) ? registerDTO.getAvatar().trim() : 
                "https://api.dicebear.com/7.x/bottts/svg?seed=" + username;

        SysUser user = SysUser.builder()
                .username(username)
                .password(encodedPassword)
                .nickname(nickname)
                .avatar(avatar)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userMapper.insert(user);
        log.info("[AuthModule] 用户注册入库成功: userId={}, username={}", user.getId(), username);

        // 自动初始化 4 个自律习惯
        initDefaultHabits(user.getId());

        // 生成 JWT Token
        String token = jwtUtils.generateToken(user.getId(), user.getUsername());

        return LoginVO.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .build();
    }

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        String username = loginDTO.getUsername().trim();
        log.info("[AuthModule] 用户尝试登录: username={}", username);

        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) {
            log.warn("[AuthModule] 登录失败，用户不存在: username={}", username);
            throw new BusinessException(ErrorCodeEnum.USER_NOT_EXIST);
        }

        if (!PasswordUtils.matches(loginDTO.getPassword(), user.getPassword())) {
            log.warn("[AuthModule] 登录失败，密码不匹配: username={}", username);
            throw new BusinessException(ErrorCodeEnum.PASSWORD_ERROR);
        }

        String token = jwtUtils.generateToken(user.getId(), user.getUsername());
        log.info("[AuthModule] 用户登录成功: userId={}, username={}", user.getId(), username);

        return LoginVO.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .build();
    }

    @Override
    public UserInfoVO getCurrentUserInfo() {
        Long userId = UserContext.getRequiredUserId();
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_EXIST);
        }

        return UserInfoVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private void initDefaultHabits(Long userId) {
        List<UserHabit> defaultHabits = Arrays.asList(
                UserHabit.builder().userId(userId).name("早起晨光唤醒 (06:30)").icon("🌅").color("#10B981").targetDays(7).isDeleted(0).createdAt(LocalDateTime.now()).build(),
                UserHabit.builder().userId(userId).name("深度专注工作 4h").icon("💻").color("#8B5CF6").targetDays(5).isDeleted(0).createdAt(LocalDateTime.now()).build(),
                UserHabit.builder().userId(userId).name("硬核健身/有氧 45m").icon("🏋️").color("#F59E0B").targetDays(4).isDeleted(0).createdAt(LocalDateTime.now()).build(),
                UserHabit.builder().userId(userId).name("睡前阅读与冥想 30m").icon("📖").color("#3B82F6").targetDays(7).isDeleted(0).createdAt(LocalDateTime.now()).build()
        );
        for (UserHabit habit : defaultHabits) {
            habitMapper.insert(habit);
        }
        log.info("[AuthModule] 已为新用户自动注入 4 个初始习惯模板: userId={}", userId);
    }
}
