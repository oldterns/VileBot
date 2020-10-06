#!/bin/bash

# Copyright (C) 2013 Oldterns
#
# This file may be modified and distributed under the terms
# of the MIT license. See the LICENSE file for details.

set -u
set -e
set -f


# Accept any existing definitons of these variables, otherwise set to their defaults.

: "${VB_REDIS_CONF_PATH:=cfg/redis.conf}"
: "${VB_REDIS_PID_PATH:=/tmp/vb-redis-server-pid-$USER}"
: "${VB_JAR_PATH:=target/vilebot-shaded.jar}"
: "${VB_PID_PATH:=/tmp/vb-server-pid-$USER}"
: "${VB_LOG_PATH:=log}"
: "${VB_REMOTE_DEBUG:=0}"
: "${VB_JAVA_STDOUT:=0}"

die() {
    if [ -n "$*" ]
    then
        echo "Error: $@" >&2
    fi
    exit 1
}

msg() {
    echo "$*" >&2
}

trap '[ $? -eq 0 ] || [ "$FUNCNAME" = "die" ] || echo "Exiting abnormally with code $? (in $FUNCNAME)"' EXIT

get_redis_port() {
    sed -r -e 's/^port ([0-9]+)$/\1/;tx;d;:x' "$VB_REDIS_CONF_PATH"
}

get_pid() {
    local pid_file="$1"
    local pid_pattern='^[0-9]+$'
    local pid=""
    if [ -f "$pid_file" ]
    then
        pid="$(head -n1 "$pid_file")"
        [[ "$pid" =~ $pid_pattern ]] || pid=""
    fi
    echo "$pid"
}

get_vb_pid() {
    get_pid "$VB_PID_PATH"
}

get_redis_pid() {
    get_pid "$VB_REDIS_PID_PATH"
}


mode_start() {
    msg "Starting services"
    msg ">> Testing prerequisites"
    local required_files=("$VB_JAR_PATH"
                          "$VB_REDIS_CONF_PATH"
                          "cfg/vilebot.conf")

    for filepath in "${required_files[@]}"
    do
        if [ -f "$filepath" ]
        then
            msg ">> '$filepath' exists and is a file"
        else
            die ">> '$filepath' does not exist or is not a file"
        fi
    done

    mkdir -p db
    mkdir -p log

    status_info=$(mode_status | grep -e ' up$' || true)

    if [[ "$status_info" =~ redis ]]
    then
        msg ">> Redis server already up"
    else
        msg ">> Starting local redis server"
        nohup redis-server "$VB_REDIS_CONF_PATH" 1>>"$VB_LOG_PATH/redis-stdout.log" 2>&1 &
        echo -n "$!" >| "$VB_REDIS_PID_PATH"
    fi

    if [[ "$status_info" =~ vilebot ]]
    then
        msg ">> Vilebot already up"
    else
        local extra_opts=""
        if [ "$VB_REMOTE_DEBUG" = "1" ]
        then
            extra_opts="-Xdebug -Xrunjdwp:transport=dt_socket,address=8001,server=y,suspend=y"
            msg ">> Starting Vilebot for remote debugging on port 8001"
        else
            msg ">> Starting Vilebot"
        fi

        if [ "$VB_JAVA_STDOUT" = "1" ]
        then
            java $extra_opts -jar "$VB_JAR_PATH" 1
        else
            nohup java $extra_opts -jar "$VB_JAR_PATH" 1>>"$VB_LOG_PATH/vilebot-stdout.log" 2>&1 &
        fi
        echo -n "$!" >| "$VB_PID_PATH"
    fi

    sleep 1s

    msg "Testing status"
    if mode_status | grep -q -e ' down$'
    then
        msg ">> Did not start successfully"
        mode_stop
    else
        msg ">> Started successfully"
    fi
}

mode_stop() {
    msg "Stopping services"

    local vb_pid="$(get_vb_pid)"
    local redis_pid="$(get_redis_pid)"

    if [ -n "$vb_pid" ]
    then
        msg ">> Stopping vilebot"
        kill -15 "$vb_pid" || msg ">> Couldn't send term signal to vilebot"
        while kill -0 "$vb_pid" >/dev/null 2>&1
        do
            sleep 1
        done
    else
        msg ">> No pid for vilebot"
    fi

    msg ">> Stopping local redis server"
    redis-cli -p "$(get_redis_port)" SHUTDOWN || msg ">> Couldn't send shutdown command to redis server"
}

mode_status() {
    _is_redis_up() {
        redis-cli -p "$(get_redis_port)" PING >/dev/null 2>&1
    }

    _is_vilebot_up() {
        local vb_pid="$(get_vb_pid)"
        if [ -n "$vb_pid" ]
        then
            kill -0 "$(get_vb_pid)" >/dev/null 2>&1
        else
            return 1
        fi
    }

    if _is_redis_up
    then
        echo "redis up"
    else
        echo "redis down"
    fi

    if _is_vilebot_up
    then
        echo "vilebot up"
    else
        echo "vilebot down"
    fi

    true
}

mode_restart() {
    mode_stop
    mode_start
}

# http://stackoverflow.com/a/630645/1905196
get_script_location() {
    prg=$0
    if [ ! -e "$prg" ]; then
      case $prg in
        (*/*) exit 1;;
        (*) prg=$(command -v -- "$prg") || exit;;
      esac
    fi
    dir=$(
      cd -P -- "$(dirname -- "$prg")" && pwd -P
    ) || exit
    prg=$dir/$(basename -- "$prg") || exit

    printf '%s\n' "$prg"
}
SCRIPT_ROOT="$(dirname $(get_script_location))"
cd "$SCRIPT_ROOT"

# Get target state
if [ "$#" -eq 1 ]
then
    mode_pattern='^[a-z]+$'
    [[ "$1" =~ $mode_pattern ]] || die "Invalid mode, must conform to: $mode_pattern"
    mode="$1"
else
    die "Need one argument: mode (one of start,stop,status,restart)"
fi

# Execute the right function, if available
if declare -f "mode_$mode" >/dev/null
then
    eval "mode_$mode"
else
    die "No such mode '$mode'"
fi
