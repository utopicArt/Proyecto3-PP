package com.paralela.screenrecorder;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.media.MediaLocator;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import com.aparapi.Kernel;

/**
 * @author: Adrian Marin Alcala 
 * Desc: Controlador de la imagen y reproduccion devideo.
 */
public class ScreenshotController implements ActionListener {

    JFrame Master;
    JLabel imageContainer;
    JButton triggerBtn;
    JButton controllerBtn;

    private Robot robot;
    private Rectangle rec;
    private File folder;
    
    private static int recordingSpeed;
    private static LinkedList<BufferedImage> screenShot;
    private static Queue<BufferedImage> screenShot2Save;
    private static Queue<String> screenshotTimeStamp;
    private BufferedImage screenshotTaken;
    private int statusInfo = 0;

    private String directory, date;
    private SimpleDateFormat formatter;
    private Thread takeSSTrhead = null;
    private Thread showThread = null;
    private Kernel saveThread = null;  
    
    private static boolean isRecording = false;
    private ImageIcon replayImage;
    static JpegImagesToMovie imageToMovie;
    

    public ScreenshotController(JFrame root, JLabel srcLbl, JButton actionBtn,
            JButton trigger) {
        this.Master = root;
        this.imageContainer = srcLbl;
        this.controllerBtn = actionBtn;
        this.triggerBtn = trigger;
        
        initVars();
        initRobot();
        initThreads();
    }

    private void initRobot() {
        try {
            robot = new Robot();
        } catch (AWTException ex) {
            JOptionPane.showMessageDialog(null,
                    "Ocurrio un error:" + ex.getMessage(),
                    "Error al iniciar Robot", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void initVars() {
        imageToMovie = new JpegImagesToMovie();
        formatter = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss-SSS");
        directory = System.getProperty("user.dir") + "\\src\\internal\\Screenshots\\";
        folder = new File(directory);
        if (!folder.isDirectory()) {
            new File(directory).mkdirs();
        }

        recordingSpeed = 10;
        screenShot = new LinkedList<>();
        screenShot2Save = new LinkedList<>();
        screenshotTimeStamp = new LinkedList<>();        
        
        rec = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());        
    }

    private synchronized void initThreads() {
        if (takeSSTrhead == null) {
            takeSSTrhead = new Thread(() -> {
                while (true) {
                    while (isRecording) {
                        try {
                            takeScreenshot();
                        } catch (IOException ex) {
                            JOptionPane.showMessageDialog(null,
                                    "Ocurrio un error:" + ex.getMessage(),
                                    "Error al tomar SS", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                    if (statusInfo == 2) {
                        if (screenShot2Save.isEmpty()) {
                            controllerBtn.setEnabled(false);
                            try {
                                Thread.sleep(1500);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(ScreenshotController.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            createVideo();
                            statusInfo = 0;
                            controllerBtn.setEnabled(true);
                        }
                    }
                }
            });
        }
        if (showThread == null) {
            showThread = new Thread(() ->{
                    while (true) {
                        showVideo();
                    }
                });
        }
        if (saveThread == null) {
            saveThread = new Kernel() {
                @Override
                public void run() {
                    while (true) {
                        if (screenShot2Save.peek() != null && screenshotTimeStamp.peek() != null) {
                            saveScreenshots();
                        }
                        if (statusInfo == 2) {
                            if (screenShot2Save.isEmpty()) {
                                controllerBtn.setEnabled(false);
                                try {
                                    Thread.sleep(1500);
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(ScreenshotController.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                createVideo();
                                statusInfo = 0;
                                controllerBtn.setEnabled(true);
                            }
                        }
                    }
                }
            };
        }
    }
    
    protected double getMemoryUsage(){
        return Runtime.getRuntime().totalMemory() / (1024.0 * 1024.0 * 1024.0);
    }

    public synchronized void takeScreenshot() throws IOException {
        if (getMemoryUsage() > 2.85) {
            System.gc();
            System.err.println("\n[!]Sistema de emergencia activado."
                    + "screenshotTimeStamp: " + screenshotTimeStamp.size()
                    + "screenShot: " + screenShot.size()
                    + "screenShot2Save: " + screenShot2Save.size());
        }
        screenshotTaken = robot.createScreenCapture(rec);
        date = formatter.format(Calendar.getInstance().getTime());
        screenshotTimeStamp.add(directory + date + ".jpg");
                      
        screenShot.add(screenshotTaken);
        screenShot2Save.add(screenshotTaken);
        System.out.println("Imagen tomada");
        if (screenShot2Save.peek()!= null && screenshotTimeStamp.peek() != null) {
            saveScreenshots();
        }
    }

    private synchronized void saveScreenshots(){
        try {
            ImageIO.write(screenShot2Save.poll(), "JPG",      
                    new File(screenshotTimeStamp.poll()));
        } catch (IOException ex) {
            Logger.getLogger(ScreenshotController.class.getName()).log(Level.SEVERE, null, ex);
        }finally{
            System.err.println("SS Guardada");
        }
    }

    private void createVideo() {
        if (folder.listFiles().length > 1) {
            System.out.println("[CREANDO VIDEO]");
            File[] listOfFiles = folder.listFiles();

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int screenWidth = (int) screenSize.getWidth();
            int screenHeight = (int) screenSize.getHeight();

            formatter = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
            String outputFile = formatter.format(Calendar.getInstance()
                    .getTime()) + ".mp4";

            ArrayList<String> imgLst = new ArrayList<>();

            System.out.println("Obteniendo imagenes de: " + this.directory);
            System.out.println("Imagenes encontradas: " + listOfFiles.length);

            for (File listOfFile : listOfFiles) {
                imgLst.add(listOfFile.getAbsolutePath());
            }
            imgLst.forEach((name) -> {
                System.out.println("Procesando: " + name);
            });

            MediaLocator oml;
            if ((oml = JpegImagesToMovie.createMediaLocator(outputFile)) == null) {
                System.err.println("No se puede construir media locator de: " + outputFile);
                System.exit(0);
            }

            imageToMovie.doIt(screenWidth, screenHeight, recordingSpeed, imgLst, oml);
            screenShot.clear();
            screenShot2Save.clear();
        }else{
            JOptionPane.showMessageDialog(null,
                            "Error al crear video:" + "Hay muy pocas imágenes"
                            + " en el directorio", "Error al crear",
                            JOptionPane.ERROR_MESSAGE);
        }
    }

    public synchronized void showVideo() {
        System.out.print("");
        if (screenShot.peek() != null) {
            try {
                //Esta variable forza al sistema
                replayImage = new ImageIcon(screenShot.poll());
                imageContainer.setIcon(replayImage);
                Thread.sleep((long) 16.6666667);
            } catch (InterruptedException ex) {
                Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        controllerBtn.setText((isRecording ? "Iniciar Grabación" : "Detener Grabación"));
        isRecording = !isRecording;

        statusInfo++;

        triggerBtn.setEnabled(true);
        triggerBtn.setBackground((isRecording
                ? new Color(230, 20, 20) : new Color(189, 189, 189)));
        triggerBtn.setEnabled(false);
        
        /*if(saveThread.isExecuting() && !isRecording){
            try {
                saveThread.wait();
            } catch (InterruptedException ex) {
                Logger.getLogger(ScreenshotController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }else if(!saveThread.isExecuting() && statusInfo > 0){
            saveThread.notifyAll();
        }*/
        
        if (takeSSTrhead.getState() == Thread.State.NEW) {
            takeSSTrhead.start();
        }
        if (showThread.getState() == Thread.State.NEW) {
            showThread.start();
        }
  
        // Al inicia pesa 248.046875mb
        System.out.println(getMemoryUsage() + "gb");
    }
}
