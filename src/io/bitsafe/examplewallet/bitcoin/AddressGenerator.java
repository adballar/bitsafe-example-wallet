/* This file is licensed as described by the file LICENCE. */

package io.bitsafe.examplewallet.bitcoin;

import com.google.bitcoin.core.ECKey;

/**
 *
 * @author Chris Chua
 */
public interface AddressGenerator {
    public abstract ECKey generateNewKey();
}
