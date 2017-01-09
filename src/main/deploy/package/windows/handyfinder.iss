;This file will be executed next to the application bundle image
;I.e. current directory will contain folder handyfinder with application files
[Setup]
SignTool=signtool
AppId={{0.104}}
AppName=handyfinder
AppVersion=0.104
AppVerName=handyfinder 0.104
AppPublisher=Handyfinder
AppComments=handyfinder
AppCopyright=Copyright (C) 2016
AppPublisherURL=https://github.com/qwefgh90/handyfinder/releases
AppSupportURL=https://github.com/qwefgh90/handyfinder/releases
AppUpdatesURL=https://github.com/qwefgh90/handyfinder/releases
DefaultDirName={localappdata}\handyfinder
DisableStartupPrompt=Yes
DisableDirPage=No
DisableProgramGroupPage=Yes
DisableReadyPage=Yes
DisableFinishedPage=Yes
DisableWelcomePage=Yes
DefaultGroupName=Handyfinder
InfoAfterFile=..\AFTER
;Optional License
LicenseFile=LICENSE
;WinXP or above
MinVersion=0,5.1 
OutputBaseFilename=handyfinder
Compression=lzma
SolidCompression=yes
PrivilegesRequired=lowest
SetupIconFile=handyfinder\handyfinder.ico
UninstallDisplayIcon={app}\handyfinder.ico
UninstallDisplayName=handyfinder
WizardImageStretch=No
WizardSmallImageFile=handyfinder-setup-icon.bmp   
ArchitecturesInstallIn64BitMode=x64


[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Files]
Source: "handyfinder\handyfinder.exe"; DestDir: "{app}"; Flags: ignoreversion sign 
Source: "handyfinder\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs   overwritereadonly  

[Icons]
Name: "{group}\handyfinder"; Filename: "{app}\handyfinder.exe"; IconFilename: "{app}\handyfinder.ico"; Check: returnTrue()
Name: "{commondesktop}\handyfinder"; Filename: "{app}\handyfinder.exe";  IconFilename: "{app}\handyfinder.ico"; Check: returnTrue()


[Run]
Filename: "{app}\handyfinder.exe"; Parameters: "-Xappcds:generatecache"; Check: returnFalse()
Filename: "{app}\handyfinder.exe"; Description: "{cm:LaunchProgram,handyfinder}"; Flags: nowait postinstall skipifsilent; Check: returnTrue()
Filename: "{app}\handyfinder.exe"; Parameters: "-install -svcName ""handyfinder"" -svcDesc ""handyfinder"" -mainExe ""handyfinder.exe""  "; Check: returnFalse()

[UninstallRun]
Filename: "{app}\handyfinder.exe "; Parameters: "-uninstall -svcName handyfinder -stopOnUninstall"; Check: returnFalse()

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