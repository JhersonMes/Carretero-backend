package com.carretero.service.billing;

import com.carretero.model.BusinessConfig;
import com.carretero.model.Client;
import com.carretero.model.Invoice;
import com.carretero.model.Order;
import com.carretero.model.OrderDetail;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Arma el XML UBL 2.1 de una boleta o factura.
 *
 * Se construye sobre DOM y no concatenando texto: un XML de SUNAT se rechaza por
 * detalles de orden de nodos y de escapado, y armarlo a mano con cadenas es
 * pedir que un nombre de plato con un ampersand tumbe la emision de un dia.
 *
 * Lo que este builder NO hace todavia, y hay que saberlo antes de venderlo:
 * no modela descuentos como cac:AllowanceCharge ni operaciones exoneradas o
 * inafectas. Todo lo que se factura aqui se declara como gravado con IGV al 18%,
 * que es lo unico que vende el local hoy.
 */
@Component
public class UblInvoiceBuilder {

    private static final String NS_INVOICE = "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2";
    private static final String NS_CAC = "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";
    private static final String NS_CBC = "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";
    private static final String NS_EXT = "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2";
    private static final String NS_DS = "http://www.w3.org/2000/09/xmldsig#";

    private static final String CURRENCY = "PEN";
    private static final BigDecimal IGV_RATE = new BigDecimal("0.18");
    private static final BigDecimal IGV_FACTOR = new BigDecimal("1.18");

    /** Identificador de la firma. SUNAT espera encontrarla referenciada por aqui. */
    public static final String SIGNATURE_ID = "SignatureSP";

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Linea del comprobante, ya con el IGV separado.
     *
     * @param description texto que ve el cliente
     * @param quantity    cantidad vendida
     * @param gross       importe con IGV, que es como se vende en la carta
     * @param net         valor de venta sin IGV
     */
    private record UblLine(String description, int quantity, BigDecimal gross, BigDecimal net) {
        BigDecimal igv() {
            return gross.subtract(net);
        }

        /** Valor unitario sin IGV. */
        BigDecimal unitNet() {
            return net.divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP);
        }

