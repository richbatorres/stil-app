# Pravilo: Testiranje nakon svake izmjene

Nakon svake izmjene funkcionalnosti (nova klasa, nova metoda, promjena logike):
- Napiši unit testove za sve nove javne metode i modele
- Napiši integracijske testove za sve nove DB operacije u DatabaseManagerTest
- Napiši robusne testove: null vrijednosti, prazni skupovi, granični slučajevi, rollback pri grešci
- Ažuriraj `run-tests.bat` da uključuje sve nove test klase
- Pokreni testove i provjeri da svi prolaze prije nego što prijaviš zadatak završenim
- Svaki test mora biti deterministički — ne smije ovisiti o redoslijedu izvršavanja
