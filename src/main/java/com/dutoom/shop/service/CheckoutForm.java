package com.dutoom.shop.service;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CheckoutForm {
    @NotBlank @Size(max = 50) private String ordererName;
    @NotBlank @Pattern(regexp = "[0-9\\-]{9,15}", message = "연락처 형식을 확인해 주세요.") private String ordererPhone;
    @NotBlank @Email @Size(max = 120) private String ordererEmail;
    @NotBlank @Size(max = 50) private String recipientName;
    @NotBlank @Pattern(regexp = "[0-9\\-]{9,15}", message = "연락처 형식을 확인해 주세요.") private String recipientPhone;
    @NotBlank @Size(max = 10) private String zipcode;
    @NotBlank @Size(max = 200) private String address1;
    @Size(max = 200) private String address2;
    @Size(max = 300) private String memo;
    @Size(max = 500) private String giftMessage;
    @Size(max = 50) private String guestPassword;
    private boolean agree;
}
