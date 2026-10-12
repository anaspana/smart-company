package com.smartcompany.service.user;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.smartcompany.model.order.Order;
import com.smartcompany.model.order.OrderItem;
import org.springframework.stereotype.Service;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.constants.StandardFonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {

    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.of("Europe/Belgrade"));

    public byte[] generateInvoice(Order order) {
        PdfFont font = null;
        try {
            font = PdfFontFactory.createFont(StandardFonts.HELVETICA, "Cp1250", com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);        
        document.setFont(font);

        // Naslov
        document.add(new Paragraph("FAKTURA #" + order.getId())
            .setBold().setFontSize(20).setMarginBottom(10));

        // Osnovne informacije
        document.add(new Paragraph("Datum: " + FORMATTER.format(order.getCreatedAt())));
        document.add(new Paragraph("Kupac: " + order.getCustomer().getUsername()));
        document.add(new Paragraph("Email: " + order.getCustomer().getEmail()));
        document.add(new Paragraph("Firma za dostavu: " + order.getCompany().getName())
            .setMarginBottom(20));

        // Tabela stavki
        Table table = new Table(UnitValue.createPercentArray(new float[]{40, 20, 20, 20}))
            .useAllAvailableWidth();

        // Header
        table.addHeaderCell(new Cell().add(new Paragraph("Proizvod").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Cena").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Količina").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Ukupno").setBold()));

        // Stavke
        for (OrderItem item : order.getItems()) {
            table.addCell(item.getProduct().getName());
            table.addCell(item.getPriceAtOrder() + " €");
            table.addCell(String.valueOf(item.getQuantity()));
            table.addCell(item.getPriceAtOrder()
                .multiply(java.math.BigDecimal.valueOf(item.getQuantity())) + " €");
        }

        document.add(table);

        // Ukupno
        document.add(new Paragraph("Ukupno za plaćanje: " + order.getTotalPrice() + " €")
            .setBold().setFontSize(14).setMarginTop(20));

        document.close();
        return out.toByteArray();
    }
}