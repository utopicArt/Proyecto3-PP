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
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author: 
 * @see:    Controlador de la toma, carga y reproduccion de video.
 */

public class ScreenshotController implements ActionListener {

    JFrame Master;
    JLabel imageContainer;
    JButton triggerBtn;
    JButton controllerBtn;

    private Robot robot;
    private Rectangle rec;
    private File folder;

    public static int recordingSpeed;
    private static LinkedList<BufferedImage> screenShot;
    private static Queue<BufferedImage> screenShot2Save;
    private static Queue<String> screenshotTimeStamp;
    private BufferedImage screenshotTaken;
    private int statusInfo = 0;

    private String directory, date;
    private SimpleDateFormat formatter;
    private ThreadPoolExecutor takeSSThread;
    private ThreadPoolExecutor showThread;
    private ThreadPoolExecutor saveThread;

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
        
        takeSSThread = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        showThread = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);
        saveThread = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);

        rec = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
    }

    protected double getMemoryUsage() {
        return Runtime.getRuntime().totalMemory() / (1024.0 * 1024.0 * 1024.0);
    }

    public void takeScreenshotController() {
        while (isRecording) {
            try {
                takeScreenshot();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null,
                        "Ocurrio un error:" + ex.getMessage(),
                        "Error al tomar SS", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void takeScreenshot() throws IOException {
        if (getMemoryUsage() > 2.85) {
            System.gc();
            System.err.println(
                    "\n[!]Sistema de emergencia activado."
                    + "screenshotTimeStamp: " + screenshotTimeStamp.size()
                    + "screenShot: " + screenShot.size()
                    + "screenShot2Save: " + screenShot2Save.size());
        }
        screenshotTaken = robot.createScreenCapture(rec);
        date = formatter.format(Calendar.getInstance().getTime());
        screenshotTimeStamp.add(directory + date + ".jpg");

        screenShot.add(screenshotTaken);
        screenShot2Save.add(screenshotTaken);
        System.out.println("SS tomada");
    }

    public void saveScreenshotsController() {
        while (true) {
            saveScreenshots();
            if (statusInfo == 2) {
                controllerBtn.setEnabled(false);
                if (screenShot2Save.isEmpty()) {
                    createVideo();
                    statusInfo = 0;
                    controllerBtn.setEnabled(true);
                    break;
                }
            }
        }
    }

    private void saveScreenshots() {
        System.out.print("");
        if (screenShot2Save.peek() != null && screenshotTimeStamp.peek() != null) {
            try {
                ImageIO.write(screenShot2Save.poll(), "JPG",
                        new File(screenshotTimeStamp.poll()));
            } catch (IOException ex) {
                Logger.getLogger(ScreenshotController.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                System.err.println("SS Guardada");
            }
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
        } else {
            JOptionPane.showMessageDialog(null,
                    "Error al crear video:" + "Hay muy pocas imágenes"
                    + " en el directorio", "Error al crear",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public void showVideoController() {
        while (true) {
            showVideo();
            if(!isRecording){
                break;
            }
        }
    }

    public void showVideo() {
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
    
    private void waitUntilSaveIsDone(){
        while (!saveThread.isTerminated()) {
            System.err.print("Waiting until all images are saved....");
            try {
                if (saveThread.awaitTermination(200, TimeUnit.MILLISECONDS)) {
                    break;
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(ScreenshotController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private boolean wasActivated = false;

    private void submitTasks() throws InterruptedException {
        if(wasActivated){
            //waitUntilSaveIsDone();
        }
        takeSSThread = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        showThread = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);
        saveThread = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        
        for (int i = 0; i < 10; i++) {
            takeSSThread.submit(() -> {
                takeScreenshotController();
                return 0;
            });
        }
        
        for (int i = 0; i < 5; i++) {
            showThread.submit(() -> {
                showVideoController();
                return 0;
            });
        }

        saveThread.submit(() -> {
            saveScreenshotsController();
            return 0;
        });
        wasActivated = true;
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
        
        if(isRecording){
            System.out.println("Creando tareas..");
            try {
                submitTasks();
            } catch (InterruptedException ex) {
                Logger.getLogger(ScreenshotController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        // Al inicia pesa 248.046875mb
        System.out.println(getMemoryUsage() + "gb");
    }
}
