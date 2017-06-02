# Vilebot
[![Build Status](https://travis-ci.org/oldterns/VileBot.svg?branch=master)](https://travis-ci.org/oldterns/VileBot)

## Setup

### Clone keratin-irc

    git clone git@github.com:ASzc/keratin-irc.git

### Create config file

    cp cfg/vilebot.conf.example cfg/vilebot.conf
    $EDITOR cfg/vilebot.conf

### Eclipse import

Install m2e

    yum install eclipse-m2e-core

Reopen Eclipse if it is open, then use File > Import > Existing Maven Projects > Root Directory: <local repo location>

Import the maven source style definitions by downloading

    http://maven.apache.org/developers/maven-eclipse-codestyle.xml

and then use it with

    Project > Properties > Java Code Style > Formatter > Enable Project Specific Settings > Import ...

### Redis

Install redis server

    yum install redis

redis-server is run with the local config file automatically by server-control.sh

## Build

Maven is used to get dependencies and to build. This command will generate a jar in target/

    mvn package

## Run

Use the start script to start Vilebot:

    ./server-control.sh start

## Notes

### Redis configuration bootstrap

This section describes how the local redis-server configuration was created.

Copy and edit redis.conf

    cp /etc/redis.conf ./cfg/
    vim redis.conf

Make sure redis.conf has the following (some may be there already), and that no exiting lines contradict:

    daemonize no
    port 6300
    bind 127.0.0.1
    logfile ../log/redis.log
    databases 1
    save 900 1
    save 300 10
    save 60 10000
    rdbcompression yes
    dbfilename vb-dbdump.rdb
    dir db/
    appendonly no
    vm-enabled no
    activerehashing yes

If redis on Fedora upgrades to a more recent verison it's possible that virtual memory will be completely removed. We don't use it anyway, so remove/comment out the virtual memory options if they start being unrecognised after an upgrade.

### Twitter Integration

Create a new application at https://apps.twitter.com

Under the API Keys tab, create a new set of access tokens.

Once generated, copy the API key, API secret, Access Token, and Access Token Secret into the appropriate variables in the UrlTweetAnnouncer class. Do not, at any point in time, commit these values.

### External Files

All external files should be placed inside the "files" folder. 

For example, our "wordlist.txt" file used by our Omgword feature is stored there.
