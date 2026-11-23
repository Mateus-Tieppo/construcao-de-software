package br.pucrs.pulsepass.ingressos.adaptadores.pdf;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Gera o PDF do ingresso com carimbo de tempo e QR Code (US25), com OpenPDF e ZXing. */
@Component
public class GeradorPdf {

  private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss z")
      .withZone(ZoneId.of("America/Sao_Paulo"));

  public record DadosIngresso(String ingressoId, String evento, String setorAssento, String titular,
      Instant emitidoEm, String conteudoQr) {
  }

  public byte[] gerar(DadosIngresso d) {
    try (ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
      Document doc = new Document(PageSize.A5);
      PdfWriter.getInstance(doc, saida);
      doc.open();
      Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
      Font normal = FontFactory.getFont(FontFactory.HELVETICA, 11);
      Font pequeno = FontFactory.getFont(FontFactory.HELVETICA, 8);
      Paragraph cab = new Paragraph("PulsePass", titulo);
      cab.setAlignment(Element.ALIGN_CENTER);
      doc.add(cab);
      doc.add(new Paragraph(" "));
      doc.add(new Paragraph("Evento: " + d.evento(), normal));
      doc.add(new Paragraph("Setor / assento: " + d.setorAssento(), normal));
      doc.add(new Paragraph("Titular: " + d.titular(), normal));
      doc.add(new Paragraph("Emitido em: " + FORMATO.format(d.emitidoEm()), normal));
      doc.add(new Paragraph("Ingresso: " + d.ingressoId(), pequeno));
      Image qr = Image.getInstance(qr(d.conteudoQr()), null);
      qr.scaleToFit(220, 220);
      qr.setAlignment(Element.ALIGN_CENTER);
      doc.add(qr);
      Paragraph rodape = new Paragraph("Apresente este QR Code na entrada. Uso único.", pequeno);
      rodape.setAlignment(Element.ALIGN_CENTER);
      doc.add(rodape);
      doc.close();
      return saida.toByteArray();
    } catch (IOException | WriterException e) {
      throw new IllegalStateException("Falha ao gerar PDF do ingresso " + d.ingressoId(), e);
    }
  }

  static BufferedImage qr(String conteudo) throws WriterException {
    BitMatrix matriz = new QRCodeWriter().encode(conteudo, BarcodeFormat.QR_CODE, 400, 400,
        Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M, EncodeHintType.MARGIN, 1));
    return MatrixToImageWriter.toBufferedImage(matriz);
  }
}
