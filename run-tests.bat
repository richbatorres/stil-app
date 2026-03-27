@echo off
set JAVAC=C:\Users\ZBARTIN\.gradle\jdks\eclipse_adoptium-21-amd64-windows.2\bin\javac.exe
set JAVA=C:\Users\ZBARTIN\.gradle\jdks\eclipse_adoptium-21-amd64-windows.2\bin\java.exe

set SQLITE=C:\Users\ZBARTIN\.gradle\caches\modules-2\files-2.1\org.xerial\sqlite-jdbc\3.47.1.0\c49b2969ec5a6ac6b34857401d184a2d1082c393\sqlite-jdbc-3.47.1.0.jar
set PDFBOX=app\libs\pdfbox-3.0.3.jar;app\libs\pdfbox-io-3.0.3.jar;app\libs\fontbox-3.0.3.jar;app\libs\commons-logging-1.3.3.jar
set JUNIT_API=C:\Users\ZBARTIN\.gradle\caches\modules-2\files-2.1\org.junit.jupiter\junit-jupiter-api\5.12.1\9ed9e01546d4eb0840769b5d0ac7924531e98d7d\junit-jupiter-api-5.12.1.jar
set JUNIT_ENGINE=C:\Users\ZBARTIN\.gradle\caches\modules-2\files-2.1\org.junit.jupiter\junit-jupiter-engine\5.12.1\fec974a323cb74d080b7801abba2e79e01cadbfe\junit-jupiter-engine-5.12.1.jar
set JUNIT_PARAMS=C:\Users\ZBARTIN\.gradle\caches\modules-2\files-2.1\org.junit.jupiter\junit-jupiter-params\5.12.1\a879b3b05785b66201e30709e7c61662b269cbd1\junit-jupiter-params-5.12.1.jar
set PLATFORM_COMMONS=C:\Users\ZBARTIN\.gradle\caches\modules-2\files-2.1\org.junit.platform\junit-platform-commons\1.12.1\d34adac31a6974eec6611cc129927a2e2cfe89ee\junit-platform-commons-1.12.1.jar
set PLATFORM_ENGINE=C:\Users\ZBARTIN\.gradle\caches\modules-2\files-2.1\org.junit.platform\junit-platform-engine\1.12.1\fc105f3430d6392a4d033fe6f9d58eddd1a03594\junit-platform-engine-1.12.1.jar
set PLATFORM_LAUNCHER=C:\Users\ZBARTIN\.gradle\caches\modules-2\files-2.1\org.junit.platform\junit-platform-launcher\1.12.1\ff5af5e325705fb1d5926a0235cc655c4e96bf1d\junit-platform-launcher-1.12.1.jar
set OPENTEST4J=C:\Users\ZBARTIN\.gradle\caches\modules-2\files-2.1\org.opentest4j\opentest4j\1.3.0\152ea56b3a72f655d4fd677fc0ef2596c3dd5e6e\opentest4j-1.3.0.jar

set CP=%SQLITE%;%PDFBOX%;%JUNIT_API%;%JUNIT_ENGINE%;%JUNIT_PARAMS%;%PLATFORM_COMMONS%;%PLATFORM_ENGINE%;%PLATFORM_LAUNCHER%;%OPENTEST4J%
set OUT=test-out\main
set TEST_OUT=test-out\test
set RUNNER_OUT=test-out\runner

echo [1/3] Kompajliranje main klasa...
%JAVAC% -d %OUT% -cp "%CP%" ^
  app\src\main\java\stil\app\model\Artikl.java ^
  app\src\main\java\stil\app\model\StavkaRacuna.java ^
  app\src\main\java\stil\app\model\Racun.java ^
  app\src\main\java\stil\app\model\Config.java ^
  app\src\main\java\stil\app\model\Dobavljac.java ^
  app\src\main\java\stil\app\model\PovratRobe.java ^
  app\src\main\java\stil\app\model\Nabava.java ^
  app\src\main\java\stil\app\model\IzvjestajPodaci.java ^
  app\src\main\java\stil\app\model\KomisijaStavka.java ^
  app\src\main\java\stil\app\model\Zaklucnica.java ^
  app\src\main\java\stil\app\util\CryptoUtil.java ^
  app\src\main\java\stil\app\util\Validator.java ^
  app\src\main\java\stil\app\fisk\FiskalizacijaZahtjev.java ^
  app\src\main\java\stil\app\fisk\ZkiKalkulator.java ^
  app\src\main\java\stil\app\fisk\FiskXmlGraditelj.java ^
  app\src\main\java\stil\app\fisk\FiskalizacijaServis.java ^
  app\src\main\java\stil\app\db\DatabaseManager.java ^
  app\src\main\java\stil\app\ui\FontManager.java ^
  app\src\main\java\stil\app\print\IspisIzvjestaja.java ^
  app\src\main\java\stil\app\print\IspisRacuna.java
if %errorlevel% neq 0 (echo GRESKA main && exit /b 1)

echo [2/3] Kompajliranje test klasa...
%JAVAC% -d %TEST_OUT% -cp "%CP%;%OUT%" ^
  app\src\test\java\stil\app\AppTest.java ^
  app\src\test\java\stil\app\model\ArtiklTest.java ^
  app\src\test\java\stil\app\model\StavkaRacunaTest.java ^
  app\src\test\java\stil\app\model\RacunTest.java ^
  app\src\test\java\stil\app\model\ConfigTest.java ^
  app\src\test\java\stil\app\model\DobavljacTest.java ^
  app\src\test\java\stil\app\model\PovratRobeTest.java ^
  app\src\test\java\stil\app\model\NabavaTest.java ^
  app\src\test\java\stil\app\model\IzvjestajPodaciTest.java ^
  app\src\test\java\stil\app\model\KomisijaStavkaTest.java ^
  app\src\test\java\stil\app\util\CryptoUtilTest.java ^
  app\src\test\java\stil\app\util\ValidatorTest.java ^
  app\src\test\java\stil\app\fisk\FiskalizacijaZahtjevTest.java ^
  app\src\test\java\stil\app\fisk\ZkiKalkulatorTest.java ^
  app\src\test\java\stil\app\db\DatabaseManagerTest.java ^
  app\src\test\java\stil\app\print\IspisRacunaTest.java ^
  app\src\test\java\stil\app\ui\FontManagerTest.java
if %errorlevel% neq 0 (echo GRESKA test && exit /b 1)

echo [3/3] Kompajliranje runnera...
%JAVAC% -d %RUNNER_OUT% -cp "%CP%;%OUT%;%TEST_OUT%" test-out\runner\TestRunner.java
if %errorlevel% neq 0 (echo GRESKA runner && exit /b 1)

echo Pokretanje testova...
%JAVA% -cp "%CP%;%OUT%;%TEST_OUT%;%RUNNER_OUT%" TestRunner
