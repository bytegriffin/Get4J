FROM anapsix/alpine-java:8u201b09_jdk
MAINTAINER bytegriffin "bytegriffin@126.com"

# 安装Get4J-core项目
RUN wget -P /opt https://github.com/bytegriffin/Get4J/releases/download/Get4J-1.2.0/get4j-core-1.2.0-bin.tar.gz
RUN tar zxvf /opt/get4j-core-1.2.0-bin.tar.gz -C /opt && rm -rf /opt/get4j-core-1.2.0-bin.tar.gz
RUN wget -P /opt https://npm.taobao.org/mirrors/chromedriver/2.44/chromedriver_linux64.zip
RUN unzip /opt/chromedriver_linux64.zip -d /opt && mv /opt/chromedriver /opt/get4j-core-1.2.0/bin && rm -rf /opt/chromedriver_linux64.zip




