# Galaxy - DKProGalaxydocker
#
# VERSION       0.1

FROM bgruening/galaxy-stable
MAINTAINER Anshul Tak, anshul.tak@gmail.com

RUN rm /bin/sh && ln -s /bin/bash /bin/sh

RUN apt-get -qq update && apt-get install --no-install-recommends -y apt-transport-https software-properties-common wget && \
    apt-key adv --keyserver hkp://p80.pool.sks-keyservers.net:80 --recv-keys 58118E89F3A912897C070ADBF76221572C52609D && \
    sh -c "echo deb https://get.docker.io/ubuntu docker main > /etc/apt/sources.list.d/docker.list" && \
    sh -c "echo deb http://research.cs.wisc.edu/htcondor/ubuntu/stable/ trusty contrib > /etc/apt/sources.list.d/htcondor.list" && \
    sh -c "wget -qO - http://research.cs.wisc.edu/htcondor/ubuntu/HTCondor-Release.gpg.key | sudo apt-key add -" && \
    apt-add-repository -y ppa:webupd8team/java && \
    apt-get update -qq && \
    echo debconf shared/accepted-oracle-license-v1-1 select true | sudo debconf-set-selections && \
    echo debconf shared/accepted-oracle-license-v1-1 seen true | sudo debconf-set-selections && \
    apt-get -y install oracle-java8-installer && \
    apt-get install -y oracle-java8-installer && \
    apt-get install -y oracle-java8-set-default && \
    apt-get purge -y software-properties-common && \
    apt-get autoremove -y && apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*


RUN apt-get update && \
    apt-get install -y curl unzip zip && \
    apt-get autoremove -y && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* && \
    rm -rf /tmp/*
ENV SDKMAN_DIR=/opt/sdkman 
RUN curl -s get.sdkman.io | bash && chown -R root:root /opt/sdkman
RUN chmod +x /opt/sdkman/bin/sdkman-init.sh
RUN source /opt/sdkman/bin/sdkman-init.sh && sdk install groovy && groovy -version
 
# Autostart script that is invoked during container start
CMD source /opt/sdkman/bin/sdkman-init.sh && /usr/bin/startup
