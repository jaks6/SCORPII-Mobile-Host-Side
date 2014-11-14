#!/bin/bash
# MHCM script
echo "Apache ODE .war  http is: $1" # [13.11.2014]apache website mirror link is http://mirror.sdunix.com/apache/ode/apache-ode-war-1.3.6.zip
echo "----------------------"
### Command line arguments
APACHEODE_URL=$1
apt-get update
apt-get install tomcat7 -y
apt-get install wget
apt-get install unzip
wget $APACHEODE_URL -O ode.zip
unzip ode.zip -d /var/lib/tomcat7/webapps