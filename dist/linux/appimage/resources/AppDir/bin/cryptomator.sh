#!/bin/sh
cd $(dirname $0)

# determine GTK version
GTK2_PRESENT=1 # initially false
GTK3_PRESENT=0 # initially true
if command -v dpkg &> /dev/null; then # do stuff for debian based things
	GTK2_PRESENT=`dpkg -l libgtk* | grep -e '\^ii' | grep -e 'libgtk2-*' &> /dev/null; echo $?`
	GTK3_PRESENT=`dpkg -l libgtk* | grep -e '\^ii' | grep -e 'libgtk-3-*' &> /dev/null; echo $?`
elif command -v rpm &> /dev/null; then # do stuff for rpm based things (including yum/dnf)
	GTK2_PRESENT=`rpm -qa | grep -e '\^gtk2-[0-9][0-9]*' &> /dev/null; echo $?`
	GTK3_PRESENT=`rpm -qa | grep -e '\^gtk3-[0-9][0-9]*' &> /dev/null; echo $?`
elif command -v pacman &> /dev/null; then # don't forget arch
	GTK2_PRESENT=`pacman -Qi gtk2 &> /dev/null; echo $?`
	GTK3_PRESENT=`pacman -Qi gtk3 &> /dev/null; echo $?`
fi

if [ "$GTK2_PRESENT" -eq 0 ] && [ "$GTK3_PRESENT" -ne 0 ]; then
	bin/Cryptomator-gtk2 $@
else
	bin/Cryptomator $@
fi