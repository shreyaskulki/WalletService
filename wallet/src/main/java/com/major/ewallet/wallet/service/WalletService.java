package com.major.ewallet.wallet.service;

import com.major.ewallet.wallet.entity.Wallet;
import com.major.ewallet.wallet.model.PendingTransactionRequest;
import com.major.ewallet.wallet.model.WalletUser;
import com.major.ewallet.wallet.repository.WalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class WalletService {

    @Autowired
    WalletRepository walletRepository;

    @Value("${pocketbook.user.systemId}")
    Long systemId;

    public Wallet createANewWallet(WalletUser user){
        Optional<Wallet> byUserId = walletRepository.findByUserId(user.getId());

        if(byUserId.isPresent()){
            log.info("***** WALLLET ALREADY PRESENT ***");
            return null;
        }

        return saveOrUpdate(user.toWallet());
    }

    private Wallet saveOrUpdate(Wallet wallet){
        return walletRepository.save(wallet);
    }

    public void topUpWallet(PendingTransactionRequest pendingTransactionRequest) {
        if (pendingTransactionRequest.getSenderId()==systemId){
            Optional<Wallet> newWalletTopUp = walletRepository.findByUserId(pendingTransactionRequest.getReceiverId());
            if(newWalletTopUp.isEmpty()){
                log.info("*** WALLET NOT FOUND ***");
                throw new RuntimeException();
            }

            Wallet reciever = newWalletTopUp.get();
            reciever.setBalance(Double.sum(reciever.getBalance(),pendingTransactionRequest.getAmount()));
            saveOrUpdate(reciever);
        }

        if (!Objects.equals(pendingTransactionRequest.getSenderId(),systemId)){
            Optional<Wallet> senderWallet = walletRepository.findByUserId(pendingTransactionRequest.getSenderId());
            if (senderWallet.isEmpty()){
                throw new RuntimeException();
            }

            if (senderWallet.get().getBalance()< pendingTransactionRequest.getAmount()){
                throw new RuntimeException();
            }

            Wallet sender = senderWallet.get();
            sender.setBalance(Double.sum(sender.getBalance(),-1* pendingTransactionRequest.getAmount()));
            saveOrUpdate(sender);
        }

        if (!Objects.equals(pendingTransactionRequest.getReceiverId(), systemId)){
            Optional<Wallet> receiverWallet = walletRepository.findByUserId(pendingTransactionRequest.getReceiverId());
            if (receiverWallet.isEmpty()){
                throw new RuntimeException();
            }


            Wallet sender = receiverWallet.get();
            sender.setBalance(Double.sum(sender.getBalance(), pendingTransactionRequest.getAmount()));
            saveOrUpdate(sender);
        }
    }
}
