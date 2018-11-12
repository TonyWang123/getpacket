/*
 * Copyright © 2016 siwind, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package com.siwind.bupt.impl;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.openflowjava.protocol.api.connection.StatisticsConfiguration;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.siwind.bupt.impl.util.BitBufferHelper;

public class PacketHandler implements PacketProcessingListener, DataTreeChangeListener<Link> {

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
    
    
    private static final int IPV4_PROTOCOL_START_POSITION = ETHER_TYPE_END_POSITION + 9;
    
    private static final int IPV4_PROTOCOL_END_POSITION = IPV4_PROTOCOL_START_POSITION + 1;
    
    private static final int IPV4_SRCIP_START_POSITION = ETHER_TYPE_END_POSITION + 12;
    
    private static final int IPV4_SRCIP_END_POSITION = IPV4_SRCIP_START_POSITION + 4;
    
    private static final int IPV4_DSTIP_START_POSITION = ETHER_TYPE_END_POSITION + 16;
    
    private static final int IPV4_DSTIP_END_POSITION = IPV4_DSTIP_START_POSITION + 4;
    
    private static final int TCP_SRCPORT_START_POSITION = IPV4_DSTIP_END_POSITION;
    
    private static final int TCP_SRCPORT_END_POSITION = TCP_SRCPORT_START_POSITION + 2;
    
    private static final int TCP_DSTPORT_START_POSITION = TCP_SRCPORT_END_POSITION;
    
    private static final int TCP_DSTPORT_END_POSITION = TCP_DSTPORT_START_POSITION + 2;
    
    
    private static final String TRIDENT_URL = "http://127.0.0.1:12321/";


    private static final Logger LOG = LoggerFactory.getLogger(PacketHandler.class);
    
    private CloseableHttpClient httpClient = null;
    
    private DataBroker dataBroker;

    public PacketHandler(CloseableHttpClient httpClient, DataBroker dataBroker) {
        LOG.info("[Siwind] PacketHandler Initiated. ");
        this.httpClient = httpClient;
        this.dataBroker = dataBroker;
    }
    
    private static Pattern ingressP = Pattern.compile("openflow:[0-9]+:[0-9]+");
    
    public static String getIngressStr(String nodeConnectorRefStr) {
    	Matcher m = ingressP.matcher(nodeConnectorRefStr);
    	if (m.find()) {
    		return m.group();
    	}
    	return null;
    }
    
    public static int toUnsigned(short s) {
    	return s & 0x0FFFF;
    }

    @Override
    public void onPacketReceived(PacketReceived notification) {
    	
    	String ingressString = null;
    	
    	NodeConnectorRef ref = notification.getIngress();
    	
    	LOG.info("[Siwind] NodeConnectorRef: " + ref.toString());
    	LOG.info("[Siwind] NodeConnectorRef value: " + ref.getValue().toString());
    	//KeyedInstanceIdentifier{targetType=interface org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector, path=[org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes, org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node[key=NodeKey [_id=Uri [_value=openflow:1]]], org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector[key=NodeConnectorKey [_id=Uri [_value=openflow:1:1]]]]}
        
    	/*NodeConnector nodeConnector;
		try {
			nodeConnector = (NodeConnector) dataBroker.newReadOnlyTransaction()
			        .read(LogicalDatastoreType.OPERATIONAL, ref.getValue()).get();
			NodeConnectorId nodeConnectorId = nodeConnector.getId();
	        ingressString = nodeConnectorId.getValue();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ExecutionException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}*/
    	
    	ingressString = getIngressStr(ref.getValue().toString());
    	
    	LOG.info("[Siwind] Packet received from ingress: " + ingressString);
    	String srcIP = null, dstIP = null, srcPort = null, dstPort = null, protocol = null;
    	
    	LOG.info("[Siwind] Received packet length: " + notification.getPayload().length);
    	
    	LOG.info("[Siwind] Received packet byte: " + byteToHexStr(notification.getPayload(), " "));
    	
    	if (notification.getPayload().length == 0) return;
    	
        // read src MAC and dst MAC
        byte[] dstMacRaw = extractDstMac(notification.getPayload());
        byte[] srcMacRaw = extractSrcMac(notification.getPayload());
        byte[] ethType   = extractEtherType(notification.getPayload());
        
        if (BitBufferHelper.getShort(ethType) == 0x0800) {
        	LOG.info("[Siwind] IP Packet received. ");
        	// IPv4
        	byte[] srcIPBytes = Arrays.copyOfRange(notification.getPayload(), IPV4_SRCIP_START_POSITION, IPV4_SRCIP_END_POSITION);
        	int srcIPInt = BitBufferHelper.getInt(srcIPBytes);
        	InetAddress srcIPAddress;
			try {
				srcIPAddress = InetAddress.getByAddress(BigInteger.valueOf(srcIPInt).toByteArray());
				srcIP = srcIPAddress.getHostAddress();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	
        	
        	byte[] dstIPBytes = Arrays.copyOfRange(notification.getPayload(), IPV4_DSTIP_START_POSITION, IPV4_DSTIP_END_POSITION);
        	int dstIPInt = BitBufferHelper.getInt(dstIPBytes);
        	InetAddress dstIPAddress;
			try {
				dstIPAddress = InetAddress.getByAddress(BigInteger.valueOf(dstIPInt).toByteArray());
				dstIP = dstIPAddress.getHostAddress();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	
        	
        	
        	byte[] protocolBytes = Arrays.copyOfRange(notification.getPayload(), IPV4_PROTOCOL_START_POSITION, IPV4_PROTOCOL_END_POSITION);
        	short protocolS = BitBufferHelper.getShort(protocolBytes);
        	protocol = String.valueOf(protocolS);
        	if (protocolS == 6) {
        		protocol = "tcp";
        		LOG.info("[Siwind] TCP Packet received. ");
        		// tcp
        		byte[] srcPortBytes = Arrays.copyOfRange(notification.getPayload(), TCP_SRCPORT_START_POSITION, TCP_SRCPORT_END_POSITION);
        		short srcPortS = BitBufferHelper.getShort(srcPortBytes);
        		srcPort = String.valueOf(toUnsigned(srcPortS));
        		
        		byte[] dstPortBytes = Arrays.copyOfRange(notification.getPayload(), TCP_DSTPORT_START_POSITION, TCP_DSTPORT_END_POSITION);
        		short dstPortS = BitBufferHelper.getShort(dstPortBytes);
        		dstPort = String.valueOf(toUnsigned(dstPortS));
        	}
        }

        //String dstMac = byteToHexStr(dstMacRaw, ":");
        //String srcMac = byteToHexStr(srcMacRaw, ":");
        //String ethStr = byteToHexStr(ethType, "");
        if (protocol == "tcp") {
        	String packet = "sip=" + srcIP + "/32&dip=" + dstIP + "/32&sport=" + srcPort + "&dport=" + dstPort + "&proto=tcp&inport=" + ingressString;
        	//String packet = srcIP + "and" + dstIP + "and" + protocol + "and" + srcPort + "and" + dstPort;
            
            HttpGet httpGet = new HttpGet(TRIDENT_URL + "packet?" + packet);

            LOG.info("[Siwind] Received packet:" + packet);
            
            try {
    			httpClient.execute(httpGet);
    			LOG.info("[Siwind] Send packet to trident");
    		} catch (ClientProtocolException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		} catch (IOException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
        }

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
            macStr.append(str);

            if( i < bts.length - 1 ) { //not last delimit string
                macStr.append(delimit);
            }
        } // end of for !!

        return macStr.toString();
    }
    
    public ListenerRegistration<PacketHandler> registerAsDataChangeListener() {
        InstanceIdentifier<Link> linkInstance = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId("flow:1"))).child(Link.class).build();
        return dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(
                LogicalDatastoreType.OPERATIONAL, linkInstance), this);
    }

	@Override
	public void onDataTreeChanged(Collection<DataTreeModification<Link>> changes) {
		boolean isGraphUpdated = false;
		
		LOG.info("[Siwind] Link changes");

        for (DataTreeModification<Link> change: changes) {
            DataObjectModification<Link> rootNode = change.getRootNode();
            switch (rootNode.getModificationType()) {
                case WRITE:
                    Link createdLink = rootNode.getDataAfter();
                    if (rootNode.getDataBefore() == null && !createdLink.getLinkId().getValue().contains("host")) {
                        isGraphUpdated = true;
                        LOG.info("[Siwind] Graph is updated! Added Link {}", createdLink.getLinkId().getValue());
                    }
                    break;
                case DELETE:
                    Link deletedLink = rootNode.getDataBefore();
                    if (!deletedLink.getLinkId().getValue().contains("host")) {
                        isGraphUpdated = true;
                        LOG.info("[Siwind] Graph is updated! Removed Link {}", deletedLink.getLinkId().getValue());
                        break;
                    }
                    break;
                default:
                    break;
            }
        }

	}

}

