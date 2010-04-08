# svn update
export MAVEN_OPTS=-Xmx1024m

mvn -Pweb,tctest1 clean package -Dmaven.test.skip=true

ssh azorin@tc-test-1 "sudo -u tc_user /ebi/www/prod/servers/tc-microarray/tomcat-controller stop"

ssh azorin@tc-test-1 "cd /ebi/www/prod/servers/tc-microarray/webapps/tc-test; sudo -u tc_user rm -r -f /ebi/www/prod/servers/tc-microarray/webapps/tc-test/microarray-as/atlas"

scp atlas-web/target/atlas-web-2.0-rc3-SNAPSHOT.war azorin@tc-test-1:/ebi/microarray/home/azorin/

ssh azorin@tc-test-1 "sudo -u tc_user cp /ebi/microarray/home/azorin/atlas-web-2.0-rc3-SNAPSHOT.war /ebi/www/prod/deploy/war/"

ssh azorin@tc-test-1 "sudo -u tc_user /ebi/www/prod/servers/tc-microarray/tomcat-controller start"

wget admin:admin@http://tc-test-1:8102/ping/wait?status=//alive

exit
