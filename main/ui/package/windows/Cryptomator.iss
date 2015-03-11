;This file will be executed next to the application bundle image
;I.e. current directory will contain folder APPLICATION_NAME with application files
[Setup]
AppId={{PRODUCT_APP_IDENTIFIER}}
AppName=APPLICATION_NAME
AppVersion=APPLICATION_VERSION
AppVerName=APPLICATION_NAME APPLICATION_VERSION
AppPublisher=APPLICATION_VENDOR
AppComments=APPLICATION_COMMENTS
AppCopyright=APPLICATION_COPYRIGHT
AppPublisherURL=https://cryptomator.org/
;AppSupportURL=http://java.com/
;AppUpdatesURL=http://java.com/
DefaultDirName=APPLICATION_INSTALL_ROOT\APPLICATION_NAME
DisableStartupPrompt=Yes
DisableDirPage=No
DisableProgramGroupPage=Yes
DisableReadyPage=Yes
DisableFinishedPage=No
DisableWelcomePage=Yes
DefaultGroupName=APPLICATION_GROUP
;Optional License
LicenseFile=APPLICATION_LICENSE_FILE
;WinXP or above
MinVersion=0,5.1
OutputBaseFilename=INSTALLER_FILE_NAME
Compression=lzma
SolidCompression=yes
PrivilegesRequired=admin
SetupIconFile=APPLICATION_NAME\APPLICATION_NAME.ico
UninstallDisplayIcon={app}\APPLICATION_NAME.ico
UninstallDisplayName=APPLICATION_NAME
WizardImageStretch=No
WizardSmallImageFile=Cryptomator-setup-icon.bmp
WizardImageBackColor=$ffffff
ArchitecturesInstallIn64BitMode=ARCHITECTURE_BIT_MODE

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Registry]
;Root: HKCU; Subkey: "Software\Microsoft\Windows\CurrentVersion\Internet Settings"; ValueType: dword; ValueName: "AutoDetect"; ValueData: "0"
Root: HKLM; Subkey: "SYSTEM\CurrentControlSet\Services\WebClient\Parameters"; ValueType: dword; ValueName: "FileSizeLimitInBytes"; ValueData: "$ffffffff"

[Files]
Source: "APPLICATION_NAME\APPLICATION_NAME.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "APPLICATION_NAME\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\APPLICATION_NAME"; Filename: "{app}\APPLICATION_NAME.exe"; IconFilename: "{app}\APPLICATION_NAME.ico"; Check: APPLICATION_MENU_SHORTCUT()
Name: "{commondesktop}\APPLICATION_NAME"; Filename: "{app}\APPLICATION_NAME.exe";  IconFilename: "{app}\APPLICATION_NAME.ico"; Check: APPLICATION_DESKTOP_SHORTCUT()

[Run]
Filename: "{app}\RUN_FILENAME.exe"; Description: "{cm:LaunchProgram,APPLICATION_NAME}"; Flags: nowait postinstall skipifsilent; Check: APPLICATION_NOT_SERVICE()
Filename: "{app}\RUN_FILENAME.exe"; Parameters: "-install -svcName ""APPLICATION_NAME"" -svcDesc ""APPLICATION_DESCRIPTION"" -mainExe ""APPLICATION_LAUNCHER_FILENAME"" START_ON_INSTALL RUN_AT_STARTUP"; Check: APPLICATION_SERVICE()
Filename: "net"; Parameters: "stop webclient"; Description: "Stopping WebClient..."; Flags: waituntilterminated runhidden
Filename: "net"; Parameters: "start webclient"; Description: "Restarting WebClient..."; Flags: waituntilterminated runhidden

[UninstallRun]
Filename: "{app}\RUN_FILENAME.exe "; Parameters: "-uninstall -svcName APPLICATION_NAME STOP_ON_UNINSTALL"; Check: APPLICATION_SERVICE()

[Code]
function returnTrue(): Boolean;
begin
  Result := True;
end;

function returnFalse(): Boolean;
begin
  Result := False;
end;

function InitializeSetup(): Boolean;
begin
// Possible future improvements:
//   if version less or same => just launch app
//   if upgrade => check if same app is running and wait for it to exit
//   Add pack200/unpack200 support?
  Result := True;
end;
