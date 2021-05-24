/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package RMI;

import static ij.IJ.beep;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.Roi;
import java.awt.datatransfer.Clipboard;
import ij.plugin.frame.RoiManager;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import static java.lang.Thread.sleep;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.activation.DataHandler;
import javax.swing.JOptionPane;
import org.micromanager.data.Coords;
import org.micromanager.data.Datastore;
import org.micromanager.data.Image;
import org.micromanager.data.Metadata;
import org.micromanager.data.SummaryMetadata;
import org.micromanager.data.internal.DefaultDataManager;
import org.micromanager.display.DisplayWindow;

/**
 *
 * @author sg
 */
public class JRMIProcessor {
   
    public JProcThread procThread;
    RMI rmi;
    File dataDir;
    File refDir;
    Plot gplot,rplot;
    PlotWindow gplotWnd,rplotWnd;
    
    Datastore dataStore,dataStoreCopy;
    Datastore refStore,refStoreCopy;
    private DisplayWindow dataDisplay;
    private DisplayWindow refDisplay;
    double[][][] bg_tc;
    double[][][] g_tc;
    double[][][] r_tc;
    double[][] av_bg_tc;
    double[][] rel_g_tc;
    double[][] rel_r_tc;
    double[] av_rel_g_tc;
    double[] av_rel_r_tc;
    double[] time_axis;
    private int lastProcessed, curProcessed;
    int nBG=0;
    int nG=0;
    int nR=0;
    int nPoints=0;
    int nCh=0;
    private Coords.CoordsBuilder builder;
    private Coords pos;
    private double[] gfp_cells;
    private Object[] raw_data;
    private Double start_time;
    private Roi[] BG_rois;
    private Roi[] G_rois;
    private Roi[] R_rois;
    double[] gfp_340_380_coef;
    double max_g=0;
    double max_r=0;
    double min_g=0;
    double min_r=0;
    boolean doAnalize;
    private boolean bOnlineAnalysis;
    
    public JRMIProcessor(RMI ctx) {
        rmi = ctx;
        
        procThread = new JProcThread();
        procThread.start();        // start acquisition thread
    };
    
