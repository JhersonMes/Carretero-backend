package com.carretero;

import com.carretero.config.SunatProperties;
import com.carretero.model.BusinessConfig;
import com.carretero.model.Client;
import com.carretero.model.Invoice;
import com.carretero.model.Order;
import com.carretero.model.OrderDetail;
import com.carretero.model.enums.DocumentType;
import com.carretero.model.enums.InvoiceType;
import com.carretero.service.billing.AmountInWords;
import com.carretero.service.billing.BillingDocumentStore;
import com.carretero.service.billing.UblInvoiceBuilder;
import com.carretero.service.billing.UblSigner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba la emision hasta donde se puede probar sin SUNAT: que el XML se arme
 * con los importes correctos y que la firma sea verificable.
 *
 * Lo que este test NO prueba, y conviene tenerlo presente: que SUNAT acepte el
 * comprobante. Eso solo se sabe enviandolo al ambiente beta. Aqui se cubre la
 * parte que si es nuestra —la aritmetica del IGV y la mecanica de la firma— para
 * que el dia del primer envio los errores que queden sean de SUNAT y no propios.
 */
class UblEmissionTest {

    private static final Path TEST_CERT = Path.of("src/test/resources/certificado-prueba.pfx");
    private static final String CERT_PASSWORD = "carretero";

    private UblInvoiceBuilder builder;
    private UblSigner signer;
    private BillingDocumentStore store;
    private BusinessConfig config;

    @BeforeEach
    void setUp() {
        builder = new UblInvoiceBuilder();

        SunatProperties properties = new SunatProperties();
        properties.setCertificatePath(TEST_CERT.toString());
        properties.setCertificatePassword(CERT_PASSWORD);
        signer = new UblSigner(properties);
        store = new BillingDocumentStore(properties);

        config = new BusinessConfig();
        config.setBusinessName("EL CARRETERO E.I.R.L.");
        config.setCommercialName("EL CARRETERO");
        config.setRuc("20610046097");
        config.setAddress("Jr. Irene Silva Nro. 183, Urb. Horacio Zevallos - Cajamarca");
    }

    // --- importe en letras ---

    @Test
    void importeEnLetrasSigueElFormatoDeSunat() {
        assertEquals("SON: SESENTA Y CINCO CON 00/100 SOLES",
                AmountInWords.of(new BigDecimal("65.00")));
        assertEquals("SON: CIENTO DIECIOCHO CON 50/100 SOLES",
                AmountInWords.of(new BigDecimal("118.50")));
        // "CIEN" exacto, pero "CIENTO UNO" cuando lleva resto.
        assertEquals("SON: CIEN CON 00/100 SOLES",
                AmountInWords.of(new BigDecimal("100.00")));
        assertEquals("SON: CIENTO UNO CON 90/100 SOLES",
                AmountInWords.of(new BigDecimal("101.90")));
        assertEquals("SON: MIL DOSCIENTOS TREINTA Y CUATRO CON 05/100 SOLES",
                AmountInWords.of(new BigDecimal("1234.05")));
        assertEquals("SON: CERO CON 00/100 SOLES",
                AmountInWords.of(BigDecimal.ZERO));
    }

    // --- estructura y aritmetica del XML ---

    @Test
    void elXmlLlevaLosDatosDelComprobante() {
        Invoice invoice = boletaDeEjemplo();
        Document doc = builder.build(invoice, config);

        assertEquals("2.1", text(doc, "UBLVersionID"));
        assertEquals("B001-00000037", text(doc, "ID"));
        assertEquals("03", text(doc, "InvoiceTypeCode"));
        assertEquals("PEN", text(doc, "DocumentCurrencyCode"));
        assertEquals("SON: SESENTA Y CINCO CON 00/100 SOLES", text(doc, "Note"));

        // El hueco de la firma tiene que existir antes de firmar.
        assertEquals(1, doc.getElementsByTagNameNS("*", "ExtensionContent").getLength());
    }

    @Test
    void elTotalDeclaradoEsElQueSeCobro() {
        Invoice invoice = boletaDeEjemplo();
        Document doc = builder.build(invoice, config);

        // 2 x 25.00 + 1 x 10.00 + 5.00 de delivery = 65.00 con IGV incluido.
        assertEquals("65.00", text(doc, "PayableAmount"));
        assertEquals("65.00", text(doc, "TaxInclusiveAmount"));
        assertEquals("55.08", text(doc, "LineExtensionAmount"));

        // El IGV del documento tiene que cuadrar con la resta, no aproximarse.
        BigDecimal net = new BigDecimal(text(doc, "LineExtensionAmount"));
        BigDecimal payable = new BigDecimal(text(doc, "PayableAmount"));
        BigDecimal igv = new BigDecimal(text(doc, "TaxAmount"));
        assertEquals(0, payable.compareTo(net.add(igv)),
                "El total tiene que ser exactamente valor de venta mas IGV");
    }

