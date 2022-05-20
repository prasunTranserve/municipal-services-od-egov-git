FROM egovio/apline-jre:8u121

USER root
COPY nma.cer $JAVA_HOME/lib/security
RUN \
    cd $JAVA_HOME/lib/security \
    && keytool -keystore cacerts -storepass changeit -noprompt -trustcacerts -importcert -alias nmacert -file nma.cer

# INSTRUCTIONS ON HOW TO BUILD JAR:
# Move to the location where pom.xml is exist in project and build project using below command
# "mvn clean package"
COPY /target/noc-services-1.0.1-SNAPSHOT.jar /opt/egov/noc-services.jar

COPY start.sh /usr/bin/start.sh

RUN chmod +x /usr/bin/start.sh

CMD ["/usr/bin/start.sh"]

# NOTE: the two 'RUN' commands can probably be combined inside of a single
# script (i.e. RUN build-and-install-app.sh) so that we can also clean up the
# extra files created during the `mvn package' command. that step inflates the
# resultant image by almost 1.0GB.
