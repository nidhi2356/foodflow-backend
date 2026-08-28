package com.foodflow.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "razorpay")
@Getter
@Setter
public class RazorpayConfig {

    private String keyId = "rzp_test_mock_key";
    private String keySecret = "mock_secret_key";
}