    @Test
    void cadaProductoEsUnaLineaMasLaDelDelivery() {
        Invoice invoice = boletaDeEjemplo();
        Document doc = builder.build(invoice, config);

        NodeList lines = doc.getElementsByTagNameNS("*", "InvoiceLine");
        assertEquals(3, lines.getLength(), "dos productos y el delivery");

        List<String> descriptions = allText(doc, "Description");
        assertTrue(descriptions.contains("1/4 POLLO A LA BRASA - CLASICO"),
                "el sabor elegido va en la descripcion: " + descriptions);
        assertTrue(descriptions.contains("SERVICIO DE DELIVERY"));
    }

    /**
     * El caso que hace fallar los comprobantes: dividir cada linea entre 1.18 y
     * redondear deja centavos sueltos. El XML tiene que seguir declarando lo que
     * el cliente pago, no lo que da la suma de los redondeos.
     */
    @Test
    void elCentavoDelRedondeoNoDescuadraElComprobante() {
        // Tres platos de 33.33 cobran 99.99, un caso donde el redondeo por linea
        // no coincide con el redondeo del total.
        Order order = new Order();
        order.setDeliveryFee(BigDecimal.ZERO);
        order.setDetails(new ArrayList<>(List.of(
                detalle("PLATO A", 1, "33.33"),
                detalle("PLATO B", 1, "33.33"),
                detalle("PLATO C", 1, "33.33"))));

        Invoice invoice = comprobante(order, InvoiceType.BOLETA, "B001", 40, "99.99");
        Document doc = builder.build(invoice, config);

        assertEquals("99.99", text(doc, "PayableAmount"));

        BigDecimal net = new BigDecimal(text(doc, "LineExtensionAmount"));
        BigDecimal igv = new BigDecimal(text(doc, "TaxAmount"));
        assertEquals(0, new BigDecimal("99.99").compareTo(net.add(igv)));
    }

    @Test
    void laBoletaSinCompradorSeDeclaraComoClientesVarios() {
        Invoice invoice = boletaDeEjemplo();
        invoice.setClient(null);
        Document doc = builder.build(invoice, config);

        List<String> names = allText(doc, "RegistrationName");
        assertTrue(names.contains("CLIENTES VARIOS"),
                "sin comprador identificado no se puede inventar un nombre: " + names);
    }

    @Test
    void laFacturaLlevaElRucDelComprador() {
        Order order = pedidoDeEjemplo();
        Invoice invoice = comprobante(order, InvoiceType.FACTURA, "F001", 12, "65.00");

        Client empresa = new Client();
        empresa.setDocType(DocumentType.RUC);
        empresa.setDocNumber("20123456789");
        empresa.setName("DISTRIBUIDORA EJEMPLO S.A.C.");
        invoice.setClient(empresa);

        Document doc = builder.build(invoice, config);

        assertEquals("01", text(doc, "InvoiceTypeCode"));
        List<String> names = allText(doc, "RegistrationName");
        assertTrue(names.contains("DISTRIBUIDORA EJEMPLO S.A.C."));
    }

    // --- firma ---

    @Test
    void laFirmaQuedaDentroDeLaExtensionYVerifica() throws Exception {
        assertTrue(Files.exists(TEST_CERT),
                "falta el certificado de prueba en " + TEST_CERT.toAbsolutePath());

        Invoice invoice = boletaDeEjemplo();
        Document doc = builder.build(invoice, config);
        UblSigner.SigningMaterial material = signer.loadMaterial();
        signer.sign(doc, material);

        NodeList signatures = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        assertEquals(1, signatures.getLength(), "tiene que haber exactamente una firma");
        assertEquals("ExtensionContent", signatures.item(0).getParentNode().getLocalName(),
                "la firma va dentro de ext:ExtensionContent o SUNAT la rechaza");

        assertTrue(verifies(signatures.item(0), material), "la firma no verifica");
    }

