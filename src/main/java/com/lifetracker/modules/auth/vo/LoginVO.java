package com.lifetracker.modules.auth.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String token;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String username;

    private String nickname;

    private String avatar;
}
