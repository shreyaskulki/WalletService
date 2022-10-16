package com.major.ewallet.wallet.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(value = {"createdAt","updatedAt","status"})
public class PendingTransactionRequest {

    Long senderId;
    Long receiverId;
    Double amount;
    Long id;
}
