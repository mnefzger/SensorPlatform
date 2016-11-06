package au.carrsq.sensorplatform.Utilities;

import java.nio.ByteBuffer;


public class ImageHolder {
    private native ByteBuffer jniStoreImageData(byte[] img);
    private native byte[] jniGetImageData(ByteBuffer buffer);
    private native void jniFreeImageData(ByteBuffer buffer);

    private ByteBuffer _handler;

    public ImageHolder(byte[] data) {
        _handler = jniStoreImageData(data);
    }

    public byte[] getImageData() {
        if(_handler == null)
            return null;

        return jniGetImageData(_handler);
    }

    public void freeImageData() {
        if(_handler == null)
            return;

        jniFreeImageData(_handler);
    }
}
