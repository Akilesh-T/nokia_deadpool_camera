package com.morphoinc.app.panoramagp3;

import android.media.Image;
import com.morphoinc.app.LogFilter;
import java.util.Locale;

public class ConvertFromYuv420 implements IImage2BytesConverter {
    private IImage2BytesConverter mImage2BytesConverter = new ConvertFromYuv420Null();

    private class ConvertFromYuv420Null implements IImage2BytesConverter {
        private ConvertFromYuv420Null() {
        }

        public byte[] image2bytes(Image image) {
            LogFilter.i("Camera2App", String.format(Locale.US, "ImageFormat.YUV_420_888 RowStride [%d][%d][%d]", new Object[]{Integer.valueOf(image.getPlanes()[0].getRowStride()), Integer.valueOf(image.getPlanes()[1].getRowStride()), Integer.valueOf(image.getPlanes()[2].getRowStride())}));
            LogFilter.i("Camera2App", String.format(Locale.US, "ImageFormat.YUV_420_888 PixelStride [%d][%d][%d]", new Object[]{Integer.valueOf(image.getPlanes()[0].getPixelStride()), Integer.valueOf(image.getPlanes()[1].getPixelStride()), Integer.valueOf(image.getPlanes()[2].getPixelStride())}));
            String format = PanoramaGP3ImageFormat.getImageFormat(image);
            if ("YUV420_PLANAR".equals(format)) {
                ConvertFromYuv420.this.mImage2BytesConverter = new ConvertFromYuv420Planar();
            } else if ("YUV420_SEMIPLANAR".equals(format)) {
                ConvertFromYuv420.this.mImage2BytesConverter = new ConvertFromYuv420SemiPlanar();
            } else if (!"YVU420_SEMIPLANAR".equals(format)) {
                return new byte[0];
            } else {
                ConvertFromYuv420.this.mImage2BytesConverter = new ConvertFromYvu420SemiPlanar();
            }
            return ConvertFromYuv420.this.mImage2BytesConverter.image2bytes(image);
        }
    }

    public byte[] image2bytes(Image image) {
        return this.mImage2BytesConverter.image2bytes(image);
    }
}
