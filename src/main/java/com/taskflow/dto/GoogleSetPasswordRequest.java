package com.taskflow.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleSetPasswordRequest {
    private String password;
    private String confirmPassword;
}
