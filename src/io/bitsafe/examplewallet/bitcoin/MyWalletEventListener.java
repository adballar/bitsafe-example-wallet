/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.bitsafe.examplewallet.bitcoin;

import com.google.bitcoin.core.AbstractWalletEventListener;
import com.google.bitcoin.core.Wallet;
import java.math.BigInteger;

/**
 *
 * @author z
 */
public class MyWalletEventListener extends AbstractWalletEventListener {
    private volatile WalletBalanceObserver observer;

    @Override
    public void onWalletChanged(Wallet wallet) {
        BigInteger c = wallet.getBalance(Wallet.BalanceType.AVAILABLE);
        BigInteger u = wallet.getBalance(Wallet.BalanceType.ESTIMATED);
        u = u.subtract(c);
        if (observer != null) {
            observer.balanceChanged(c, u);
        }
    }

    public void setObserver(WalletBalanceObserver newObserver) {
        observer = newObserver;
    }
}
