#!/bin/sh

# based on https://gitlab.gnome.org/GNOME/gnome-builder/-/blob/main/build-aux/flatpak/fusermount-wrapper.sh
#
# Sandbox escape hatch to call fusermount3 on the host system
#
# * fusermount3 is required for mount and unmount
# * FUSE3 requires for mounting a socket for communication, its file descriptor id is given in the _FUSE_COMMFD variable
# * Forwarding fd 1 and fd 2 ensures to catch process output of the fuse process
# * watch-bus ensures when the flatpak exits, also this process is killed
exec flatpak-spawn --host --watch-bus --forward-fd=1 --forward-fd=2 --env=_FUSE_COMMFD=${_FUSE_COMMFD} --forward-fd=${_FUSE_COMMFD} fusermount3 "$@"
