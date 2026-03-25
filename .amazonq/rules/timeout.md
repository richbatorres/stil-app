# Pravilo: Timeout za naredbe

- Svaku `executeBash` naredbu koja može trajati dulje pokreni s eksplicitnim timeoutom
- Sintaksa za Windows: `cmd /c "cd /d <dir> && <naredba>"` — cmd /c automatski vraća kontrolu
- Za pokretanje testova koristi `run-tests.bat` koji je već optimiziran za brzo izvršavanje
- Ako naredba ne vrati rezultat u razumnom vremenu (30s kratke, 60s testovi), prekini i prijavi grešku
- NIKAD ne čekaj beskonačno — ako nema odgovora, prijavi korisniku i predloži alternativu
- Ne ponavljaj automatski naredbu koja je istekla — prvo dijagnosticiraj uzrok
