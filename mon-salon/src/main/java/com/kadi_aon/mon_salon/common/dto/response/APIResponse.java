package com.kadi_aon.mon_salon.common.dto.response;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class APIResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private LocalDateTime date;

    public APIResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.date = LocalDateTime.now();
    }
}