 boolean openData(File file) throws IOException   
 {
    String s = file.getName(); 
    String [] datasuf = s.split("-");
    if (datasuf.length!=5) {JOptionPane.showMessageDialog(null,"Data folder must start with \'data-\'"); return false;} 
    boolean result = false;
    boolean pair_found = false;
    
    if (datasuf[0].compareTo("data")!=0){JOptionPane.showMessageDialog(null,"Please, choose a directory that starts with 'data-'"); return false;} 
    if (!datasuf[4].matches("\\d")) {JOptionPane.showMessageDialog(null,"Please, choose a directory that starts with 'data-' and ends with a -<number>"); return false;} 

    File[] files = file.getParentFile().listFiles(); //get the rest of files in this directory and seek to 'ref-' folder
    for (File f : files) {
        if (f.isDirectory())
        {
            String [] refsuf = f.getName().split("-");
            if ((refsuf[0].compareTo("ref")==0)&&(refsuf[4].compareTo(datasuf[4])==0))
            {
                // we have found the ref-containing folder corresponding to user-selected data folder
                dataDir = file;
                refDir = f;
                pair_found = true;
            }
        }
    }
    
    if (pair_found) // now we can start working with data
    {
        
        DefaultDataManager manager = new DefaultDataManager();
        dataStore = manager.loadData(dataDir.getAbsolutePath(),true);
        refStore = manager.loadData(refDir.getAbsolutePath(),true);
        dataDisplay = rmi.app.displays().createDisplay(dataStore);
        refDisplay = rmi.app.displays().createDisplay(refStore);
        rmi.app.displays().manage(refStore);
        rmi.app.displays().manage(dataStore);
    }   
    return result;
 }
 
//double getROIaverage(ImageProcessor ip, Roi roi)
 double getROIaverage(ImageProcessor ip, Roi roi)
{
            Rectangle rect = roi.getBounds();
            double num=0;
            double intensity=0;
            for (int x=rect.x;x<rect.x+rect.width;x++)
                for (int y=rect.y;y<rect.y+rect.height;y++)
                {
                    if (roi.contains(x, y)) 
                    {
                        //intensity = intensity + img.getIntensityAt(x, y);
                        intensity = intensity + ip.get(x, y);
                        num++;
                    }

                }
    return (num==0)? -1 : intensity/num;
}
 double getROIaverageRaw(short[] data, int w,int h, Roi roi)
{
            Rectangle rect = roi.getBounds();
            double num=0;
            double intensity=0;
            for (int x=rect.x;x<rect.x+rect.width;x++)
                for (int y=rect.y;y<rect.y+rect.height;y++)
                {
                    if (roi.contains(x, y)) 
                    {
                        //intensity = intensity + img.getIntensityAt(x, y);
                        intensity = intensity + data[x+w*y];
                        num++;
                    }

                }
    return (num==0)? -1 : intensity/num;
}
boolean AnalyzeData()
{
    bOnlineAnalysis = rmi.bOnlineAnalysis;
    lastProcessed = 0;
       
    // Get all ROIs from ROI manager
    if ((!rmi.bExperimentIsSetUp)&&(bOnlineAnalysis)) {JOptionPane.showMessageDialog(null,"Please, start acquisition first."); return false;} 
    RoiManager rm = RoiManager.getInstance();
    if (rm==null){ 
        rm = new RoiManager();
    }
    Roi[] rois = rm.getRoisAsArray();
    boolean loadROI = false;
    if (!bOnlineAnalysis){
        if (rois.length!=0){ // There are ROIs already selected
            if (JOptionPane.showConfirmDialog(null,"Clear existing ROIs?", "Clear existing ROIs?", JOptionPane.YES_NO_OPTION) 
                        == JOptionPane.YES_OPTION) {
                rm.reset();
                loadROI=true; // clear ROIs and load ones previousely stored
            }
            else // leave existing ROIs
                loadROI=false;
        }
        else{ //ROI list is empty
            loadROI=true;
        }    
        if (loadROI){
            rm.runCommand("Open",dataDir.getParent()+"\\RoiSet.zip");
            rois = rm.getRoisAsArray();
            if (rois.length==0){JOptionPane.showMessageDialog(null,"No ROIs to analyze"); return false;} 
        }
        
    }
    else{
        //**********temporary code
            //rm.runCommand("Open",rmi.DataHome+"\\DefaultRoiSet.zip");
            //rois = rm.getRoisAsArray();
        //************temporary code ends
        if (rois.length==0){JOptionPane.showMessageDialog(null,"No ROIs to analyze"); return false;} 
        
    }
    
    // Count the number of background, red, and green ROIs
    nBG = nG = nR = 0;
    for (int i=0; i< rois.length; i++)
    {
        if (rois[i].getName().toLowerCase().startsWith("bg")) nBG++;
        if (rois[i].getName().toLowerCase().startsWith("g")) nG++;
        if (rois[i].getName().toLowerCase().startsWith("r")) nR++;
    }
    
    if ((nBG==0)&&(!rmi.doConstBGSubtraction)){JOptionPane.showMessageDialog(null,"Please select background ROI(s) or turn on constant bacground subtraction"); return false;} 
    if ((nG==0)&&(nR==0)){JOptionPane.showMessageDialog(null,"Nothing to do. At least one non-background ROI must be selected.\nIf you selected ROIs, check the naming."); return false;}     
// Split them into arrays
    BG_rois = new Roi[nBG];
    G_rois = new Roi[nG];
    R_rois = new Roi[nR];
    int bc=0,gc=0,rc=0;
    for (int i=0; i< rois.length; i++)
    {
        if (rois[i].getName().toLowerCase().startsWith("bg")) BG_rois[bc++] = rois[i];
        if (rois[i].getName().toLowerCase().startsWith("g")) G_rois[gc++] = rois[i];
        if (rois[i].getName().toLowerCase().startsWith("r")) R_rois[rc++] = rois[i];
    }
    
    if (bOnlineAnalysis)
    {
        nPoints = rmi.recCyclesTotal;
        nCh = rmi.dataNames.length; // Will process only last two channels
        dataStore = rmi.dataStore;
        refStore = rmi.refStore;
        dataDisplay = rmi.dataDisplay;
        refDisplay = rmi.refDisplay;
    }
    else
    {
        Coords crds = dataStore.getMaxIndices();
        nPoints = crds.getTime()+1;
        nCh = crds.getChannel()+1; // Will process only last two channels
        
        lastProcessed = 1 - (dataStore.getNumImages()/nCh - crds.getTime()); // this is to reveal if the Time axis is o or 1 based
    }
    
     if (nCh!=2) 
            {JOptionPane.showMessageDialog(null,"Currently, only ratiometric analysis is implemented, which expects L1 and L2 channels to be present in acquisition."); return false;} 
    
    gplot = new Plot(dataStore.getSavePath()+" Green", "Time,min", "A.U.");
    rplot = new Plot(dataStore.getSavePath()+" Red", "Time,min", "A.U.");
        
    // Create storage for time courses (per ROI per channel)
    bg_tc = new double[nBG][2][nPoints];
    g_tc = new double[nG][2][nPoints];
    r_tc = new double[nR][2][nPoints];
    av_bg_tc = new double[2][nPoints];
    time_axis = new double [nPoints];
    rel_g_tc = new double[nG][nPoints];
    rel_r_tc = new double[nR][nPoints];
    av_rel_g_tc = new double[nPoints];
    av_rel_r_tc = new double[nPoints];
    
    builder = rmi.app.data().getCoordsBuilder();
    pos = builder.stagePosition(0).z(0).time(1).channel(0).build();
    

    // get first image to retreive first timestamp and to initialize builder
    Image first_img = dataStore.getImage(pos);
    start_time = 0.0;//first_img.getMetadata().getElapsedTimeMs();
    if (first_img.getBytesPerPixel()!=2) 
            {JOptionPane.showMessageDialog(null,"Cannot analyse 8-bit images. Please, change camera settings to 12-,14- or 16-bit acquisition."); return false;} 
    
    raw_data = new Object[nCh];
    
    /////////////////////////////////////////////////////////////////////////////
    // CORRECTION FOR GFP BLEED_THROUGH
    // get GFP channel from reference image. Will be used to correct 340 and 380 
    // channels for GFP bleed-through
    /*
    if (refStore.getNumImages()>0)
    {
        gfp_cells = new double[nG]; //allocate storage for GFP intencities
        Object raw_ref = new Object();
        Image gfp_img = refStore.getImage(builder.time(0).channel(rmi.FLT_G).build());
        if (gfp_img==null) {Logger.getAnonymousLogger().log(Level.WARNING, String.format("gfp_img == null ")); return false;}
        raw_ref = gfp_img.getRawPixels();
        
        // Calculate ROIs for each GFP-expressing cell
        for(int gfpi=0; gfpi<nG; gfpi++)
        {
            gfp_cells[gfpi] = getROIaverageRaw((short[])raw_ref,gfp_img.getWidth(),gfp_img.getHeight(),G_rois[gfpi]);     // calculate mean from ROI
        }
        gfp_340_380_coef=new double[2];
    }
    if (rmi.rmi_form.bGFPCorrection.isSelected()!=true){
        gfp_340_380_coef[0]=0.0;
        gfp_340_380_coef[1]=0.0;
    }
    else{
        gfp_340_380_coef[0]=0.22;
        gfp_340_380_coef[1]=0.25;
    }
    */
    ////////////////////////////////////////////////////////////////////////////

    max_g=0;
    min_g=1000;
    max_r=0;
    min_r=1000;
  
    rplotWnd = rplot.show();
    gplotWnd = gplot.show();
    
    doAnalize = true;
    return true;
}

boolean ProcessNextFrames()
{
    // go over all new frames and calculate timecourses
    rmi.rmi_form.labelProgressCalc.setText("Processing...");
    //Logger.getAnonymousLogger().log(Level.WARNING, String.format("lastProcessed : %d\ncurCycle : %d\ndoAnalyse : %b ",lastProcessed,rmi.curCycle,doAnalize));
    int limit = bOnlineAnalysis ? rmi.curCycle : nPoints;
    for (; lastProcessed < limit; lastProcessed++)
    {
                
        Image img=null;
        for(int ch=0; ch < 2; ch++)
        {
            // Take only last two channels
            img = dataStore.getImage(builder.time(lastProcessed).channel(nCh-(2-ch)).build());
            if (img==null) {Logger.getAnonymousLogger().log(Level.WARNING, String.format("img==null ")); return false;}
            raw_data[ch] = img.getRawPixels();
            // add new time point to the time axis
            if (ch==0) 
            {
                 //   time_axis[lastProcessed] = lastProcessed*rmi.recInterval/60.;
                //time_axis[lastProcessed] = (img.getMetadata().getElapsedTimeMs() - start_time)/60000;        
                time_axis[lastProcessed] = img.getMetadata().getElapsedTimeMs()/(1000);        
            }
        }
        //if not sabtracting const background,  for each channel calculate mean BG from all ROI
        if (!rmi.doConstBGSubtraction)
        for(int ch=0; ch < 2; ch++)
        {
            for(int bgi=0; bgi<BG_rois.length; bgi++)
            {
                double res = getROIaverageRaw((short[])raw_data[ch],img.getWidth(),img.getHeight(),BG_rois[bgi]);     // calculate mean from ROI
                bg_tc[bgi][ch][lastProcessed] = res;            // store it in tc
                av_bg_tc[ch][lastProcessed] += res;             // accumulate for averaging
            }
            av_bg_tc[ch][lastProcessed] /= BG_rois.length;      // average ROIs
        }
        //GREEN: calculate L1/L2 ratios for each ROI and average timecources from all ROIs
        av_rel_g_tc[lastProcessed]=0;
        for(int gi=0; gi<G_rois.length; gi++)
        {
            for(int ch=0; ch < 2; ch++)
            {
                g_tc[gi][ch][lastProcessed] = getROIaverageRaw((short[])raw_data[ch],img.getWidth(),img.getHeight(),G_rois[gi]);
                                                        //- gfp_cells[gi]*gfp_340_380_coef[ch]; // here we correct for GFP bleed-through
            }
            double rel_g = 1;
            if (!rmi.doConstBGSubtraction)
                   rel_g = (g_tc[gi][0][lastProcessed]-av_bg_tc[0][lastProcessed])/(g_tc[gi][1][lastProcessed]-av_bg_tc[1][lastProcessed]);
            else
                   rel_g = (g_tc[gi][0][lastProcessed]-rmi.L1bg)/(g_tc[gi][1][lastProcessed]-rmi.L2bg);
    
            if (max_g<rel_g) max_g = rel_g;
            if (min_g>rel_g) min_g = rel_g;
            
            rel_g_tc[gi][lastProcessed] = rel_g;
            av_rel_g_tc[lastProcessed]+=rel_g;
        }
        av_rel_g_tc[lastProcessed]/=G_rois.length;

        //RED: calculate L1/L2 ratios for each ROI and average timecources from all ROIs
        av_rel_r_tc[lastProcessed]=0;
        for(int ri=0; ri<R_rois.length; ri++)
        {
            for(int ch=0; ch < 2; ch++)
            {
                r_tc[ri][ch][lastProcessed] = getROIaverageRaw((short[])raw_data[ch],img.getWidth(),img.getHeight(),R_rois[ri]);
            }
            double rel_r = 1;
            if (!rmi.doConstBGSubtraction)
                rel_r = (r_tc[ri][0][lastProcessed]-av_bg_tc[0][lastProcessed])/(r_tc[ri][1][lastProcessed]-av_bg_tc[1][lastProcessed]);
            else
                rel_r = (r_tc[ri][0][lastProcessed]-rmi.L1bg)/(r_tc[ri][1][lastProcessed]-rmi.L2bg);
            if (max_r<rel_r) max_r = rel_r;
            if (min_r>rel_r) min_r = rel_r;
            
            rel_r_tc[ri][lastProcessed] = rel_r;
            av_rel_r_tc[lastProcessed]+=rel_r;
        }
        av_rel_r_tc[lastProcessed]/=R_rois.length;
        int pr = lastProcessed*100/nPoints;
        if (pr%5==0) rmi.rmi_form.SetProgressCalcBar(pr);
        //try { sleep(10);} catch (InterruptedException ex) { Logger.getLogger(JRMIProcessor.class.getName()).log(Level.SEVERE, null, ex); }
    }
    rmi.rmi_form.eLastFrame.setText(String.valueOf(lastProcessed));

    gplot = new Plot(dataStore.getSavePath()+" Green", "Time,sec", "a.u.");
    rplot = new Plot(dataStore.getSavePath()+" Red", "Time,sec", "a.u.");
    
    //plot.setLimits(0, time_axis[time_axis.length-1], 0, max_r>max_g?max_r:max_g);
    gplot.setLimits(0, time_axis[lastProcessed-1], min_g, max_g); gplot.draw();
    rplot.setLimits(0, time_axis[lastProcessed-1], min_r, max_r); rplot.draw();
    
    //double buf[][] = new double[G_rois.length+R_rois.length+1][time_axis.length];
    //buf[0] = time_axis;
    gplot.setColor(Color.green);
    for(int gi=0; gi<G_rois.length; gi++)
    {
        double[] xtmp = new double[lastProcessed-1];
        double[] ytmp = new double[lastProcessed-1];
        System.arraycopy( time_axis, 0, xtmp, 0, lastProcessed-1 );
        System.arraycopy( rel_g_tc[gi], 0, ytmp, 0, lastProcessed-1 );
        gplot.addPoints(xtmp,ytmp,Plot.LINE);
        //buf[gi] = rel_g_tc[gi];
    }
    gplot.setAxes(false, false, true, true, true, true, 1, (int)((max_g-min_g)/8));
    gplotWnd.drawPlot(gplot);
        
    rplot.setColor(Color.red);
    for(int ri=0; ri<R_rois.length; ri++)
    {
        double[] xtmp = new double[lastProcessed-1];
        double[] ytmp = new double[lastProcessed-1];
        System.arraycopy( time_axis, 0, xtmp, 0, lastProcessed-1 );
        System.arraycopy( rel_r_tc[ri], 0, ytmp, 0, lastProcessed-1 );
        rplot.addPoints(xtmp,ytmp,Plot.LINE);
        //buf[ri+G_rois.length+1] = rel_r_tc[ri];
    }
    rplot.setAxes(false, false, true, true, true, true, 1, (int)((max_r-min_r)/8));
    rplotWnd.drawPlot(rplot);
    
    //Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
    //DataHandler dataHandler = new DataHandler(buf, "application/octet-stream");
    //cb.setContents(dataHandler, null);
    
    rmi.rmi_form.labelProgressCalc.setText("Idle");
    if (!bOnlineAnalysis) doAnalize = false; // if worked on loaded data, do not wait for new data
    return true;
}

    private class JProcThread extends Thread{
        
        private boolean plotUpdate;
        public JProcThread() {
        }

        @Override
        public void run() {
            //super.run(); //To change body of generated methods, choose Tools | Templates.
            while ( true )
            {
                try {
                    if (doAnalize){
                        if (dataStore.getMaxIndices().getTime()+1>lastProcessed)
                        {
                            ProcessNextFrames();
                        }
                    }
                    Thread.sleep(50);
                }
                catch ( InterruptedException e ) { 
                    return;
                }

            }
            
        }
        
    }
    
}
