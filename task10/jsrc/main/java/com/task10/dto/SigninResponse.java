package com.task10.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SigninResponse {
    private String accessToken;
}
