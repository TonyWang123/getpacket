getpacket project
=================
This is the Opendaylight demo project for receiving any raw packet-data from network.
It works on :
* Opendaylight version: Boron-SR1, Beryllium-SR+, and previous version


# Introduction
In Opendaylight, "packet-received" notification from module(packet-processing 2013-07-09) 
provides the ability to obtain any raw packet-data from network to controller.

# For Opendaylight version of Beryllium
The content of "xxxProvider.java " would look like this:
```
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.NotificationListener;

public class MygetpacketProvider implements BindingAwareProvider, AutoCloseable {

    private NotificationService notificatonService = null;

    // registration for PacketProcessingListener
    private ListenerRegistration<NotificationListener> registration = null;

    @Override
    public void onSessionInitiated(ProviderContext session) {
        LOG.info("MygetpacketProvider Session Initiated");

        notificatonService = session.getSALService(NotificationService.class);

        PacketReceivedHandler handler = new PacketReceivedHandler();
        registration = notificatonService.registerNotificationListener(handler);

    }

    @Override
    public void close() throws Exception {
        LOG.info("MygetpacketProvider Closed");

        if( registration != null ){
            LOG.info("Mygetpacket registration closed.");
            registration.close();
        }

    }

}

```


# HOW TO BUILD
In order to build the project, it's required to have JDK 1.8+ and Maven 3.2+. 
The following commands are used to build and run this example.

```
$ git clone https://github.com/siwind/getpacket
$ mvn clean install
$ ./karaf/target/assembly/bin/karaf 

karaf>feature:list -i | grep getpacket
karaf>log:tail

```

