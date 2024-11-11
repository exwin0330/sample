@echo off
REM 引数が渡されているかを確認
if "%1"=="" (
    echo Usage: run.bat HelloSoot
    exit /b
)

REM 引数に基づいてファイル名を設定
set "filename=%~1"

set filepath=""
set classpath=""

REM 有効なファイル名のチェック
if "%1"=="mc" (
    set "filepath=src\main\java\com\example\sample\flow\MethodCallAnalysis.java src\main\java\com\example\sample\flow\AnalysisGraph.java"
    set classpath="com.example.sample.flow.MethodCallAnalysis"
) else (
    echo %filename% is invalid filename. Please use one of the following:
    type valid_filenames.txt
    exit /b
)

REM 指定されたJavaファイルをコンパイル
echo Compiling %filepath%...
"c:\Program Files\Java\jdk1.8.0_202\bin\javac.exe" -encoding utf-8 -cp libs\soot-4.5.0-jar-with-dependencies.jar -d bin %filepath%
if %errorlevel% neq 0 (
    echo Failed to compile %filename%.java.
    exit /b
)
echo Running %filename%...
set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8
"c:\Program Files\Java\jdk1.8.0_202\bin\java.exe" -cp bin;libs\soot-4.5.0-jar-with-dependencies.jar %classpath%

exit /b