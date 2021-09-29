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
	GTK_FLAG="-Djdk.gtk.version=2"
fi

# workaround for https://github.com/cryptomator/cryptomator-linux/issues/27
export LD_PRELOAD=lib/app/libjffi.so

# start Cryptomator
./lib/runtime/bin/java \
	-p "lib/app/mods" \
	-cp "lib/app/*" \
	-Dfile.encoding="utf-8" \
	-Dcryptomator.logDir="~/.local/share/Cryptomator/logs" \
	-Dcryptomator.pluginDir="~/.local/share/Cryptomator/plugins" \
	-Dcryptomator.mountPointsDir="~/.local/share/Cryptomator/mnt" \
	-Dcryptomator.settingsPath="~/.config/Cryptomator/settings.json:~/.Cryptomator/settings.json" \
	-Dcryptomator.ipcSocketPath="~/.config/Cryptomator/ipc.socket" \
	-Dcryptomator.buildNumber="appimage-${REVISION_NO}" \
	-Dcryptomator.appVersion="${SEMVER_STR}" \
	$GTK_FLAG \
	-Xss5m \
	-Xmx256m \
	-m org.cryptomator.desktop/org.cryptomator.launcher.Cryptomator
