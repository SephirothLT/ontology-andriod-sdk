/*
 * Copyright (C) 2018 The ontology Authors
 * This file is part of The ontology library.
 *
 *  The ontology is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  The ontology is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with The ontology.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.github.ontio.smartcontract.nativevm;

import com.github.ontio.OntSdk;
import com.github.ontio.account.Account;
import com.github.ontio.common.Address;
import com.github.ontio.common.ErrorCode;
import com.github.ontio.core.transaction.Transaction;
import com.github.ontio.core.transaction.Attribute;
import com.github.ontio.core.transaction.AttributeUsage;
import com.github.ontio.core.VmType;
import com.github.ontio.core.asset.*;
import com.github.ontio.core.payload.Vote;
import com.github.ontio.sdk.exception.SDKException;
import com.github.ontio.sdk.info.AccountInfo;
//import org.spongycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.UUID;


/**
 *
 */
public class OntAssetTx {
    private OntSdk sdk;
    private final String ontContract = "ff00000000000000000000000000000000000001";
    private final String ongContract = "ff00000000000000000000000000000000000002";
    private int precision = 1;

    public OntAssetTx(OntSdk sdk) {
        this.sdk = sdk;
    }

    /**
     * @param assetName
     * @param sendAddr
     * @param password
     * @param amount
     * @param recvAddr
     * @return
     * @throws Exception
     */
    public String sendTransfer(String assetName, String sendAddr, String password, String recvAddr, long amount,long gas) throws Exception {
        Transaction tx = makeTransfer(assetName, sendAddr, password, recvAddr, amount,gas);
        sdk.signTx(tx, sendAddr, password);
        boolean b = sdk.getConnectMgr().sendRawTransaction(tx.toHexString());
        if (b) {
            return tx.hash().toString();
        }
        return null;
    }

    public Transaction makeTransfer(String assetName, String sendAddr, String password, String recvAddr, long amount,long gas) throws Exception {
        String contractAddr = null;
        if (assetName.equals("ong")) {
            contractAddr = ongContract;
        } else if (assetName.equals("ont")) {
            contractAddr = ontContract;
        } else {
            throw new SDKException(ErrorCode.AssetNameError);
        }
        amount = amount * precision;
        AccountInfo sender = sdk.getWalletMgr().getAccountInfo(sendAddr, password);
        State state = new State(Address.addressFromPubKey(sender.pubkey), Address.decodeBase58(recvAddr), amount);
        Transfers transfers = new Transfers(new State[]{state});
        Transaction tx = sdk.vm().makeInvokeCodeTransaction(contractAddr, "transfer", transfers.toArray(), VmType.Native.value(), sendAddr,gas);
        return tx;
    }

    /**
     * @param assetName
     * @param sendAddr
     * @param password
     * @param amount
     * @param recvAddr
     * @return
     * @throws Exception
     */
    public String sendTransferToMany(String assetName, String sendAddr, String password, String[] recvAddr, long[] amount,long gas) throws Exception {
        String contractAddr = null;
        if (assetName.equals("ong")) {
            contractAddr = ongContract;
        } else if (assetName.equals("ont")) {
            contractAddr = ontContract;
        } else {
            throw new SDKException(ErrorCode.AssetNameError);
        }

        AccountInfo sender = sdk.getWalletMgr().getAccountInfo(sendAddr, password);
        State[] states = new State[recvAddr.length];
        if (amount.length != recvAddr.length) {
            throw new Exception("");
        }
        for (int i = 0; i < recvAddr.length; i++) {
            amount[i] = amount[i] * precision;
            states[i] = new State(Address.addressFromPubKey(sender.pubkey), Address.decodeBase58(recvAddr[i]), amount[i]);
        }
        Transfers transfers = new Transfers(states);
        Transaction tx = sdk.vm().makeInvokeCodeTransaction(contractAddr, "transfer", transfers.toArray(), VmType.Native.value(), sendAddr,gas);
        sdk.signTx(tx, new Account[][]{{sdk.getWalletMgr().getAccount(sendAddr, password)}});
        boolean b = sdk.getConnectMgr().sendRawTransaction(tx.toHexString());
        if (b) {
            return tx.hash().toString();
        }
        return null;
    }

