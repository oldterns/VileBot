#!/bin/bash

# Copyright (C) 2013 Oldterns
#
# This file may be modified and distributed under the terms
# of the MIT license. See the LICENSE file for details.

# Disable:
set -u # Undefined vars
set -e # Complete statement error
set -f # File expansion
set -C # Clobbering

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

trap "die \"Error at line \$LINENO\". Exiting." ERR

[ "$#" -eq 0 ] || die "No arguments are accepted."


require_dir() {
    local dir="$1"
    local name="$2"
    local make_mode="$3"

    if [ -e "$dir" ]
    then
        if ! [ -d "$dir" ]
        then
            die "$name directory ($dir) exists but is not a directory"
        fi
    else
        if [ "$make_mode" = "make" ]
        then
            mkdir -p "$dir"
        else
            die "$name directory ($dir) does not exist"
        fi
    fi
}

require_file() {
    local file="$1"
    local name="$2"

    if [ -e "$file" ]
    then
        if ! [ -f "$file" ]
        then
            die "$name file ($file) exists but is not a file"
        fi
    else
        die "$name file ($file) does not exist"
    fi
}

require_non_existent() {
    local path="$1"
    local name="$2"

    if [ -e "$path" ]
    then
        die "$name path ($path) exists"
    fi
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

DB_PATH="$SCRIPT_ROOT/../db"
require_dir "$DB_PATH" "Database" "nomake"

DB_BACKUPS_PATH="$SCRIPT_ROOT/../db-backups"
require_dir "$DB_BACKUPS_PATH" "Database Backup" "make"

DB_FILE="$DB_PATH/vb-dbdump.rdb"
require_file "$DB_FILE" "Database Dump"

timestamp=$(date +%y-%m-%d-%s)
DB_BACKUP_FILE="$DB_BACKUPS_PATH/vilebot-db_$timestamp.rdb.xz"
DB_VERIFY_FILE="/tmp/vilebot-db_$timestamp.rdb.verify"


echo_and_run() {
    echo "    >> $@" | sed -e "s:$SCRIPT_ROOT:SCRIPT_ROOT:g" >&2
    eval "$@" 2>&1 | sed -e 's/^/       /'
    [ "$PIPESTATUS" -eq 0 ] || die "$1 exited with error code $PIPESTATUS. Stopping."
}

# Redis apparently does some fancy stuff to allow use of regular copy
# operations to backup the dump file without consistency issues. No need for a
# special "hot backup" type thing.

msg "Compressing dump file"
require_non_existent "$DB_BACKUP_FILE" "Compressed backup"
echo_and_run xz -z --keep -C sha256 -F xz --stdout "$DB_FILE" "> \"$DB_BACKUP_FILE\""

msg "Testing compressed backup"
echo_and_run xz -t "$DB_BACKUP_FILE"

msg "Verifying with redis-check-rdb"
echo_and_run xz -d --keep --stdout "$DB_BACKUP_FILE" "> \"$DB_VERIFY_FILE\""
echo_and_run redis-check-rdb "$DB_VERIFY_FILE"
echo_and_run rm "$DB_VERIFY_FILE"

msg "Backup complete: $DB_BACKUP_FILE"
