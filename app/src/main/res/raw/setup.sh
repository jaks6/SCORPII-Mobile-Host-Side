#!/bin/bash
# MHCM script
# echo "Apache ODE .war  http is: $1" # [13.11.2014]apache website mirror link is
# http://mirror.sdunix.com/apache/ode/apache-ode-war-1.3.6.zip
# echo "----------------------"
# ## Command line arguments
APACHEODE_URL=$1
## temporary folder to store process files before deploying them to engine
##, this is needed to set right permissions for deployment to be succesful
TEMPORARY_FOLDER=/var/lib/tomcat7/webapps/ode/temp
apt-get update
apt-get install tomcat7 -y
apt-get install wget
apt-get install unzip
#####################################################
##  APACHE ODE
#####################################################
wget $APACHEODE_URL -O ode.zip
##move .war file from the zip to tomcat webapps
unzip -j "ode.zip" "apache-ode-war-1.3.6/ode.war" -d "/var/lib/tomcat7/webapps/"
service tomcat7 restart
#cleanup
rm ode.zip
#####################################################
##  BPEL
#####################################################
mkdir -p ${TEMPORARY_FOLDER}
mkdir -p /var/lib/tomcat7/webapps/ode/WEB-INF/processes/HelloWorld
unzip -j "bpel.zip" -d ${TEMPORARY_FOLDER}
##Set owner of the process directory to tomcat7
chown -R tomcat7 /var/lib/tomcat7/webapps/ode/temp
chown -R tomcat7 /var/lib/tomcat7/webapps/ode/WEB-INF/processes/HelloWorld
mv /var/lib/tomcat7/webapps/ode/temp/* /var/lib/tomcat7/webapps/ode/WEB-INF/processes/HelloWorld