        /** Precio unitario con IGV, el que figura en la carta. */
        BigDecimal unitGross() {
            return gross.divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP);
        }
    }

    /**
     * Devuelve el documento sin firmar, con la extension vacia donde va la firma.
     *
     * @throws IllegalStateException si el comprobante no trae lo minimo para
     *                               declararse; es una falla del sistema, no un
     *                               rechazo de SUNAT
     */
    public Document build(Invoice invoice, BusinessConfig config) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document doc = factory.newDocumentBuilder().newDocument();

            Element root = doc.createElementNS(NS_INVOICE, "Invoice");

            // El namespace por defecto se declara a mano, aunque createElementNS ya
            // ponga el elemento en el. Si no se declara, el DOM no lleva el atributo
            // xmlns y el serializador se lo agrega al escribir: se firmaria un
            // documento y se enviaria otro, con la firma invalida y un rechazo de
            // SUNAT que no explica nada. Lo mismo vale para los prefijos de abajo.
            root.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns", NS_INVOICE);
            root.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:cac", NS_CAC);
            root.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:cbc", NS_CBC);
            root.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:ext", NS_EXT);
            root.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:ds", NS_DS);
            doc.appendChild(root);

            addSignatureSlot(doc, root);
            addHeader(doc, root, invoice);
            addSignatureReference(doc, root, config);
            addSupplier(doc, root, config);
            addCustomer(doc, root, invoice);

            List<UblLine> lines = buildLines(invoice);
            addDocumentTotals(doc, root, invoice, lines);
            addLines(doc, root, lines);

            return doc;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "No se pudo armar el XML del comprobante " + invoice.getFullNumber(), e);
        }
    }

    /**
     * La extension vacia donde despues se inserta la firma.
     *
     * Tiene que existir antes de firmar: la firma se cuelga de este nodo, y si el
     * hueco no esta el documento firmado queda con la firma en otro lugar y SUNAT
     * lo rechaza.
     */
    private void addSignatureSlot(Document doc, Element root) {
        Element extensions = child(doc, root, NS_EXT, "ext:UBLExtensions");
        Element extension = child(doc, extensions, NS_EXT, "ext:UBLExtension");
        child(doc, extension, NS_EXT, "ext:ExtensionContent");
    }

    private void addHeader(Document doc, Element root, Invoice invoice) {
        text(doc, root, NS_CBC, "cbc:UBLVersionID", "2.1");
        text(doc, root, NS_CBC, "cbc:CustomizationID", "2.0");
        text(doc, root, NS_CBC, "cbc:ID", invoice.getFullNumber());
        text(doc, root, NS_CBC, "cbc:IssueDate", invoice.getIssueDate().format(DATE));
        text(doc, root, NS_CBC, "cbc:IssueTime", invoice.getIssueDate().format(TIME));

        Element typeCode = text(doc, root, NS_CBC, "cbc:InvoiceTypeCode",
                SunatCodes.documentType(invoice.getInvoiceType()));
        typeCode.setAttribute("listID", "0101");

        Element note = text(doc, root, NS_CBC, "cbc:Note",
                AmountInWords.of(invoice.getTotalAmount()));
        note.setAttribute("languageLocaleID", "1000");

        text(doc, root, NS_CBC, "cbc:DocumentCurrencyCode", CURRENCY);
    }

    private void addSignatureReference(Document doc, Element root, BusinessConfig config) {
        Element signature = child(doc, root, NS_CAC, "cac:Signature");
        text(doc, signature, NS_CBC, "cbc:ID", SIGNATURE_ID);

        Element party = child(doc, signature, NS_CAC, "cac:SignatoryParty");
        Element identification = child(doc, party, NS_CAC, "cac:PartyIdentification");
        text(doc, identification, NS_CBC, "cbc:ID", config.getRuc());
        Element name = child(doc, party, NS_CAC, "cac:PartyName");
        text(doc, name, NS_CBC, "cbc:Name", config.getBusinessName());

        Element attachment = child(doc, signature, NS_CAC, "cac:DigitalSignatureAttachment");
        Element reference = child(doc, attachment, NS_CAC, "cac:ExternalReference");
        text(doc, reference, NS_CBC, "cbc:URI", "#" + SIGNATURE_ID);
    }

    private void addSupplier(Document doc, Element root, BusinessConfig config) {
        Element supplier = child(doc, root, NS_CAC, "cac:AccountingSupplierParty");
        Element party = child(doc, supplier, NS_CAC, "cac:Party");

        Element identification = child(doc, party, NS_CAC, "cac:PartyIdentification");
        Element id = text(doc, identification, NS_CBC, "cbc:ID", config.getRuc());
        id.setAttribute("schemeID", "6");

        Element name = child(doc, party, NS_CAC, "cac:PartyName");
        text(doc, name, NS_CBC, "cbc:Name", config.getCommercialName());

        Element legal = child(doc, party, NS_CAC, "cac:PartyLegalEntity");
        text(doc, legal, NS_CBC, "cbc:RegistrationName", config.getBusinessName());

        Element address = child(doc, legal, NS_CAC, "cac:RegistrationAddress");
        text(doc, address, NS_CBC, "cbc:AddressTypeCode", "0000");
        Element line = child(doc, address, NS_CAC, "cac:AddressLine");
        text(doc, line, NS_CBC, "cbc:Line", config.getAddress());
    }

    /**
     * El adquirente.
     *
     * Una boleta sin comprador identificado es valida por debajo del umbral que
     * obliga a pedir el DNI, y en ese caso se declara con documento "0" y nombre
     * generico. Inventar un DNI para llenar el hueco seria declarar a nombre de
     * una persona que no compro nada.
     */
    private void addCustomer(Document doc, Element root, Invoice invoice) {
        Element customer = child(doc, root, NS_CAC, "cac:AccountingCustomerParty");
        Element party = child(doc, customer, NS_CAC, "cac:Party");

        Client client = invoice.getClient();
        boolean identified = client != null
                && client.getDocNumber() != null
                && !client.getDocNumber().isBlank();

        Element identification = child(doc, party, NS_CAC, "cac:PartyIdentification");
        Element id = text(doc, identification, NS_CBC, "cbc:ID",
                identified ? client.getDocNumber() : "00000000");
        id.setAttribute("schemeID",
                identified ? SunatCodes.identityType(client.getDocType()) : "0");

        Element legal = child(doc, party, NS_CAC, "cac:PartyLegalEntity");
        text(doc, legal, NS_CBC, "cbc:RegistrationName",
                identified && client.getName() != null ? client.getName() : "CLIENTES VARIOS");
    }

    private void addDocumentTotals(Document doc, Element root, Invoice invoice, List<UblLine> lines) {
        BigDecimal net = lines.stream().map(UblLine::net).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal igv = lines.stream().map(UblLine::igv).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = net.add(igv);

        Element taxTotal = child(doc, root, NS_CAC, "cac:TaxTotal");
        amount(doc, taxTotal, "cbc:TaxAmount", igv);

        Element subtotal = child(doc, taxTotal, NS_CAC, "cac:TaxSubtotal");
        amount(doc, subtotal, "cbc:TaxableAmount", net);
        amount(doc, subtotal, "cbc:TaxAmount", igv);
        addIgvCategory(doc, subtotal, false);

        Element monetary = child(doc, root, NS_CAC, "cac:LegalMonetaryTotal");
        amount(doc, monetary, "cbc:LineExtensionAmount", net);
        amount(doc, monetary, "cbc:TaxInclusiveAmount", total);
        amount(doc, monetary, "cbc:PayableAmount", total);
    }

    private void addLines(Document doc, Element root, List<UblLine> lines) {
        int number = 1;
        for (UblLine line : lines) {
            Element element = child(doc, root, NS_CAC, "cac:InvoiceLine");
            text(doc, element, NS_CBC, "cbc:ID", String.valueOf(number++));

            Element quantity = text(doc, element, NS_CBC, "cbc:InvoicedQuantity",
                    String.valueOf(line.quantity()));
            // NIU: unidad. El local vende platos y bebidas, nada al peso.
            quantity.setAttribute("unitCode", "NIU");

            amount(doc, element, "cbc:LineExtensionAmount", line.net());

            Element pricing = child(doc, element, NS_CAC, "cac:PricingReference");
            Element alternative = child(doc, pricing, NS_CAC, "cac:AlternativeConditionPrice");
            amount(doc, alternative, "cbc:PriceAmount", line.unitGross());
            text(doc, alternative, NS_CBC, "cbc:PriceTypeCode", "01");

            Element taxTotal = child(doc, element, NS_CAC, "cac:TaxTotal");
            amount(doc, taxTotal, "cbc:TaxAmount", line.igv());
            Element subtotal = child(doc, taxTotal, NS_CAC, "cac:TaxSubtotal");
            amount(doc, subtotal, "cbc:TaxableAmount", line.net());
            amount(doc, subtotal, "cbc:TaxAmount", line.igv());
            addIgvCategory(doc, subtotal, true);

            Element item = child(doc, element, NS_CAC, "cac:Item");
            text(doc, item, NS_CBC, "cbc:Description", line.description());

            Element price = child(doc, element, NS_CAC, "cac:Price");
            amount(doc, price, "cbc:PriceAmount", line.unitNet());
        }
    }

    /** Categoria tributaria: IGV, operacion gravada (catalogo 07, codigo 10). */
    private void addIgvCategory(Document doc, Element parent, boolean withPercent) {
        Element category = child(doc, parent, NS_CAC, "cac:TaxCategory");
        if (withPercent) {
            text(doc, category, NS_CBC, "cbc:Percent", IGV_RATE.movePointRight(2).toPlainString());
            text(doc, category, NS_CBC, "cbc:TaxExemptionReasonCode", "10");
        }
        Element scheme = child(doc, category, NS_CAC, "cac:TaxScheme");
        text(doc, scheme, NS_CBC, "cbc:ID", "1000");
        text(doc, scheme, NS_CBC, "cbc:Name", "IGV");
        text(doc, scheme, NS_CBC, "cbc:TaxTypeCode", "VAT");
    }

    /**
     * Convierte el pedido en lineas con el IGV separado.
     *
     * Los precios de la carta ya incluyen IGV, asi que cada linea se divide entre
     * 1.18 y se redondea. Esa division deja centavos sueltos: la suma de las
     * lineas redondeadas casi nunca da exactamente el total cobrado. La diferencia
     * se carga a la ultima linea, de modo que el XML cuadre consigo mismo y a la
     * vez con el importe que el cliente pago. Si en cambio se dejara que el
     * documento sumara distinto de lo cobrado, el comprobante declararia un monto
     * que nadie pago.
     */
    private List<UblLine> buildLines(Invoice invoice) {
        Order order = invoice.getOrder();
        List<UblLine> lines = new ArrayList<>();

        for (OrderDetail detail : order.getDetails()) {
            BigDecimal gross = detail.getSubtotal();
            if (gross == null || gross.signum() <= 0) {
                continue;
            }
            int quantity = detail.getQuantity() == null || detail.getQuantity() < 1
                    ? 1
                    : detail.getQuantity();
            lines.add(new UblLine(describe(detail), quantity, gross, netOf(gross)));
        }

        BigDecimal deliveryFee = order.getDeliveryFee();
        if (deliveryFee != null && deliveryFee.signum() > 0) {
            lines.add(new UblLine("SERVICIO DE DELIVERY", 1, deliveryFee, netOf(deliveryFee)));
        }

        // Un pedido sin items no deberia llegar a facturarse, pero si llega es
        // mejor declarar una linea por el total que emitir un XML sin detalle.
        if (lines.isEmpty()) {
            BigDecimal total = invoice.getTotalAmount();
            lines.add(new UblLine("CONSUMO", 1, total, netOf(total)));
            return lines;
        }

        return absorbRoundingDifference(lines, invoice.getTotalAmount());
    }

    private List<UblLine> absorbRoundingDifference(List<UblLine> lines, BigDecimal chargedTotal) {
        BigDecimal grossSum = lines.stream().map(UblLine::gross).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal difference = chargedTotal.subtract(grossSum);
        if (difference.signum() == 0) {
            return lines;
        }

        int last = lines.size() - 1;
        UblLine tail = lines.get(last);
        BigDecimal adjustedGross = tail.gross().add(difference);
        lines.set(last, new UblLine(tail.description(), tail.quantity(), adjustedGross, netOf(adjustedGross)));
        return lines;
    }

    private BigDecimal netOf(BigDecimal gross) {
        return gross.divide(IGV_FACTOR, 2, RoundingMode.HALF_UP);
    }

    /** Descripcion que va en el comprobante, con el sabor si lo tiene. */
    private String describe(OrderDetail detail) {
        String name = detail.getProductName() == null ? "PRODUCTO" : detail.getProductName();
        String flavor = detail.getFlavorName();
        return (flavor == null || flavor.isBlank()) ? name : name + " - " + flavor;
    }

    // --- utilidades DOM ---

    private Element child(Document doc, Element parent, String namespace, String qualifiedName) {
        Element element = doc.createElementNS(namespace, qualifiedName);
        parent.appendChild(element);
        return element;
    }

    private Element text(Document doc, Element parent, String namespace, String qualifiedName, String value) {
        Element element = child(doc, parent, namespace, qualifiedName);
        element.setTextContent(value == null ? "" : value);
        return element;
    }

    private void amount(Document doc, Element parent, String qualifiedName, BigDecimal value) {
        Element element = text(doc, parent, NS_CBC, qualifiedName,
                value.setScale(2, RoundingMode.HALF_UP).toPlainString());
        element.setAttribute("currencyID", CURRENCY);
    }
}
