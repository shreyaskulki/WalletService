package com.major.ewallet.wallet.listener;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.major.ewallet.wallet.entity.Wallet;
import com.major.ewallet.wallet.model.WalletUser;
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
public class UserCreatedListener {

    private static final String USER_CREATED = "USER_CREATED";

    private static final String NEW_WALLET_CREATED = "NEW_WALLET_CREATED";

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    WalletService walletService;

    @Autowired
    KafkaTemplate <String, String> kafkaTemplate;

    @SneakyThrows
    @KafkaListener(topics = {USER_CREATED}, groupId = "wallet_group")
    public void receiveMessage(@Payload String message){
        /*
        * Read from User Created Kafka Queue > Create a new Waller > Persist in Db > Send message to Wallet created Kafka Queue*/
        log.info("*** INSIDE USER CREATED LISTENER RECEIVE MESSAGE IN WALLER SERVICE");
        objectMapper.registerModule(new JavaTimeModule());
        WalletUser walletUser = objectMapper.readValue(message,WalletUser.class);

        Wallet wallet = walletService.createANewWallet(walletUser);

        String sendMessage = objectMapper.writeValueAsString(wallet);
        log.info("*** SENDING MESSAGE TO NEW_WALLET_CREATED ***");
        addCallBack(sendMessage,kafkaTemplate.send(NEW_WALLET_CREATED,sendMessage));
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
