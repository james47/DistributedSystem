
mkdir -p ~/src/usr/local/

# cassandra
mkdir -p ~/src/cassandra
cd ~/src/cassandra
wget http://apache.cs.utah.edu/cassandra/3.11.2/apache-cassandra-3.11.2-bin.tar.gz
tar -xvf apache-cassandra-3.11.2-bin.tar.gz
mv ~/src/cassandra/apache-cassandra-3.11.2/ ~/src/usr/local/cassandra

# run cassandra in frontend, stop it by Ctrl+C
# ~/src/usr/local/cassandra/bin/cassandra -f

# redis
mkdir -p ~/src/redis
cd ~/src/redis
wget http://download.redis.io/releases/redis-4.0.9.tar.gz
tar xzf redis-4.0.9.tar.gz
cd redis-4.0.9
make

# run redis server in frontend, stop it by Ctrl+C
# ~/src/redis/redis-4.0.9/src/redis-server --protected-mode no
# run redis cli
# ~/src/redis/redis-4.0.9/src/redis-cli

# using python3.4
# python packages, including cassandra-driver, redis, flask, etc
pip install virtualenv --user
~/.local/bin/virtualenv ~/src/PYENV
source ~/src/PYENV/bin/activate
pip install cassandra-driver
pip install redis
pip install Flask
pip install pyyaml
pip install Pillow
pip install requests
deactivate

# nginx
# mkdir -p ~/src/nginx
# cd ~/src/nginx
# wget http://nginx.org/download/nginx-1.10.1.tar.gz
# tar xvf nginx-1.10.1.tar.gz
# cd ~/src/nginx/nginx-1.10.1
# mkdir -p ~/src/usr/local/nginx
# ./configure
# make clean && make && make install
