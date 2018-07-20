FROM daocloud.io/centos
MAINTAINER bytegriffin "bytegriffin@126.com"

# OS环境配置
RUN yum install -y wget unzip

# 安装JDK
RUN wget --no-check-certificate --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie"  -P /opt http://download.oracle.com/otn-pub/java/jdk/8u171-b11/512cd62ec5174c3487ac17c61aaa89e8/jdk-8u171-linux-x64.tar.gz
RUN tar xzf /opt/jdk-8u171-linux-x64.tar.gz -C /opt && rm -rf /opt/jdk-8u171-linux-x64.tar.gz
RUN rm -rf /opt/jdk1.8.0_171/javafx-src.zip && rm -rf /opt/jdk1.8.0_171/src.zip && rm -rf /opt/jdk1.8.0_171/db/

# 安装Get4J-core项目
RUN wget -P /opt https://github.com/bytegriffin/Get4J/releases/download/1.1.0/get4j-core-1.1.0-bin.tar.gz
RUN tar zxvf /opt/get4j-core-1.1.0-bin.tar.gz -C /opt && rm -rf /opt/get4j-core-1.1.0-bin.tar.gz
RUN wget -P /opt http://npm.taobao.org/mirrors/chromedriver/2.40/chromedriver_linux64.zip
RUN unzip /opt/chromedriver_linux64.zip -d /opt && mv /opt/chromedriver /opt/get4j-core-1.1.0/bin && rm -rf /opt/chromedriver_linux64.zip

# 设置环境变量
ENV JAVA_HOME /opt/jdk1.8.0_171
ENV CLASSPATH .:$JAVA_HOME/lib:$JRE_HOME/lib
ENV PATH $PATH:$JAVA_HOME/bin:



