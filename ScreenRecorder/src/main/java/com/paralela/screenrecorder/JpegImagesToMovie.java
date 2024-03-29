package com.paralela.screenrecorder;

/*
 * @(#)JpegImagesToMovie.java   1.3 01/03/13
 *
 * Copyright (c) 1999-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Sun grants you ("Licensee") a non-exclusive, royalty free, license to use,
 * modify and redistribute this software in source and binary code form,
 * provided that i) this copyright notice and license appear on all copies of
 * the software; and ii) Licensee does not utilize the software in a manner
 * which is disparaging to Sun.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE
 * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING
 * OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS
 * LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT,
 * INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF
 * OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line control of
 * aircraft, air traffic, aircraft navigation or aircraft communications; or in
 * the design, construction, operation or maintenance of any nuclear
 * facility. Licensee represents and warrants that it will not use or
 * redistribute the Software for such purposes.
 */
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.media.ConfigureCompleteEvent;
import javax.media.Controller;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.DataSink;
import javax.media.EndOfMediaEvent;
import javax.media.Format;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.NoDataSinkException;
import javax.media.NoProcessorException;
import javax.media.PrefetchCompleteEvent;
import javax.media.Processor;
import javax.media.RealizeCompleteEvent;
import javax.media.ResourceUnavailableEvent;
import javax.media.control.TrackControl;
import javax.media.datasink.DataSinkErrorEvent;
import javax.media.datasink.DataSinkEvent;
import javax.media.datasink.DataSinkListener;
import javax.media.datasink.EndOfStreamEvent;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.FileTypeDescriptor;

/*
*   Desc: API de comunicación con el Datasink para crear los videos.  
*/
class JpegImagesToMovie implements ControllerListener, DataSinkListener {

    public boolean doIt(int width, int height, int frameRate,
            ArrayList<String> inFiles, MediaLocator outML) {
        ImageDataSource ids = new ImageDataSource(width, height, frameRate,
                inFiles);

        Processor p;

        try {
            System.err.println("- create processor for the image datasource ...");
            p = Manager.createProcessor(ids);
        } catch (IOException | NoProcessorException e) {
            System.err.println("Yikes!  Cannot create a processor from the data source.");
            return false;
        }

        p.addControllerListener(this);

        // Put the Processor into configured state so we can set
        // some processing options on the processor.
        p.configure();
        if (!waitForState(p, Processor.Configured)) {
            System.err.println("Failed to configure the processor.");
            return false;
        }

        // Set the output content descriptor to QuickTime.
        p.setContentDescriptor(new ContentDescriptor(
                FileTypeDescriptor.QUICKTIME));// FileTypeDescriptor.MSVIDEO

        // Query for the processor for supported formats.
        // Then set it on the processor.
        TrackControl tcs[] = p.getTrackControls();
        Format f[] = tcs[0].getSupportedFormats();
        if (f == null || f.length <= 0) {
            System.err.println("The mux does not support the input format: "
                    + tcs[0].getFormat());
            return false;
        }

        tcs[0].setFormat(f[0]);

        System.err.println("Setting the track format to: " + f[0]);

        // We are done with programming the processor. Let's just
        // realize it.
        p.realize();
        if (!waitForState(p, Controller.Realized)) {
            System.err.println("Failed to realize the processor.");
            return false;
        }

        // Now, we'll need to create a DataSink.
        DataSink dsink;
        if ((dsink = createDataSink(p, outML)) == null) {
            System.err
                    .println("Fallo al crear el DataSink con el MediaLocator de"
                            + " salida: " + outML);
            return false;
        }

        dsink.addDataSinkListener(this);
        fileDone = false;

        System.err.println("Creando video...");

        // OK, we can now start the actual transcoding.
        try {
            p.start();
            dsink.start();
        } catch (IOException e) {
            System.err.println("IO error durante el procesamiento");
            return false;
        }

        // Wait for EndOfStream event.
        waitForFileDone();

        // Cleanup.
        try {
            dsink.close();
        } catch (Exception e) {
        }
        p.removeControllerListener(this);

        System.out.println("...Video creado correctamente.");

        return true;
    }

    /**
     * Create the DataSink.
     */
    DataSink createDataSink(Processor p, MediaLocator outML) {

        DataSource ds;

        if ((ds = p.getDataOutput()) == null) {
            System.err
                    .println("Something is really wrong: the processor does not have an output DataSource");
            return null;
        }

        DataSink dsink;

        try {
            System.err.println("- create DataSink for: " + outML);
            dsink = Manager.createDataSink(ds, outML);
            dsink.open();
        } catch (IOException | SecurityException | NoDataSinkException e) {
            System.err.println("Cannot create the DataSink: " + e);
            return null;
        }

        return dsink;
    }

    Object waitSync = new Object();
    boolean stateTransitionOK = true;

    /**
     * Block until the processor has transitioned to the given state. Return
     * false if the transition failed.
     */
    boolean waitForState(Processor p, int state) {
        synchronized (waitSync) {
            try {
                while (p.getState() < state && stateTransitionOK) {
                    waitSync.wait();
                }
            } catch (InterruptedException e) {
            }
        }
        return stateTransitionOK;
    }

    /**
     * Controller Listener.
     */
    public void controllerUpdate(ControllerEvent evt) {

        if (evt instanceof ConfigureCompleteEvent
                || evt instanceof RealizeCompleteEvent
                || evt instanceof PrefetchCompleteEvent) {
            synchronized (waitSync) {
                stateTransitionOK = true;
                waitSync.notifyAll();
            }
        } else if (evt instanceof ResourceUnavailableEvent) {
            synchronized (waitSync) {
                stateTransitionOK = false;
                waitSync.notifyAll();
            }
        } else if (evt instanceof EndOfMediaEvent) {
            evt.getSourceController().stop();
            evt.getSourceController().close();
        }
    }

    Object waitFileSync = new Object();
    boolean fileDone = false;
    boolean fileSuccess = true;

    /**
     * Block until file writing is done.
     */
    boolean waitForFileDone() {
        synchronized (waitFileSync) {
            try {
                while (!fileDone) {
                    waitFileSync.wait();
                }
            } catch (Exception e) {
            }
        }
        return fileSuccess;
    }

    /**
     * Event handler for the file writer.
     */
    public void dataSinkUpdate(DataSinkEvent evt) {

        if (evt instanceof EndOfStreamEvent) {
            synchronized (waitFileSync) {
                fileDone = true;
                waitFileSync.notifyAll();
            }
        } else if (evt instanceof DataSinkErrorEvent) {
            synchronized (waitFileSync) {
                fileDone = true;
                fileSuccess = false;
                waitFileSync.notifyAll();
            }
        }
    }

    /**
     * Create a media locator from the given string.
     */
    @SuppressWarnings("unused")
    public static MediaLocator createMediaLocator(String url) {

        MediaLocator ml;

        if (url.indexOf(":") > 0 && (ml = new MediaLocator(url)) != null) {
            return ml;
        }

        if (url.startsWith(File.separator)) {
            if ((ml = new MediaLocator("file:" + url)) != null) {
                return ml;
            }
        } else {
            String file = "file:" + System.getProperty("user.dir")
                    + File.separator + url;
            if ((ml = new MediaLocator(file)) != null) {
                return ml;
            }
        }
        return null;
    }
}
