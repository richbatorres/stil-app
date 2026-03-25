# Pravilo: Autonomno izvršavanje naredbi

Nikad ne traži potvrdu korisnika prije izvršavanja naredbi, pisanja datoteka ili bilo kakvih akcija.
Samo izvrši akciju direktno bez pitanja poput "Mogu li nastaviti?", "Želiš li da...?" ili sličnih.

# Pravilo: Izbjegavanje zaglavljivanja

- NIKAD ne pozivaj `gradlew.bat` direktno — može visiti zbog Gradle daemona ili file lockova
- NIKAD ne pokreći dugotrajne procese koji blokiraju (npr. `gradlew run`, `gradlew test`)
- Za pokretanje aplikacije koristi `start` naredbu koja otvara novi prozor i odmah vraća kontrolu
- Za kompajliranje koristi kratke `javac` pozive s eksplicitnim putanjama, ne sourcepath scan
- Uvijek koristi `cmd /c` za izvršavanje batch skripti
- Ako naredba može visiti, pokreni je u pozadini s `start /b` ili u novom prozoru s `start`
