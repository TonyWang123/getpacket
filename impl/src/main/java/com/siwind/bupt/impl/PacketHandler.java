/*
 * Copyright © 2016 siwind, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package com.siwind.bupt.impl;

import java.util.Arrays;

import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketHandler implements PacketProcessingListener {

    /**
     * size of MAC address in octets (6*8 = 48 bits)
     */
    private static final int MAC_ADDRESS_SIZE = 6;

    /**
     * start position of destination MAC address in array
     */
    private static final int DST_MAC_START_POSITION = 0;

    /**
     * end position of destination MAC address in array
     */
    private static final int DST_MAC_END_POSITION = 6;

    /**
     * start position of source MAC address in array
     */
    private static final int SRC_MAC_START_POSITION = 6;

    /**
     * end position of source MAC address in array
     */
    private static final int SRC_MAC_END_POSITION = 12;


    /**
     * start position of ethernet type in array
     */
    private static final int ETHER_TYPE_START_POSITION = 12;

    /**
     * end position of ethernet type in array
     */
    private static final int ETHER_TYPE_END_POSITION = 14;

    private static final Logger LOG = LoggerFactory.getLogger(PacketHandler.class);

    public PacketHandler() {
        LOG.info("[Siwind] PacketHandler Initiated. ");
    }

    @Override
    public void onPacketReceived(PacketReceived notification) {
        // TODO Auto-generated method stub

        // read src MAC and dst MAC
        byte[] dstMacRaw = extractDstMac(notification.getPayload());
        byte[] srcMacRaw = extractSrcMac(notification.getPayload());
        byte[] ethType   = extractEtherType(notification.getPayload());

        String dstMac = byteToHexStr(dstMacRaw, ":");
        String srcMac = byteToHexStr(srcMacRaw, ":");
        String ethStr = byteToHexStr(ethType, "");

        LOG.info("[Siwind] Received packet from MAC {} to MAC {}, EtherType=0x{} ", srcMac, dstMac, ethStr);
    }

    /**
     * @param payload
     * @return destination MAC address
     */
    public static byte[] extractDstMac(final byte[] payload) {
        return Arrays.copyOfRange(payload, DST_MAC_START_POSITION, DST_MAC_END_POSITION);
    }

    /**
     * @param payload
     * @return source MAC address
     */
    public static byte[] extractSrcMac(final byte[] payload) {
        return Arrays.copyOfRange(payload, SRC_MAC_START_POSITION, SRC_MAC_END_POSITION);
    }

    /**
     * @param payload
     * @return source MAC address
     */
    public static byte[] extractEtherType(final byte[] payload) {
        return Arrays.copyOfRange(payload, ETHER_TYPE_START_POSITION, ETHER_TYPE_END_POSITION);
    }

    /**
     * @param bts
     * @return wrapping string value, baked upon binary MAC address
     */
    public static String byteToHexStr(final byte[] bts, String delimit) {
        StringBuffer macStr = new StringBuffer();

        for (int i = 0; i < bts.length; i++) {
            String str = Integer.toHexString(bts[i] & 0xFF);
            if( str.length()<=1 ){
                macStr.append("0");
            }
            macStr.append(str + delimit);
        }
        
        if( delimit.length()>=1 ){
            macStr.setCharAt(macStr.length() - 1, ' ');
        }
        return macStr.toString();
    }

}

