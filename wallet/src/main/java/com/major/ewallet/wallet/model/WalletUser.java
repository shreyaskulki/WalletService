package com.major.ewallet.wallet.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.major.ewallet.wallet.entity.Wallet;
import lombok.Data;

@Data
@JsonIgnoreProperties(value = {"contactNumber","dob","createdAt","updatedAt"})
public class WalletUser {

    private Long id;
    private String name;
    private String email;

    public Wallet toWallet(){
        return Wallet.builder()
                .userId(id)
                .balance((double) 0)
                .build();
    }
}
