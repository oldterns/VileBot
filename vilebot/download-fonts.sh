#!/usr/bin/env bash

mkdir files/fonts
for font in `cat files/fontlist.txt`;
do
    wget -P files/fonts/ $font
done
