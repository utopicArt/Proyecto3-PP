package Server;

import javax.media.MediaLocator;
import javax.media.Time;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PullBufferDataSource;
import javax.media.protocol.PullBufferStream;
import java.util.List;


class ImageDataSource extends PullBufferDataSource {

    ImageSourceStream streams[];

    ImageDataSource(int width, int height, int frameRate,
                    List<String> images) {
        streams = new ImageSourceStream[1];
        streams[0] = new ImageSourceStream(width, height, frameRate, images);
    }

    @Override
    public void setLocator(MediaLocator source) {
    }

    @Override
    public MediaLocator getLocator() { return null; }

    @Override
    public String getContentType() { return ContentDescriptor.RAW; }

    @Override
    public void connect() {}

    @Override
    public void disconnect() {}

    @Override
    public void start() {}

    @Override
    public void stop() {}

    @Override
    public PullBufferStream[] getStreams() {
        return streams;
    }

    @Override
    public Time getDuration() {
        return DURATION_UNKNOWN;
    }

    @Override
    public Object[] getControls() {
        return new Object[0];
    }

    @Override
    public Object getControl(String type) {
        return null;
    }
}
