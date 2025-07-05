package org.pdfImageVerifier.util;

import com.github.jaiimageio.jpeg2000.impl.J2KImageReaderSpi;
import javax.imageio.spi.IIORegistry;

public class ImageIOInitializer {
    public static void registerJPEG2000() {
        IIORegistry.getDefaultInstance().registerServiceProvider(new J2KImageReaderSpi());
    }
}
