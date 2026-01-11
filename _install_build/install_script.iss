; Script de Instalación para Sprite Lab
; Generado para FedeiaTech

#define MyAppName "Sprite Lab"
#define MyAppVersion "0.2"
#define MyAppPublisher "FedeiaTech"
#define MyAppURL "https://fedeiatech.itch.io/spritelab"
#define MyAppExeName "SpriteLab.jar"

[Setup]
; --- Identificación y Diseño ---
AppId={{A1B2C3D4-E5F6-7890-ABCD-1234567890AB}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}

; --- LICENCIA (¡NUEVO!) ---
; Esto mostrará la pantalla de "Acepto" al instalar
LicenseFile=D:\Documentos\PROGRAMACION\.PROGRAMAS\CREADOS\SpriteLab\_install_build\LICENSE.txt

; --- Ubicación de Instalación ---
DefaultDirName={autopf}\{#MyAppPublisher}\{#MyAppName}
DefaultGroupName={#MyAppPublisher}
DisableProgramGroupPage=yes

; --- Configuración del Archivo de Salida ---
OutputDir=.
OutputBaseFilename=Instalar_SpriteLab_v0.2
SetupIconFile=D:\Documentos\PROGRAMACION\.PROGRAMAS\CREADOS\SpriteLab\_install_build\app_icon.ico
Compression=lzma2/ultra64
SolidCompression=yes
WizardStyle=modern
ArchitecturesInstallIn64BitMode=x64

[Languages]
Name: "spanish"; MessagesFile: "compiler:Languages\Spanish.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
; 1. El Archivo JAR Principal (FAT JAR)
Source: "D:\Documentos\PROGRAMACION\.PROGRAMAS\CREADOS\SpriteLab\target\SpriteLab.jar"; DestDir: "{app}"; Flags: ignoreversion

; 2. El Icono
Source: "D:\Documentos\PROGRAMACION\.PROGRAMAS\CREADOS\SpriteLab\_install_build\app_icon.ico"; DestDir: "{app}"; Flags: ignoreversion

; 3. La Licencia (¡NUEVO!) - Se copia a la carpeta de instalación
Source: "D:\Documentos\PROGRAMACION\.PROGRAMAS\CREADOS\SpriteLab\_install_build\LICENSE.txt"; DestDir: "{app}"; Flags: ignoreversion

; 4. La carpeta BIN con FFmpeg
Source: "D:\Documentos\PROGRAMACION\.PROGRAMAS\CREADOS\SpriteLab\bin\*"; DestDir: "{app}\bin"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
; Acceso directo en el Menú Inicio (Usa javaw.exe para ocultar consola)
Name: "{group}\{#MyAppName}"; Filename: "javaw.exe"; Parameters: "-jar ""{app}\{#MyAppExeName}"""; IconFilename: "{app}\app_icon.ico"; WorkingDir: "{app}"

; Acceso directo en el Escritorio (Usa javaw.exe para ocultar consola)
Name: "{commondesktop}\{#MyAppName}"; Filename: "javaw.exe"; Parameters: "-jar ""{app}\{#MyAppExeName}"""; IconFilename: "{app}\app_icon.ico"; Tasks: desktopicon; WorkingDir: "{app}"

[Run]
; Ejecutar al finalizar (Usa javaw.exe)
Filename: "javaw.exe"; Parameters: "-jar ""{app}\{#MyAppExeName}"""; Description: "{cm:LaunchProgram,{#MyAppName}}"; Flags: nowait postinstall skipifsilent