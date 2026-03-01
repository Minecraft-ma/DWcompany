@echo off
echo Checking compilation without external dependencies...

REM Create target directory
if not exist target\classes mkdir target\classes

REM Try to compile with basic Java only
javac -d target\classes -cp "src\main\java" src\main\java\fr\dominatuin\dwcompany\Company.java 2>compile_errors.txt
if %ERRORLEVEL% NEQ 0 (
    echo Company.java compilation failed
    type compile_errors.txt
    pause
    exit /b 1
)

echo Company.java compiled successfully

javac -d target\classes -cp "src\main\java;target\classes" src\main\java\fr\dominatuin\dwcompany\CompanyCommandExecutor.java 2>>compile_errors.txt
if %ERRORLEVEL% NEQ 0 (
    echo CompanyCommandExecutor.java compilation failed
    type compile_errors.txt
    pause
    exit /b 1
)

echo CompanyCommandExecutor.java compiled successfully
echo All files compiled successfully!
pause
