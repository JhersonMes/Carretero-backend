package com.carretero.service.billing;

import com.carretero.config.SunatProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Firma el XML del comprobante con XML-DSig, dentro de la extension UBL.
 *
 * Usa la implementacion que ya trae el JDK (javax.xml.crypto.dsig): no hace
 * falta Apache Santuario ni ninguna libreria externa, y una dependencia menos es
 * una dependencia menos que actualizar cuando salga su proxima vulnerabilidad.
 *
 * Los parametros no son negociables porque los fija SUNAT: canonicalizacion C14N
 * inclusiva, RSA-SHA1, y una unica referencia con URI vacia y transformada
 * "enveloped", que equivale a firmar el documento entero menos la propia firma.
 */
@Component
@RequiredArgsConstructor
public class UblSigner {

    private static final String NS_EXT =
            "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2";

    private final SunatProperties properties;

    /** Clave privada y certificado con los que se firma. */
    public record SigningMaterial(PrivateKey key, X509Certificate certificate) {
    }

    /**
     * Firma el documento en el sitio.
     *
     * @throws IllegalStateException si el documento no trae el hueco de la
     *                               extension, o si la firma no se puede generar
     */
    public void sign(Document document, SigningMaterial material) {
        try {
            Node target = extensionContent(document);

            XMLSignatureFactory factory = XMLSignatureFactory.getInstance("DOM");

            Reference reference = factory.newReference(
                    "",
                    factory.newDigestMethod(DigestMethod.SHA1, null),
                    Collections.singletonList(
                            factory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)),
                    null, null);

            SignedInfo signedInfo = factory.newSignedInfo(
                    factory.newCanonicalizationMethod(
                            CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
                    factory.newSignatureMethod(SignatureMethod.RSA_SHA1, null),
                    Collections.singletonList(reference));

            KeyInfoFactory keyInfoFactory = factory.getKeyInfoFactory();
            KeyInfo keyInfo = keyInfoFactory.newKeyInfo(Collections.singletonList(
                    keyInfoFactory.newX509Data(List.of(material.certificate()))));

            DOMSignContext context = new DOMSignContext(material.key(), target);
            XMLSignature signature = factory.newXMLSignature(
                    signedInfo, keyInfo, null, UblInvoiceBuilder.SIGNATURE_ID, null);
            signature.sign(context);

        } catch (Exception e) {
            throw new IllegalStateException("No se pudo firmar el comprobante: " + e.getMessage(), e);
        }
    }

    /**
     * Lee el certificado configurado.
     *
     * Para el ambiente beta sirve uno autofirmado; SUNAT no valida la cadena ahi.
     * Para produccion tiene que ser el certificado tributario vigente del cliente.
     *
     * @throws IllegalStateException si no hay certificado configurado o no se
     *                               puede abrir. Es una falla de instalacion, y
     *                               tiene que detener la emision en vez de dejar
     *                               pasar comprobantes sin firmar
     */
    public SigningMaterial loadMaterial() {
        String path = properties.getCertificatePath();
        if (path == null || path.isBlank()) {
            throw new IllegalStateException(
                    "No hay certificado configurado (carretero.sunat.certificate-path). "
                            + "Sin certificado no se puede firmar ni declarar.");
        }

        Path file = Path.of(path);
        if (!Files.exists(file)) {
            throw new IllegalStateException("No se encontro el certificado en " + file.toAbsolutePath());
        }

        char[] password = properties.getCertificatePassword() == null
                ? new char[0]
                : properties.getCertificatePassword().toCharArray();

        try (InputStream in = Files.newInputStream(file)) {
            KeyStore store = KeyStore.getInstance("PKCS12");
            store.load(in, password);

            // Se toma el primer alias con clave privada: un .pfx de emision trae
            // uno solo, y pedirle al instalador que ademas configure el alias
            // correcto es una forma barata de que la instalacion falle.
            Enumeration<String> aliases = store.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (!store.isKeyEntry(alias)) {
                    continue;
                }
                PrivateKey key = (PrivateKey) store.getKey(alias, password);
                X509Certificate certificate = (X509Certificate) store.getCertificate(alias);
                if (key != null && certificate != null) {
                    return new SigningMaterial(key, certificate);
                }
            }
            throw new IllegalStateException("El certificado " + file + " no contiene ninguna clave privada.");

        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "No se pudo abrir el certificado " + file + ": " + e.getMessage(), e);
        }
    }

    private Node extensionContent(Document document) {
        NodeList nodes = document.getElementsByTagNameNS(NS_EXT, "ExtensionContent");
        if (nodes.getLength() == 0) {
            throw new IllegalStateException(
                    "El XML no tiene ext:ExtensionContent, que es donde va la firma.");
        }
        return nodes.item(0);
    }
}
