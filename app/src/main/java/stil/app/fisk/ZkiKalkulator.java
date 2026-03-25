package stil.app.fisk;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Izračunava ZKI (Zaštitni Kod Izdavatelja) prema Fina specifikaciji za fiskalizaciju.
 *
 * Algoritam (prema Tehničkoj specifikaciji Porezne uprave):
 * 1. Konkatenira se string: OIB + datum_vrijeme + broj_računa + oznaka_pos_prostora + oznaka_uređaja + ukupni_iznos
 * 2. Nad tim stringom se izračuna RSA digitalni potpis koristeći privatni ključ iz certifikata (SHA1withRSA)
 * 3. Nad dobivenim potpisom se izračuna MD5 hash
 * 4. MD5 hash se pretvori u lowercase hex string — to je ZKI
 *
 * ZKI se ispisuje na računu i šalje Fini kao dokaz autentičnosti izdavatelja.
 */
public class ZkiKalkulator {

    /** Format datuma i vremena koji Fina očekuje u ZKI izračunu. */
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    /**
     * Izračunava ZKI za zadane parametre računa.
     *
     * @param oib OIB tvrtke (11 znamenki)
     * @param vrijemeIzdavanja datum i vrijeme izdavanja računa
     * @param brojRacuna redni broj računa (samo numerički dio)
     * @param oznakaPosProstora oznaka poslovnog prostora (npr. "PP1")
     * @param oznakaUredaja oznaka naplatnog uređaja (npr. "1")
     * @param ukupniIznos ukupni iznos računa zaokružen na 2 decimale
     * @param privateKey privatni RSA ključ iz Fina certifikata (.p12)
     * @return ZKI kao 32-znakni lowercase hex string
     * @throws GeneralSecurityException ako RSA potpis ili MD5 hash ne uspiju
     */
    public static String izracunaj(
            String oib,
            LocalDateTime vrijemeIzdavanja,
            String brojRacuna,
            String oznakaPosProstora,
            String oznakaUredaja,
            BigDecimal ukupniIznos,
            PrivateKey privateKey) throws GeneralSecurityException {

        String iznos = ukupniIznos.setScale(2, RoundingMode.HALF_UP).toPlainString();
        String poruka = oib + vrijemeIzdavanja.format(DTF) + brojRacuna + oznakaPosProstora + oznakaUredaja + iznos;

        // Korak 1: RSA potpis nad konkateniranim stringom
        Signature sig = Signature.getInstance("SHA1withRSA");
        sig.initSign(privateKey);
        sig.update(poruka.getBytes(StandardCharsets.UTF_8));
        byte[] potpis = sig.sign();

        // Korak 2: MD5 hash nad RSA potpisom
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] hash = md5.digest(potpis);

        // Korak 3: Pretvorba u hex string
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
