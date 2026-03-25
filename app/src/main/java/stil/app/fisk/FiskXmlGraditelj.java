package stil.app.fisk;

import org.w3c.dom.*;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.*;
import javax.xml.crypto.dsig.spec.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.UUID;

/**
 * Gradi i digitalno potpisuje SOAP XML poruku za Fina fiskalizacijski servis.
 *
 * Struktura poruke:
 * <pre>
 * soapenv:Envelope
 *   soapenv:Header
 *   soapenv:Body
 *     tns:RacunZahtjev (Id="RacunZahtjev" — referenca za XMLDSig potpis)
 *       tns:Zaglavlje
 *         tns:IdPoruke   (UUID)
 *         tns:DatumVrijeme
 *       tns:Racun
 *         tns:Oib, tns:USustPdv, tns:DatVrijeme, tns:OznSlijed
 *         tns:BrRac (tns:BrOznRac, tns:OznPosPr, tns:OznNapUr)
 *         tns:IznosUkupno, tns:NacinPlac, tns:OibOper, tns:ZastKod, tns:NakDost
 *         tns:Pdv
 *           tns:Porez (tns:Stopa, tns:Osnovica, tns:Iznos)
 *       ds:Signature (XMLDSig enveloped potpis)
 * </pre>
 *
 * Potpis koristi:
 * - Kanonizacija: Inclusive C14N
 * - Algoritam potpisa: RSA-SHA1
 * - Digest: SHA1
 * - Transform: Enveloped signature
 * - KeyInfo: X509 certifikat
 */
public class FiskXmlGraditelj {

    private static final String NS_TNS = "http://www.apis-it.hr/fin/2012/types/f73";
    private static final String NS_SOAP = "http://schemas.xmlsoap.org/soap/envelope/";

    /**
     * Gradi potpisan SOAP XML string za RacunZahtjev.
     *
     * @param z podaci zahtjeva
     * @param keyStore PKCS12 keystore s certifikatom i privatnim ključem
     * @param alias alias certifikata u keystoreu
     * @param lozinka lozinka keystorea
     * @return potpisan SOAP XML kao String
     * @throws Exception ako izgradnja ili potpisivanje ne uspiju
     */
    public static String izgradiRacunZahtjev(FiskalizacijaZahtjev z, KeyStore keyStore,
            String alias, String lozinka) throws Exception {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder().newDocument();

        // SOAP omotnica
        Element envelope = doc.createElementNS(NS_SOAP, "soapenv:Envelope");
        envelope.setAttribute("xmlns:soapenv", NS_SOAP);
        envelope.setAttribute("xmlns:tns", NS_TNS);
        doc.appendChild(envelope);

        envelope.appendChild(doc.createElementNS(NS_SOAP, "soapenv:Header"));

        Element body = doc.createElementNS(NS_SOAP, "soapenv:Body");
        envelope.appendChild(body);

        // Tijelo zahtjeva — Id atribut je referenca za XMLDSig potpis
        Element zahtjev = doc.createElementNS(NS_TNS, "tns:RacunZahtjev");
        zahtjev.setAttribute("Id", "RacunZahtjev");
        body.appendChild(zahtjev);

        // Zaglavlje poruke
        Element zaglavlje = doc.createElementNS(NS_TNS, "tns:Zaglavlje");
        zahtjev.appendChild(zaglavlje);
        dodajElement(doc, zaglavlje, "tns:IdPoruke", UUID.randomUUID().toString());
        dodajElement(doc, zaglavlje, "tns:DatumVrijeme", z.getDatumVrijemePoruke());

        // Podaci računa
        Element racun = doc.createElementNS(NS_TNS, "tns:Racun");
        zahtjev.appendChild(racun);

        dodajElement(doc, racun, "tns:Oib", z.getOib());
        dodajElement(doc, racun, "tns:USustPdv", "true");
        dodajElement(doc, racun, "tns:DatVrijeme", z.getDatumVrijemeRacuna());
        dodajElement(doc, racun, "tns:OznSlijed", "P"); // P = redoslijed po poslovnom prostoru

        // Broj računa (složena struktura)
        Element brRac = doc.createElementNS(NS_TNS, "tns:BrRac");
        racun.appendChild(brRac);
        dodajElement(doc, brRac, "tns:BrOznRac", z.getBrojRacuna());
        dodajElement(doc, brRac, "tns:OznPosPr", z.getOznakaPosProstora());
        dodajElement(doc, brRac, "tns:OznNapUr", z.getOznakaUredaja());

        dodajElement(doc, racun, "tns:IznosUkupno", z.getIznosUkupno());
        dodajElement(doc, racun, "tns:NacinPlac", z.getNacinPlacanja());
        dodajElement(doc, racun, "tns:OibOper", z.getOib()); // za j.d.o.o. OIB operatera = OIB tvrtke
        dodajElement(doc, racun, "tns:ZastKod", z.getZki());
        dodajElement(doc, racun, "tns:NakDost", "false"); // nije naknadna dostava

        // PDV razrada
        Element pdv = doc.createElementNS(NS_TNS, "tns:Pdv");
        racun.appendChild(pdv);
        Element porez = doc.createElementNS(NS_TNS, "tns:Porez");
        pdv.appendChild(porez);
        dodajElement(doc, porez, "tns:Stopa", "25.00");
        dodajElement(doc, porez, "tns:Osnovica", z.getOsnovica());
        dodajElement(doc, porez, "tns:Iznos", z.getIznosPdv());

        // XMLDSig enveloped potpis
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, lozinka.toCharArray());
        X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
        potpisi(doc, zahtjev, privateKey, cert);

        return docToString(doc);
    }

    /**
     * Dodaje XMLDSig enveloped potpis na zadani element dokumenta.
     * Potpis se umeće kao zadnje dijete elementa.
     */
    private static void potpisi(Document doc, Element elementZaPotpis,
            PrivateKey privateKey, X509Certificate cert) throws Exception {

        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

        Reference ref = fac.newReference(
            "#RacunZahtjev",
            fac.newDigestMethod(DigestMethod.SHA1, null),
            Collections.singletonList(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)),
            null, null);

        SignedInfo si = fac.newSignedInfo(
            fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
            fac.newSignatureMethod(SignatureMethod.RSA_SHA1, null),
            Collections.singletonList(ref));

        KeyInfoFactory kif = fac.getKeyInfoFactory();
        X509Data x509Data = kif.newX509Data(Collections.singletonList(cert));
        KeyInfo ki = kif.newKeyInfo(Collections.singletonList(x509Data));

        DOMSignContext dsc = new DOMSignContext(privateKey, elementZaPotpis);
        fac.newXMLSignature(si, ki).sign(dsc);
    }

    /** Kreira XML element s tekstualnim sadržajem i dodaje ga kao dijete parent elementa. */
    private static void dodajElement(Document doc, Element parent, String tag, String value) {
        Element el = doc.createElementNS(NS_TNS, tag);
        el.setTextContent(value);
        parent.appendChild(el);
    }

    /** Pretvara DOM Document u XML String bez XML deklaracije. */
    private static String docToString(Document doc) throws TransformerException {
        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        StringWriter sw = new StringWriter();
        t.transform(new DOMSource(doc), new StreamResult(sw));
        return sw.toString();
    }
}
