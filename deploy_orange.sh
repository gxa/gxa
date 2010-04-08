svn update
mvn package -Dmaven.test.skip=true
ssh azorin@orange "sudo -u ma-svc sh /ebi/microarray/home/tomcats/ORANGE.ATLAS2.ATLASLOAD/bin/shutdown.sh"
ssh azorin@orange "sudo -u ma-svc rm -r -f /ebi/microarray/home/tomcats/ORANGE.ATLAS2.ATLASLOAD/webapps/gxa-load"

scp atlas-web/target/atlas-web-2.0-rc3-SNAPSHOT.war azorin@orange:/ebi/microarray/home/tomcats/ORANGE.ATLAS2.ATLASLOAD/
ssh azorin@orange "sudo -u ma-svc sh /ebi/microarray/home/tomcats/ORANGE.ATLAS2.ATLASLOAD/bin/startup.sh"

exit
