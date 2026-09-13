package com.carretero.service.billing;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Lee la Constancia de Recepcion que devuelve SUNAT.
 *
 * El codigo 0 significa aceptado. Un CDR aceptado puede ademas traer
 * observaciones (codigos 4000 en adelante): el comprobante vale, pero hay algo
 * que corregir para los siguientes. Se guardan en la descripcion porque si no
 * nadie las lee nunca, y son justamente el aviso temprano de que algo del
 * armado del XML esta mal.
 */
@Component
public class CdrReader {

    /**
     * @param responseCode codigo del catalogo de SUNAT; "0" es conformidad
     * @param description  descripcion, con las observaciones si las hubo
     */
    public record CdrContent(String responseCode, String description) {

        public boolean accepted() {
            return "0".equals(responseCode);
        }
    }

    public CdrContent read(byte[] cdr) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(cdr));

            String code = first(document, "ResponseCode");
            String description = first(document, "Description");

            List<String> notes = all(document, "Note");
            if (!notes.isEmpty()) {
                description = (description == null ? "" : description)
                        + " | Observaciones: " + String.join("; ", notes);
            }

            return new CdrContent(
                    code == null ? null : code.trim(),
                    description == null ? "Sin descripcion en el CDR" : description.trim());

        } catch (Exception e) {
            throw new IllegalStateException("No se pudo leer el CDR: " + e.getMessage(), e);
        }
    }

    private String first(Document document, String localName) {
        NodeList nodes = document.getElementsByTagNameNS("*", localName);
        return nodes.getLength() > 0 ? nodes.item(0).getTextContent() : null;
    }

    private List<String> all(Document document, String localName) {
        NodeList nodes = document.getElementsByTagNameNS("*", localName);
        List<String> values = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            String value = nodes.item(i).getTextContent();
            if (value != null && !value.isBlank()) {
                values.add(value.trim());
            }
        }
        return values;
    }
}
