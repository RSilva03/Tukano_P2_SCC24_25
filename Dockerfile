FROM tomcat:10.0-jdk17-openjdk
WORKDIR /usr/local/tomcat
ADD target/tukano-1.war webapps
EXPOSE 8080