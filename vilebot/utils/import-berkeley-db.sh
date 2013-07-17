#!/bin/bash

# Copyright (C) 2013 Oldterns
#
# This file may be modified and distributed under the terms
# of the MIT license. See the LICENSE file for details.

# Script to import old Berkeley DB databases into Redis
# For Fedora, requires packages: db4-utils redis

set -u
set -e

die() {
    if [ -n "$*" ]
    then
        echo "==> $@" >&2
    fi
    exit 1
}

msg() {
    echo "==> $@" >&2
}


[ "$#" -eq 2 ] || die "Two arguments are required: <db4 dir> <redis port number>"

vilebot_db_dir="$1"
[ -e "$vilebot_db_dir" ] || die "Given db dir does not exist"
[ -d "$vilebot_db_dir" ] || die "Given db dir is not a directory"

redis_server="localhost"
redis_port="$2"
[[ "$redis_port" =~ ^[0-9]+$ ]] || die "Given redis port is not a number"


expect_db() {
    local db_file="$1"
    [ -e "$db_file" ] || die "$db_file does not exist"
    [ -f "$db_file" ] || die "$db_file is not a file"
}

# Dumps the named db's contents as lines of:  key\tvalue
dump_db() {
    local db_file_basename="$1"
    local db_file="$vilebot_db_dir/$db_file_basename"
    expect_db "$db_file"
    db4_dump -p "$db_file" | sed -e "s/^ //;tx;d;:x"
    [ "$PIPESTATUS" -eq 0 ] || die "db4_dump exited with code $PIPESTATUS"
}

# Executes the given command for every BDB entry against the redis server, with expansion of bash variables
exec_redis() {
    _redis_call_expand() {
        expanded_args=()
        while [ "$#" -gt 0 ]
        do
            expanded_args+=("$(eval echo "$1")")
            shift
        done
        set -- "${expanded_args[@]}"

        echo "$@"
        # This is not the most efficient way to send multiple commands, but it's simple.
        redis-cli -p "$redis_port" "$@" || die "redis-cli exited with code $?"
    }

    # Default to impossible regex
    : ${KEY_FILTER:=$.^}
    : ${VALUE_FILTER:=$.^}

    local db_name="$1"
    shift

    dump_db "$db_name" | while read -r line
    do
        key="$line"
        read -r line || die "Odd number of dumped lines at key $key"
        value="$line"

        if echo "$key" | grep -q -E -e "$KEY_FILTER"
        then
            msg "Skipping entry due to filter hit on key ($key=$value)"
        elif echo "$value" | grep -q -E -e "$VALUE_FILTER"
        then
            msg "Skipping entry due to filter hit on value ($key=$value)"
        else
            _redis_call_expand "$@"
        fi
    done
}

>| db-import.log
{

    msg "Facts"
    KEY_FILTER='^[^a-zA-Z0-9]' exec_redis facts SADD 'noun-facts-$key' '$(echo "$value" | sed -e "s/^ *$key *//")'

    msg "Quotes"
    KEY_FILTER='^[^a-zA-Z0-9]' exec_redis quotes SADD 'noun-quotes-$key' '$value'

    msg "Lists"
    KEY_FILTER='seekrit' exec_redis lists SADD 'userlist-$key' '$value'

    msg "Ranks/Karma"
    KEY_FILTER="^[^a-zA-Z1-9]|[+ |:~'\\|_-,]" exec_redis ranks ZADD 'noun-karma' '$value' '$key'

    msg "Last Seen"
    # Value is converted to miliseconds since epoch
    KEY_FILTER="^[^a-zA-Z0-9]|[+ |:~'\\|_-,]" exec_redis lastSeen HSET 'last-seen' '$key' '$(( $(date -d "$value" +%s%N) / 1000000 ))'

    msg "Excuses"
    exec_redis excuses SADD 'excuses' '$value'

} >> db-import.log 2> >(tee -a db-import.log >&2)

msg "Done. Review db-import.log"
