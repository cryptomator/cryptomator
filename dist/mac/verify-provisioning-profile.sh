#!/bin/bash
if [ $# -ne 3 ]; then echo "Usage: $0 <provisioning-profile> <application-identifier> <entitlement>" 1>&2; exit 1; fi

PROFILE_PLIST=$(security cms -D -i "$1") || exit 1
PROFILE_APP_IDENTIFIER=$(plutil -extract 'Entitlements.com\.apple\.application-identifier' raw -o - - <<< "${PROFILE_PLIST}" 2>/dev/null)
PROFILE_ENTITLEMENT=$(plutil -extract "Entitlements.${3//./\\.}" raw -o - - <<< "${PROFILE_PLIST}" 2>/dev/null)
if [[ "${PROFILE_APP_IDENTIFIER}" != "$2" ]] || [[ "${PROFILE_ENTITLEMENT}" != "true" ]]; then
    >&2 echo "Provisioning profile $1 is not for $2 with $3"
    exit 1
fi
