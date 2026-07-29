package com.fluttiris.admincontrol.qrcode.application;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

@Component
public class QrImageGenerator {

    private static final int SIZE_PX = 300;

    public byte[] genererPng(String contenu) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(contenu, BarcodeFormat.QR_CODE, SIZE_PX, SIZE_PX);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            throw new UncheckedIOException("Échec de génération du QR code", new IOException(e));
        }
    }
}
