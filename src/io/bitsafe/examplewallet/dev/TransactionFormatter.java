/* This file is licensed as described by the file LICENCE. */

package io.bitsafe.examplewallet.dev;

import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.crypto.TransactionSignature;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.script.ScriptOpCodes;
import io.bitsafe.examplewallet.bitcoin.BitcoinJInterface;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

/**
 *
 * @author Chris Chua
 */
public class TransactionFormatter {
    private static void writeSupportingTransaction(TransactionInput ti, OutputStream os) throws IOException {
        // Write BitSafe supporting transaction header.
        os.write(0x01); // is_ref = 1 (is supporting transaction)
        Utils.uint32ToByteStreamLE(ti.getOutpoint().getIndex(), os);
        // Write serialised supporting transaction.
        ti.getOutpoint().getConnectedOutput().getParentTransaction().bitcoinSerialize(os);
    }

    /** Get old (Bitcoin serialisation) transaction data for BitSafe
     * SignTransaction message. This only signs one input, since the
     * SignTransaction message only deals with one signature.
     * @param t Transaction to sign
     * @param inputIndex Which input to sign (0 = first, 1 = second etc.)
     * @return
     * @throws IOException 
     * @warning This will mangle the input scripts of t
     */
    public static byte[] getSignTransactionStream(Transaction t, int inputIndex) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Iterator<TransactionInput> iterator = t.getInputs().iterator();
        while (iterator.hasNext()) {
            writeSupportingTransaction(iterator.next(), os);
        }
        os.write(0x00); // is_ref = 0 (is spending transaction)
        // Go through OP_CHECKSIG with hashtype == SIGHASH_ALL procedure.
        // Blank out all scriptSigs.
        for (int i = 0; i < t.getInputs().size(); i++) {
            Script emptyScript = new Script(new byte[0]);
            t.getInputs().get(i).setScriptSig(emptyScript);
        }
        // Insert scriptPubKey from connected output into scriptSig.
        TransactionInput input = t.getInputs().get(inputIndex);
        byte[] connectedScript = input.getOutpoint().getConnectedOutput().getScriptBytes();
        connectedScript = Script.removeAllInstancesOfOp(connectedScript, ScriptOpCodes.OP_CODESEPARATOR);
        input.setScriptSig(new Script(connectedScript));
        t.bitcoinSerialize(os);
        // Append hashType.
        int hashType = TransactionSignature.calcSigHashValue(Transaction.SigHash.ALL, false);
        Utils.uint32ToByteStreamLE(hashType & 0x000000ff, os);

        return os.toByteArray();
    }
}
