getpacket project
=================
This is the Opendaylight demo project for receiving any raw packet-data from network.
It works on :
* Opendaylight version: Boron-SR1, Beryllium-SR+, and previous version


# Introduction
In Opendaylight, "packet-received" notification from module(packet-processing 2013-07-09) 
provides the ability to obtain any raw packet-data from network to controller.


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

