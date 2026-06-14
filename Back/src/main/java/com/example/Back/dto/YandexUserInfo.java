package com.example.Back.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
/**
 * Yandex OAuth user-info response: id, default e-mail, login and display name.
 */


public record YandexUserInfo(
        @JsonProperty("id")            String id,
        @JsonProperty("login")         String login,
        @JsonProperty("default_email") String defaultEmail,
        @JsonProperty("real_name")     String realName,
        @JsonProperty("display_name")  String displayName
) {}
