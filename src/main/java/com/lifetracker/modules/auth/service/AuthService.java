package com.lifetracker.modules.auth.service;

import com.lifetracker.modules.auth.dto.LoginDTO;
import com.lifetracker.modules.auth.dto.RegisterDTO;
import com.lifetracker.modules.auth.vo.LoginVO;
import com.lifetracker.modules.auth.vo.UserInfoVO;

public interface AuthService {

    LoginVO register(RegisterDTO registerDTO);

    LoginVO login(LoginDTO loginDTO);

    UserInfoVO getCurrentUserInfo();
}