    /**
     * El XML que se envia es el serializado, no el DOM en memoria. Si serializar
     * altera un espacio, la firma deja de verificar y SUNAT lo rechaza con un
     * error que no dice nada util.
     */
    @Test
    void laFirmaSigueValiendoDespuesDeSerializar() throws Exception {
        Invoice invoice = boletaDeEjemplo();
        Document doc = builder.build(invoice, config);
        UblSigner.SigningMaterial material = signer.loadMaterial();
        signer.sign(doc, material);

        byte[] xml = store.toBytes(doc);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document reparsed = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));

        NodeList signatures = reparsed.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        assertEquals(1, signatures.getLength());
        assertEquals("OK", validationDetail(signatures.item(0), material),
                "la firma dejo de verificar al serializar y volver a leer el XML");
    }

    @Test
    void elZipUsaElNombreQueExigeSunat() {
        Invoice invoice = boletaDeEjemplo();
        assertEquals("20610046097-03-B001-00000037", store.baseName(invoice, config.getRuc()));
    }

    // --- ayudas ---

    private boolean verifies(org.w3c.dom.Node signatureNode, UblSigner.SigningMaterial material)
            throws Exception {
        return "OK".equals(validationDetail(signatureNode, material));
    }

    /**
     * Devuelve "OK" o el motivo exacto del fallo.
     *
     * Separa las dos causas posibles, que se arreglan distinto: si falla el
     * digest, el documento cambio despues de firmar; si falla el SignatureValue,
     * cambio el bloque SignedInfo o la clave no corresponde.
     */
    private String validationDetail(org.w3c.dom.Node signatureNode, UblSigner.SigningMaterial material)
            throws Exception {
        DOMValidateContext context =
                new DOMValidateContext(material.certificate().getPublicKey(), signatureNode);
        // SHA1 es lo que documenta SUNAT; la validacion segura del JDK lo rechaza
        // por antiguo, asi que para comprobar nuestra propia firma se desactiva.
        context.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.FALSE);

        XMLSignatureFactory factory = XMLSignatureFactory.getInstance("DOM");
        XMLSignature signature = factory.unmarshalXMLSignature(context);

        if (signature.validate(context)) {
            return "OK";
        }
        if (!signature.getSignatureValue().validate(context)) {
            return "SignatureValue invalido: cambio SignedInfo o no es la clave";
        }
        StringBuilder detail = new StringBuilder("digest distinto: el documento cambio tras firmar");
        for (Object reference : signature.getSignedInfo().getReferences()) {
            javax.xml.crypto.dsig.Reference ref = (javax.xml.crypto.dsig.Reference) reference;
            detail.append(" [URI=").append(ref.getURI())
                    .append(" valida=").append(ref.validate(context)).append("]");
        }
        return detail.toString();
    }

    private Invoice boletaDeEjemplo() {
        return comprobante(pedidoDeEjemplo(), InvoiceType.BOLETA, "B001", 37, "65.00");
    }

    private Order pedidoDeEjemplo() {
        Order order = new Order();
        order.setOrderCode("PED-20260907-0037");
        order.setDeliveryFee(new BigDecimal("5.00"));

        OrderDetail pollo = detalle("1/4 POLLO A LA BRASA", 2, "50.00");
        pollo.setFlavorName("CLASICO");
        order.setDetails(new ArrayList<>(List.of(
                pollo,
                detalle("GASEOSA INCA KOLA 1L", 1, "10.00"))));
        return order;
    }

    private OrderDetail detalle(String name, int quantity, String subtotal) {
        OrderDetail detail = new OrderDetail();
        detail.setProductName(name);
        detail.setQuantity(quantity);
        detail.setSubtotal(new BigDecimal(subtotal));
        detail.setUnitPrice(new BigDecimal(subtotal)
                .divide(BigDecimal.valueOf(quantity), 2, java.math.RoundingMode.HALF_UP));
        return detail;
    }

    private Invoice comprobante(Order order, InvoiceType type, String series,
                                int correlative, String total) {
        BigDecimal amount = new BigDecimal(total);
        BigDecimal taxable = amount.divide(new BigDecimal("1.18"), 2, java.math.RoundingMode.HALF_UP);

        Invoice invoice = new Invoice();
        invoice.setOrder(order);
        invoice.setInvoiceType(type);
        invoice.setSeries(series);
        invoice.setCorrelativeNumber(correlative);
        invoice.setFullNumber(String.format("%s-%08d", series, correlative));
        invoice.setIssueDate(LocalDateTime.of(2026, 9, 7, 20, 41, 0));
        invoice.setTaxableAmount(taxable);
        invoice.setIgvAmount(amount.subtract(taxable));
        invoice.setTotalAmount(amount);

        Client cliente = new Client();
        cliente.setDocType(DocumentType.DNI);
        cliente.setDocNumber("45678912");
        cliente.setName("JUAN PEREZ QUIROZ");
        invoice.setClient(cliente);

        return invoice;
    }

    private String text(Document doc, String localName) {
        NodeList nodes = doc.getElementsByTagNameNS("*", localName);
        return nodes.getLength() > 0 ? nodes.item(0).getTextContent() : null;
    }

    private List<String> allText(Document doc, String localName) {
        NodeList nodes = doc.getElementsByTagNameNS("*", localName);
        List<String> values = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            values.add(nodes.item(i).getTextContent());
        }
        return values;
    }
}
