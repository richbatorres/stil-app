@echo off
set "JAVAC=C:\Users\ZBARTIN\.gradle\jdks\eclipse_adoptium-21-amd64-windows.2\bin\javac.exe"
set "JAVA=C:\Users\ZBARTIN\.gradle\jdks\eclipse_adoptium-21-amd64-windows.2\bin\java.exe"
set "SQLITE=C:\Users\ZBARTIN\.gradle\caches\modules-2\files-2.1\org.xerial\sqlite-jdbc\3.47.1.0\c49b2969ec5a6ac6b34857401d184a2d1082c393\sqlite-jdbc-3.47.1.0.jar"
set "OUT=test-out\main"
set "SRC=app\src\main\java\stil\app"

echo Kompajliranje svih klasa...
"%JAVAC%" -d "%OUT%" -cp "%SQLITE%;%OUT%" ^
  "%SRC%\model\Artikl.java" ^
  "%SRC%\model\StavkaRacuna.java" ^
  "%SRC%\model\Racun.java" ^
  "%SRC%\model\Config.java" ^
  "%SRC%\model\Dobavljac.java" ^
  "%SRC%\model\PovratRobe.java" ^
  "%SRC%\model\Nabava.java" ^
  "%SRC%\model\IzvjestajPodaci.java" ^
  "%SRC%\util\CryptoUtil.java" ^
  "%SRC%\util\Validator.java" ^
  "%SRC%\fisk\FiskalizacijaZahtjev.java" ^
  "%SRC%\fisk\ZkiKalkulator.java" ^
  "%SRC%\fisk\FiskXmlGraditelj.java" ^
  "%SRC%\fisk\FiskalizacijaServis.java" ^
  "%SRC%\db\DatabaseManager.java" ^
  "%SRC%\db\TestDataGenerator.java" ^
  "%SRC%\print\IspisRacuna.java" ^
  "%SRC%\ui\LockScreen.java" ^
  "%SRC%\ui\ArtiklForm.java" ^
  "%SRC%\ui\ArtikliPanel.java" ^
  "%SRC%\ui\DobavljacForm.java" ^
  "%SRC%\ui\DobavljaciPanel.java" ^
  "%SRC%\ui\PovratRobePanel.java" ^
  "%SRC%\ui\NabavaPanel.java" ^
  "%SRC%\ui\IzvjestajiPanel.java" ^
  "%SRC%\ui\PostavkePanel.java" ^
  "%SRC%\ui\ProdajaPanel.java" ^
  "%SRC%\ui\MainWindow.java" ^
  "%SRC%\App.java"
if %errorlevel% neq 0 (
  echo GRESKA pri kompajliranju!
  pause
  exit /b 1
)

echo Pokretanje aplikacije...
"%JAVA%" -cp "%OUT%;%SQLITE%" stil.app.App
