package com.major.ewallet.wallet.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.major.ewallet.wallet.entity.Wallet;
import com.major.ewallet.wallet.model.PendingTransactionRequest;
import com.major.ewallet.wallet.service.WalletService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

@Component
@Slf4j
public class WalletTopUpListener {

    private static final String TOPUP_WALLET="TOPUP_WALLET";

    private static final String TOPUP_SUCCESS="TOPUP_SUCCESS";

    private static final String TOPUP_FAILURE="TOPUP_FAILURE";

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    KafkaTemplate<String,String> kafkaTemplate;

    @Autowired
    WalletService walletService;

    @SneakyThrows
    @KafkaListener(topics = {TOPUP_WALLET}, groupId = "wallet_group")
    public void receiveMessage(@Payload String message){
        /*
        * Read from TOP UP Wallet Kafka Qeueue > do the transaction in the wallet > Persist the status > Send success/failure to Kafka Queue*/
        PendingTransactionRequest pendingTransactionRequest = objectMapper.readValue(message,PendingTransactionRequest.class);
        String sendMessage = null;
        ListenableFuture<SendResult<String,String >> send = null;
        objectMapper.registerModule(new JavaTimeModule());
        try{
            walletService.topUpWallet(pendingTransactionRequest);
            sendMessage = objectMapper.writeValueAsString(pendingTransactionRequest);
            log.info("*** SENDING MESSAGE TO TOPUP_SUCCESS ***");
            send = kafkaTemplate.send(TOPUP_SUCCESS,sendMessage);
        }
        catch (Exception e){
            log.info("*** SENDING MESSAGE TO TOPUP_FAILURE ***");
            send = kafkaTemplate.send(TOPUP_FAILURE,sendMessage);
        }
        finally {
            if(send!=null){
                addCallBack(sendMessage,send);
            }
        }
    }

    public void addCallBack(String message, ListenableFuture<SendResult<String, String>> send){
        send.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {
            @Override
            public void onFailure(Throwable ex) {
                log.info("****** FAILURE TO SEND MESSAGE "+message);
            }

            @Override
            public void onSuccess(SendResult<String, String> result) {
                log.info("****** SUCCESS TO SEND MESSAGE "+message+" WITH PARTITION "+result.getRecordMetadata().partition()
                        +" WITH OFFSET "+result.getRecordMetadata().offset());
            }
        });
    }
}
