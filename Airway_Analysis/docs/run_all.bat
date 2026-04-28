@echo off
set BASE_PATH=C:\Users\USER\Desktop\AeroPath\AeroPath

for /L %%i in (1,1,27) do (
    echo Processing case %%i...
    java -Xmx6g --module-path "C:\javafx-sdk-21\lib" --add-modules javafx.controls -cp bin model.CaseProcessor %%i %BASE_PATH%
    echo Case %%i done.
    timeout /t 2
)

echo ALL DONE
pause