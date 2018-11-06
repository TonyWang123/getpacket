/*
 * Copyright © 2016 siwind, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package com.siwind.bupt.impl;

import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;

import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetpacketProvider {

    private static final Logger LOG = LoggerFactory.getLogger(GetpacketProvider.class);

    private final DataBroker dataBroker;

    private final NotificationPublishService notificationPublishService;
    private final NotificationService notificationService;

    // registration for PacketProcessingListener
    private ListenerRegistration<NotificationListener> registration = null;

    public GetpacketProvider(final DataBroker dataBroker,
            final NotificationPublishService notificationPublishService,
            final NotificationService notificationService ) {

        this.dataBroker = dataBroker;
        this.notificationPublishService = notificationPublishService;
        this.notificationService = notificationService;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("GetpacketProvider Session Initiated");
        
        if (notificationService != null) {
            LOG.info("NotificationService is: " + notificationService.toString());
            
            CloseableHttpClient client = HttpClients.createDefault();
            
            LOG.info("Created httpclient");
            
            PacketHandler packetHandler = new PacketHandler(client, this.dataBroker);
            registration = notificationService.registerNotificationListener(packetHandler);


        }

    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("GetpacketProvider Closed");

        if( registration != null){
            registration.close();           
        }
    }
}
