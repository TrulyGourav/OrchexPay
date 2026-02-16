package com.orchexpay.walletledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UserWalletApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserWalletApplication.class, args);
    }
}
