#!/bin/bash
# MHCM script
# echo "Apache ODE .war  http is: $1" # [13.11.2014]apache website mirror link is
# http://mirror.sdunix.com/apache/ode/apache-ode-war-1.3.6.zip
# echo "----------------------"
# ## Command line arguments
APACHEODE_URL=$1
BPEL_ZIP_FILENAME=$2
BPEL_FOLDERNAME=$3
## temporary folder to store process files before deploying them to engine
##, this is needed to set right permissions for deployment to be succesful
TEMPORARY_FOLDER=/var/lib/tomcat7/webapps/ode/temp
apt-get update
apt-get install wget
apt-get install unzip tomcat7 -y &
#####################################################
##  APACHE ODE
#####################################################
wget $APACHEODE_URL -O ode.zip &
##move .war file from the zip to tomcat webapps
wait
unzip -j "ode.zip" "apache-ode-war-1.3.6/ode.war" -d "/var/lib/tomcat7/webapps/"
#service tomcat7 restart #is this even necessary?
#cleanup
rm ode.zip &
#####################################################
##  BPEL
#####################################################
mkdir -p /var/lib/tomcat7/webapps/ode/WEB-INF/processes/${BPEL_FOLDERNAME}
unzip -j ${BPEL_ZIP_FILENAME} -d /var/lib/tomcat7/webapps/ode/WEB-INF/processes/${BPEL_FOLDERNAME}
##Set owner of the process directory to tomcat7
chown -R tomcat7 /var/lib/tomcat7/webapps/ode/WEB-INF/processes/${BPEL_FOLDERNAME}
rm -f /var/lib/tomcat7/webapps/ode/WEB-INF/processes/*.deployed