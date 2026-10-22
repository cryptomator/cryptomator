#!/bin/sh

# From: https://gitlab.gnome.org/GNOME/gnome-builder/-/blob/main/build-aux/flatpak/fusermount-wrapper.sh

if [ -z "$_FUSE_COMMFD" ]; then
    FD_ARGS=
else
    FD_ARGS="--env=_FUSE_COMMFD=${_FUSE_COMMFD} --forward-fd=${_FUSE_COMMFD}"
fi

if [ -e /proc/self/fd/3 ] && [ 3 != "$_FUSE_COMMFD" ]; then
    FD_ARGS="$FD_ARGS --forward-fd=3"
fi

exec flatpak-spawn --host --forward-fd=1 --forward-fd=2 $FD_ARGS fusermount3 "$@"
