#!/bin/sh

# fix for https://github.com/cryptomator/cryptomator/issues/1370
export LD_PRELOAD=/usr/lib/x86_64-linux-gnu/jni/libjffi-1.2.so

/usr/lib/cryptomator/bin/cryptomator