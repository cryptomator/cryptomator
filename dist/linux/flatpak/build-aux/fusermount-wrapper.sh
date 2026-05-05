#!/bin/sh

# based on https://gitlab.gnome.org/GNOME/gnome-builder/-/blob/main/build-aux/flatpak/fusermount-wrapper.sh
# FUSE3 requires a socket for communication, its file descriptor id is given in the _FUSE_COMMFD variable
exec flatpak-spawn --host --watch-bus --forward-fd=1 --forward-fd=2 --env=_FUSE_COMMFD=${_FUSE_COMMFD} --forward-fd=${_FUSE_COMMFD} fusermount3 "$@"
