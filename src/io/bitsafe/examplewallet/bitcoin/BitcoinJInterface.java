/* This file is licensed as described by the file LICENCE. */

package io.bitsafe.examplewallet.bitcoin;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.BlockChain;
import com.google.bitcoin.core.DownloadListener;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.InsufficientMoneyException;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.PeerEventListener;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.net.discovery.DnsDiscovery;
import com.google.bitcoin.params.TestNet3Params;
import com.google.bitcoin.store.BlockStoreException;
import com.google.bitcoin.store.SPVBlockStore;
import com.google.bitcoin.store.UnreadableWalletException;
import com.google.bitcoin.wallet.CoinSelector;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;

/** Lots of code has been copied from BitcoinJ's WalletAppKit.java
 *
 * @author Mike Hearn, Chris Chua
 */
public class BitcoinJInterface extends AbstractIdleService implements AddressGenerator {

    private final String filePrefix;
    private final NetworkParameters params;
    private volatile BlockChain vChain;
    private volatile SPVBlockStore vStore;
    private volatile Wallet vWallet;
    private volatile PeerGroup vPeerGroup;

    private final File directory;

    private PeerEventListener downloadListener;
    private final MyWalletEventListener walletListener;
    private final Executor runInUIThread;
    private volatile AddressGenerator addressGenerator;

    public BitcoinJInterface(String inDirectoryString, String inFilePrefix) {
        params = TestNet3Params.get();
        directory = new File(inDirectoryString);
        filePrefix = inFilePrefix;
        walletListener = new MyWalletEventListener();
        // Executor object to associate with wallet event listeners so that
        // they always run in the Swing UI thread.
        runInUIThread = new Executor() {
            @Override public void execute(Runnable runnable) {
                SwingUtilities.invokeLater(runnable);
            }
        };
        addressGenerator = this;
        startAndWait();
    }

    @Override
    public ECKey generateNewKey() {
        return new ECKey();
    }

    public String generateAddress() {
        ECKey key = addressGenerator.generateNewKey();
        if (key == null) {
            // TODO: handle this better
            return null;
        }
        vWallet.addKey(key);
        return key.toAddress(params).toString();
    }

    public void setObserver(WalletBalanceObserver newObserver) {
        walletListener.setObserver(newObserver);
    }

    public void setAddressGenerator(AddressGenerator newAddressGenerator) {
        addressGenerator = newAddressGenerator;
    }

    /** Create, sign, commit and broadcast a transaction which spends to
     * multiple recipients.
     * @param recipients Who to send to
     * @param allowUnconfirmedSpend Whether to allow spending unconfirmed outputs
     * @param signer External transaction signer (can be null)
     * @throws AddressFormatException 
     * @throws InsufficientMoneyException 
     * @throws InterruptedException 
     * @throws ExecutionException 
     */
    public void sendMulti(SendRecipient[] recipients, boolean allowUnconfirmedSpend, TransactionSigner signer)
            throws AddressFormatException, InsufficientMoneyException,
            InterruptedException, ExecutionException {
        // Create empty send request.
        Transaction tx = new Transaction(params);
        Wallet.SendRequest req = Wallet.SendRequest.forTx(tx);
        // Populate output list with recipients.
        Address addr;
        for (SendRecipient recipient : recipients) {
            addr = new Address(params, recipient.getAddress());
            tx.addOutput(recipient.getAmount(), addr);
        }
        // Always generate a new change address.
        req.changeAddress = addressGenerator.generateNewKey().toAddress(params);
        // Complete transaction by adding inputs, a change output and fees.
        CoinSelector oldSelector = vWallet.getCoinSelector();
        if (allowUnconfirmedSpend) {
            vWallet.allowSpendingUnconfirmedTransactions();
        }
        vWallet.completeTx(req);
        boolean discardTransaction = false;
        if (signer != null) {
            discardTransaction = !signer.signTransaction(req.tx, vWallet);
        }

        if (!discardTransaction) {
            vWallet.commitTx(req.tx);
            vWallet.setCoinSelector(oldSelector);
            // Broadcast the transaction.
            ListenableFuture<Transaction> future = vPeerGroup.broadcastTransaction(req.tx);
            future.get();
        }
    }

    private void detachWallet() {
        vWallet.removeEventListener(walletListener);
        vChain.removeWallet(vWallet);
        vPeerGroup.removeWallet(vWallet);
    }

    private void attachWallet(Wallet w) {
        vChain.addWallet(w);
        vPeerGroup.addWallet(w);
        w.addEventListener(walletListener, runInUIThread);
    }

    public File getWalletFile(int inWalletNumber) {
        return new File(directory, filePrefix + ".wallet" + inWalletNumber);
    }

    public void saveWallet(int inWalletNumber) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(getWalletFile(inWalletNumber))) {
            vWallet.saveToFileStream(fos);
        }
    }

    public void loadWallet(int inWalletNumber) throws UnreadableWalletException, IOException {
        try (FileInputStream fis = new FileInputStream(getWalletFile(inWalletNumber))) {
            detachWallet();
            vWallet = Wallet.loadFromFileStream(fis);
        }
        attachWallet(vWallet);
        walletListener.onWalletChanged(vWallet);
        vWallet.autosaveToFile(getWalletFile(inWalletNumber), 10, TimeUnit.SECONDS, null);
    }

    public void deleteWallet(int inWalletNumber) {
        getWalletFile(inWalletNumber).delete();
    }

    public void resetWallet() {
        detachWallet();
        vWallet = new Wallet(params);
        attachWallet(vWallet);
    }

    @Override
    protected void startUp() throws Exception {
        // Runs in a separate thread.
        if (!directory.exists()) {
            if (!directory.mkdir()) {
                throw new IOException("Could not create named directory.");
            }
        }
        FileInputStream walletStream = null;
        try {
            File chainFile = new File(directory, filePrefix + ".spvchain");
            vStore = new SPVBlockStore(params, chainFile);
            vChain = new BlockChain(params, vStore);
            vPeerGroup = new PeerGroup(params, vChain);
            vWallet = new Wallet(params);
            // Set up peer addresses or discovery first, so if wallet extensions try to broadcast a transaction
            // before we're actually connected the broadcast waits for an appropriate number of connections.
            vPeerGroup.addPeerDiscovery(new DnsDiscovery(params));
            attachWallet(vWallet);

            Futures.addCallback(vPeerGroup.start(), new FutureCallback<State>() {
                @Override
                public void onSuccess(State result) {
                    final PeerEventListener l = downloadListener == null ? new DownloadListener() : downloadListener;
                    vPeerGroup.startBlockChainDownload(l);
                }

                @Override
                public void onFailure(Throwable t) {
                    throw new RuntimeException(t);
                }
            });
        } catch (BlockStoreException e) {
            throw new IOException(e);
        } finally {
            if (walletStream != null) walletStream.close();
        }
    }

    @Override
    protected void shutDown() throws Exception {
        // Runs in a separate thread.
        try {
            vPeerGroup.stopAndWait();
            vStore.close();
            vPeerGroup = null;
            vWallet = null;
            vStore = null;
            vChain = null;
        } catch (BlockStoreException e) {
            throw new IOException(e);
        }

    }

    public Wallet wallet() {
        return vWallet;
    }

    public NetworkParameters params() {
        return params;
    }
}