    /**
     * @param assetName
     * @param sendAddr
     * @param password
     * @param amount
     * @param recvAddr
     * @return
     * @throws Exception
     */
    public String sendTransferFromMany(String assetName, String[] sendAddr, String[] password, String recvAddr, long[] amount,long gas) throws Exception {
        String contractAddr = null;
        if (assetName.equals("ong")) {
            contractAddr = ongContract;
        } else if (assetName.equals("ont")) {
            contractAddr = ontContract;
        } else {
            throw new SDKException(ErrorCode.AssetNameError);
        }
        if (sendAddr == null || sendAddr.length != password.length) {
            throw new Exception("");
        }
        State[] states = new State[sendAddr.length];
        for (int i = 0; i < sendAddr.length; i++) {
            AccountInfo sender = sdk.getWalletMgr().getAccountInfo(sendAddr[i], password[i]);
            amount[i] = amount[i] * precision;
            states[i] = new State(Address.addressFromPubKey(sender.pubkey), Address.decodeBase58(recvAddr), amount[i]);
        }

        Transfers transfers = new Transfers(states);
        Transaction tx = sdk.vm().makeInvokeCodeTransaction(contractAddr, "transfer", transfers.toArray(), VmType.Native.value(), sendAddr[0],gas);
        Account[][] acct = new Account[sendAddr.length][];
        for (int i = 0; i < sendAddr.length; i++) {
            acct[i] = new Account[]{sdk.getWalletMgr().getAccount(sendAddr[i], password[i])};
        }

        sdk.signTx(tx, acct);
        boolean b = sdk.getConnectMgr().sendRawTransaction(tx.toHexString());
        if (b) {
            return tx.hash().toString();
        }
        return null;
    }

    public String sendOngTransferFrom(String sendAddr, String password, String to, long amount,long gas) throws Exception {
        if (amount <= 0){
            throw new SDKException(ErrorCode.NullKeyOrValue);
        }
        AccountInfo sender = sdk.getWalletMgr().getAccountInfo(sendAddr, password);
        TransferFrom transferFrom = new TransferFrom(Address.addressFromPubKey(sender.pubkey), Address.parse(ontContract), Address.decodeBase58(to), amount);
        Transaction tx = sdk.vm().makeInvokeCodeTransaction(ontContract, "transferFrom", transferFrom.toArray(), VmType.Native.value(), sendAddr,gas);
        sdk.signTx(tx, sendAddr, password);
        boolean b = sdk.getConnectMgr().sendRawTransaction(tx.toHexString());
        if (b) {
            return tx.hash().toString();
        }
        return null;
    }
//    private String voteTx(String addr, String password, ECPoint... pubKeys) throws Exception {
//        Vote tx = makeVoteTx(sdk.getWalletMgr().getAccount(addr, password,sdk.keyType,sdk.curveParaSpec).getAddressU160(), pubKeys);
//        sdk.signTx(tx, new Account[][]{{sdk.getWalletMgr().getAccount(addr, password,sdk.keyType,sdk.curveParaSpec)}});
//        boolean b = sdk.getConnectMgr().sendRawTransaction(tx.toHexString());
//        if (b) {
//            return tx.hash().toString();
//        }
//        return null;
//    }
//
//    private Vote makeVoteTx(Address account, ECPoint... pubKeys) {
//        Vote tx = new Vote();
//        tx.pubKeys = pubKeys;
//        tx.account = account;
//
//        tx.attributes = new Attribute[1];
//        tx.attributes[0] = new Attribute();
//        tx.attributes[0].usage = AttributeUsage.Description;
//        tx.attributes[0].data = UUID.randomUUID().toString().getBytes();
//        return tx;
//    }
}