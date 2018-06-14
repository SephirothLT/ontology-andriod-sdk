package com.github.ontio.crypto;

import com.github.ontio.common.ErrorCode;
import com.github.ontio.sdk.exception.SDKException;


public enum KeyType {
    ECDSA(0x12),
    SM2(0x13),
    EDDSA(0x14);

    private int label;

    private KeyType(int b) {
        this.label = b;
    }

    // get the crypto.KeyType according to the input label
    public static KeyType fromLabel(byte label) throws Exception {
        for (KeyType k : KeyType.values()) {
            if (k.label == label) {
                return k;
            }
        }
        throw new SDKException(ErrorCode.ParamError);
    }

    public int getLabel() {
        return label;
    }
}
