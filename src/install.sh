# Install the stable Python version because the current version (v3.5) is deprecated
VERSION=3.8
add-apt-repository ppa:deadsnakes/ppa -y
apt update
apt install python$VERSION python$VERSION-distutils python$VERSION-venv -y
update-alternatives --install /usr/bin/python3 python3 /usr/bin/python$VERSION 1

# Install pip (Python package manager)
apt install curl -y
curl -fsSL -o- https://bootstrap.pypa.io/get-pip.py | python3

# Update Java version
apt install openjdk-8-jdk -y

# Install Maven
VERSION=3.6.3
wget -O /tmp/maven.tar.gz https://apache.dattatec.com/maven/maven-3/3.6.3/binaries/apache-maven-$VERSION-bin.tar.gz
tar -zxvf /tmp/maven.tar.gz -C /etc/
mv /etc/apache-maven-$VERSION /etc/maven
update-alternatives --install /usr/bin/mvn mvn /etc/maven/bin/mvn 1
