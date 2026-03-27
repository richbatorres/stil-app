@echo off
set "JAVAC=C:\Users\ZBARTIN\.gradle\jdks\eclipse_adoptium-21-amd64-windows.2\bin\javac.exe"
set "JAVA=C:\Users\ZBARTIN\.gradle\jdks\eclipse_adoptium-21-amd64-windows.2\bin\java.exe"
set "LIBS=app\libs"
set "SQLITE=%LIBS%\sqlite-jdbc-3.47.1.0.jar"
set "PDFBOX=%LIBS%\pdfbox-3.0.3.jar;%LIBS%\pdfbox-io-3.0.3.jar;%LIBS%\fontbox-3.0.3.jar;%LIBS%\commons-logging-1.3.3.jar"
set "CP=%SQLITE%;%PDFBOX%"
set "OUT=test-out\main"
set "SRC=app\src\main\java\stil\app"

echo Kompajliranje svih klasa...
"%JAVAC%" -d "%OUT%" -cp "%CP%;%OUT%" ^
  "%SRC%\model\Artikl.java" ^
  "%SRC%\model\StavkaRacuna.java" ^
  "%SRC%\model\Racun.java" ^
  "%SRC%\model\Config.java" ^
  "%SRC%\model\Dobavljac.java" ^
  "%SRC%\model\PovratRobe.java" ^
  "%SRC%\model\Nabava.java" ^
  "%SRC%\model\IzvjestajPodaci.java" ^
  "%SRC%\model\KomisijaStavka.java" ^
  "%SRC%\model\Zaklucnica.java" ^
  "%SRC%\util\CryptoUtil.java" ^
  "%SRC%\util\Validator.java" ^
  "%SRC%\fisk\FiskalizacijaZahtjev.java" ^
  "%SRC%\fisk\ZkiKalkulator.java" ^
  "%SRC%\fisk\FiskXmlGraditelj.java" ^
  "%SRC%\fisk\FiskalizacijaServis.java" ^
  "%SRC%\db\DatabaseManager.java" ^
  "%SRC%\db\TestDataGenerator.java" ^
  "%SRC%\print\IspisRacuna.java" ^
  "%SRC%\print\IspisIzvjestaja.java" ^
  "%SRC%\ui\FontManager.java" ^
  "%SRC%\ui\LockScreen.java" ^
  "%SRC%\ui\ArtiklForm.java" ^
  "%SRC%\ui\ArtikliPanel.java" ^
  "%SRC%\ui\DobavljacForm.java" ^
  "%SRC%\ui\DobavljaciPanel.java" ^
  "%SRC%\ui\PovratRobePanel.java" ^
  "%SRC%\ui\NabavaPanel.java" ^
  "%SRC%\ui\IzvjestajiPanel.java" ^
  "%SRC%\ui\PostavkePanel.java" ^
  "%SRC%\ui\KomisijaPanel.java" ^
  "%SRC%\ui\ProdajaPanel.java" ^
  "%SRC%\ui\MainWindow.java" ^
  "%SRC%\App.java"
if %errorlevel% neq 0 (
  echo GRESKA pri kompajliranju!
  pause
  exit /b 1
)

echo Pokretanje aplikacije...
"%JAVA%" -cp "%OUT%;%CP%" stil.app.App
