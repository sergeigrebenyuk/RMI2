/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

//start up script sg.bsh
//import RMI.RMI; 
//RMI plugin = new RMI(); 
//plugin.setApp(gui); 
//plugin.show(); 


package RMI;

//import ij.*;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.lang.System;
import java.lang.Math;
import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimerTask;
import java.util.prefs.Preferences;
import javax.swing.JOptionPane;

import mmcorej.CMMCore;
import org.micromanager.Studio;
import java.util.List;
import org.micromanager.MenuPlugin;
import org.micromanager.data.Coords;
import org.micromanager.data.Datastore;
import org.micromanager.data.Image;
import org.micromanager.data.Metadata;
import org.micromanager.data.Metadata.MetadataBuilder;
import org.micromanager.data.SummaryMetadata;
import org.micromanager.data.internal.DefaultSummaryMetadata;
//import org.micromanager.data.internal.DefaultSummaryMetadata;
//import org.micromanager.data.internal.DefaultSummaryMetadata;
import org.micromanager.display.DisplayWindow;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;


/**
 *
 * @author sg
 */
enum RSTATE {REC_START,REC_STOP,REC_RUNNING, REC_IDLE, REC_UNINIT, REC_EXIT, REC_CAL} ;
class TConf{
    String label;
    int filterSlot;
    int exposure;
    boolean capture; 
    Color col;
    public TConf()
    {
        label = "";
        filterSlot = 0;
        exposure = 0;
        capture = false;
        col = Color.WHITE;
    }
}
//@Plugin(type = ProcessorPlugin.class)
@Plugin(type = MenuPlugin.class)
public class RMI implements MenuPlugin, SciJavaPlugin {

    //public ScriptInterface app;
    public CMMCore core;
    public Studio app;
    RMIControlForm rmi_form;
    RMICommentFrame rmi_comment_form;
    JRMIProcessor processor;
    
    public float recTime = 30;
    public float recInterval = 2;
    public int recCyclesTotal = 0;
    public float curTime = 0;
    public int curCycle = 0;
    public boolean bWriteToDisk = true;
    public String textStatus = "Idle";
    public String DataHome = "D:\\Data";
    public String DataFile = "";
    boolean canRun;
    public String channelGroup="Channels";
    public String zoomLive = "1";
    public String binLive = "1";
    public String zoomRec = "1";
    public String binRec = "1";
    public String lastCommentFile = "";
    
    public int shutterDelay=0;
    
    // constants
    static final int FLT_TRANS = 0;
    static final int FLT_L1 = 1;
    static final int FLT_L2 = 2;
    static final int FLT_G = 3;
    static final int FLT_R = 4;
    Color[] refCols;
    Color[] dataCols;
    String[] refNames;
    String[] dataNames = null;
    public int filterSelected = FLT_TRANS;
    TConf[] channels;
    String stateDeviceName;
    boolean bAcqNotFastEnough;
    boolean bAutoRef;
    boolean bExperimentIsSetUp;
    File    fDescFile;
    PrintWriter fDescFileWriter;
    String sDescription;
    int nTrialCnt;
    String sAcqName;
    int adjustedInterval;
    private String sRefName;
    private String acqPath;
    //FileChannel fDescFileChannel;
    private String expTime;
    private String expDate;
    Date timeStart;
    long msecStart;
    private RSTATE _state;
    private java.util.Timer timer;
    private int _sweep_delay;
    ClockTask clockTask;
    AcqTask acqTask;
    Datastore dataStore;
    Datastore refStore;
    DisplayWindow dataDisplay;
    DisplayWindow refDisplay;
     SummaryMetadata.SummaryMetadataBuilder data_summary;
     SummaryMetadata.SummaryMetadataBuilder ref_summary;
    boolean bOnlineAnalysis;
    String cameraDeviceName;
    private long startMilliseconds;
    private long elapsedMilliseconds;
    boolean shutterOpen;
    
    public RMI() {
        super();
        rmi_form = new RMIControlForm();
        rmi_form.SetContext(this);
        rmi_comment_form = new RMICommentFrame();
        rmi_comment_form.SetContext(this);
        rmi_form.setVisible(true);
        processor = new JRMIProcessor(this);
        canRun = true;
        channels = new TConf[5];
        channels[FLT_TRANS] = new TConf();
        channels[FLT_L1] = new TConf();
        channels[FLT_L2] = new TConf();
        channels[FLT_G] = new TConf();
        channels[FLT_R] = new TConf();
        fDescFile = null;
        fDescFileWriter = null;
        _state = RSTATE.REC_IDLE;
        timer = new java.util.Timer();//create a new Timer
        clockTask = null;
        acqTask = null;     
        
    }
     
    @Override
    public void setContext(Studio _app) {
            core = _app.core();
            app = _app;
     try {   LoadSettings();        } catch (Exception ex) {            Logger.getLogger(RMIControlForm.class.getName()).log(Level.SEVERE, null, ex);        }       
    }
  
    public void SaveSettings()
    {
        Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
        prefs.put("DataHome", DataHome);
        prefs.put("lastCommentFile", lastCommentFile);
        prefs.putFloat("recInterval", recInterval);
        prefs.putFloat("recTime",recTime);
        prefs.putBoolean("writeToDisk",bWriteToDisk);
        prefs.putBoolean("autoRef",bAutoRef);
        prefs.put("channelGroup",channelGroup);
        prefs.put("zoomLive",zoomLive);
        prefs.put("binLive",binLive);
        prefs.put("zoomRec",zoomRec);
        prefs.put("binRec",binRec);
        prefs.putInt("shutterDelay",shutterDelay);
        prefs.putInt("expTrans",channels[FLT_TRANS].exposure);
        prefs.putInt("expL1",channels[FLT_L1].exposure);
        prefs.putInt("expL2",channels[FLT_L2].exposure);
        prefs.putInt("expG",channels[FLT_G].exposure);
        prefs.putInt("expR",channels[FLT_R].exposure);
        prefs.putInt("filterTrans",channels[FLT_TRANS].filterSlot);
        prefs.putInt("filterL1",channels[FLT_L1].filterSlot);
        prefs.putInt("filterL2",channels[FLT_L2].filterSlot);
        prefs.putInt("filterG",channels[FLT_G].filterSlot);
        prefs.putInt("filterR",channels[FLT_R].filterSlot);
        prefs.put("filterTransLabel",channels[FLT_TRANS].label);
        prefs.put("filterL1Label",channels[FLT_L1].label);
        prefs.put("filterL2Label",channels[FLT_L2].label);
        prefs.put("filterGLabel",channels[FLT_G].label);
        prefs.put("filterRLabel",channels[FLT_R].label);
        prefs.putBoolean("captureTrans", channels[FLT_TRANS].capture);
        prefs.putBoolean("captureL1", channels[FLT_L1].capture);
        prefs.putBoolean("captureL2", channels[FLT_L2].capture);
        prefs.putBoolean("captureG", channels[FLT_G].capture);
        prefs.putBoolean("captureR", channels[FLT_R].capture);
        prefs.putBoolean("onlineAnalysis",bOnlineAnalysis);
        prefs.putInt("colorTrans",channels[FLT_TRANS].col.getRGB() );
        prefs.putInt("colorL1",channels[FLT_L1].col.getRGB() );
        prefs.putInt("colorL2",channels[FLT_L2].col.getRGB() );
        prefs.putInt("colorG",channels[FLT_G].col.getRGB() );
        prefs.putInt("colorR",channels[FLT_R].col.getRGB() );
        
        prefs.put("stateDeviceName",stateDeviceName);
        prefs.put("cameraDeviceName",cameraDeviceName);
        
    }
    private void LoadSettings() throws Exception
    {
        Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
        DataHome = prefs.get("DataHome", "d:\\data" );
        lastCommentFile = prefs.get("lastCommentFile", "");
        recInterval = prefs.getFloat("recInterval", 1);
        recTime = prefs.getFloat("recTime",10);
        
        bWriteToDisk = prefs.getBoolean("writeToDisk",true);
        bAutoRef = prefs.getBoolean("autoRef",true);
        bOnlineAnalysis = prefs.getBoolean("onlineAnalysis",true);
        channelGroup = prefs.get("channelGroup","Channels");
        zoomLive = prefs.get("zoomLive","1");
        binLive = prefs.get("binLive","1");
        zoomRec = prefs.get("zoomRec","1");
        binRec = prefs.get("binRec","1");
        shutterDelay = prefs.getInt("shutterDelay",0);
        
        channels[FLT_TRANS].exposure = prefs.getInt("expTrans",10);
        channels[FLT_L1].exposure = prefs.getInt("expL1",50);
        channels[FLT_L2].exposure = prefs.getInt("expL2",50);
        channels[FLT_G].exposure = prefs.getInt("expG",50);
        channels[FLT_R].exposure = prefs.getInt("expR",50);
        channels[FLT_TRANS].filterSlot = prefs.getInt("filterTrans",7);
        channels[FLT_L1].filterSlot = prefs.getInt("filterL1",8);
        channels[FLT_L2].filterSlot = prefs.getInt("filterL2",9);
        channels[FLT_G].filterSlot = prefs.getInt("filterG",2);
        channels[FLT_R].filterSlot = prefs.getInt("filterR",3);
        channels[FLT_TRANS].label = prefs.get("filterTransLabel","Trans");
        channels[FLT_L1].label = prefs.get("filterL1Label","340nm");
        channels[FLT_L2].label = prefs.get("filterL2Label","380nm");
        channels[FLT_G].label = prefs.get("filterGLabel","Blue");
        channels[FLT_R].label = prefs.get("filterRLabel","Green");
        channels[FLT_TRANS].capture = prefs.getBoolean("captureTrans", false);
        channels[FLT_L1].capture = prefs.getBoolean("captureL1", false);
        channels[FLT_L2].capture = prefs.getBoolean("captureL2", false);
        channels[FLT_G].capture = prefs.getBoolean("captureG", false);
        channels[FLT_R].capture = prefs.getBoolean("captureR", false);
        channels[FLT_TRANS].col = new Color( prefs.getInt("colorTrans", Color.WHITE.getRGB()));
        channels[FLT_L1].col = new Color( prefs.getInt("colorL1", Color.MAGENTA.getRGB()));
        channels[FLT_L2].col = new Color( prefs.getInt("colorL2", Color.CYAN.getRGB()));
        channels[FLT_G].col = new Color( prefs.getInt("colorG", Color.GREEN.getRGB()));
        channels[FLT_R].col = new Color( prefs.getInt("colorR", Color.RED.getRGB()));
        stateDeviceName = prefs.get("stateDeviceName","LPT1");
        cameraDeviceName = prefs.get("cameraDeviceName","DCam");
        
        //setFilterExp(FLT_TRANS);
    }

    @Override    public String getName() {      return "Ratiometric Imaging Plugin";   }
    @Override    public String getHelpText() {      return "Implements full stack of actions for single wavelength/ratiometric fluorescent measurments";   }
    @Override    public String getVersion() {        return "v.0.5";        }
    @Override    public String getCopyright() {        return "Sergei Grebenyuk (c) 2015";        }

    void newDataFile() {
        
        Date d = new Date();
        SimpleDateFormat form = new SimpleDateFormat("yyyy-MM-dd@HHmmss");
        DataFile = form.format(d); // set the default name
        rmi_form.setDataFileName(DataFile);
        form = new SimpleDateFormat("yyyy/MM/dd ");
        expDate = form.format(d); // set the default name
        form = new SimpleDateFormat("HH:mm:ss");
        expTime = form.format(d); // set the default name
        
    }

    void setFilterExp(int flt) throws Exception {
        // data = wheel[0-A; 128-B] + speed*16 + filter position
        filterSelected = flt;
        core.setProperty(stateDeviceName, "State", String.valueOf(16 + channels[flt].filterSlot));
        //core.setProperty(core.getCameraDevice(), "Exposure", channels[flt].exposure);
        core.setExposure(core.getCameraDevice(),channels[flt].exposure);
    }
    void setLiveZoom(String zoom) { 
        
        app.live().getDisplay().setMagnification(Float.valueOf(zoom));
        
    }
    void setLiveBin(String bin) throws Exception { 
        core.setProperty(core.getCameraDevice(), "Binning", bin);
        app.live().getDisplay().getImageWindow().invalidate();
    }
    void setRecZoom(String zoom) { 
       dataDisplay.setMagnification(Float.valueOf(zoom));
    }
    
    int InitAcquisition() throws IOException, Exception {
        int chNum = 0;
        for (int i=0; i<5; i++)
        {   // count the number of channels which are checked to be included into acquisition
            if (channels[i].capture)  chNum++;
        }
        if (chNum == 0) {JOptionPane.showMessageDialog(null,"At least one channels must be selected for acquisition."); return 0;}
        if (!bExperimentIsSetUp)
            if (setupNewExperiment(false)==0) 
                return 0;
        
        // Setup variables
        curTime = 0;
        curCycle = 0;
        recCyclesTotal = (int)Math.round( recTime*60.0 / recInterval);
        refCols = new Color[5];
        dataCols = new Color[chNum];
        refNames = new String[5];
        dataNames = new String[chNum];
        int cnt=0; _sweep_delay = 0;
        for (int i=0; i<5; i++)
        {
            if (channels[i].capture) 
            {
                dataCols[cnt] = channels[i].col;
                dataNames[cnt] = channels[i].label;
                _sweep_delay+= channels[i].exposure + shutterDelay;
                cnt++;
            }
            refCols[i] = channels[i].col;
            refNames[i] = channels[i].label;
        }
        
        //Set up acquisition
        core.setCameraDevice(cameraDeviceName);
        core.setProperty(core.getCameraDevice(), "Binning", "1");
        core.setProperty(core.getCameraDevice(), "BitDepth", "16");
            // check if every device is present
            // ... don't know how to do that
        
        // Create data storages and displays
        if (bWriteToDisk)
        {
                          

            //DefaultCoords.Builder imageCoords = new DefaultCoords.Builder();
            //imageCoords.channel(0).stagePosition(0).time(0).z(0);
      
            
            dataStore = app.data().createMultipageTIFFDatastore(acqPath+"\\"+sAcqName, false, true);
            refStore = app.data().createMultipageTIFFDatastore(acqPath+"\\"+sRefName, false, true);
            //refStore = app.data().createSinglePlaneTIFFSeriesDatastore(acqPath+"\\"+sRefName);
            //dataStore = app.data().createSinglePlaneTIFFSeriesDatastore(acqPath+"\\"+sAcqName);
            
        }
        else
        {
            dataStore = app.data().createRAMDatastore();
            refStore = app.data().createRAMDatastore();
        }
        
        app.displays().manage(dataStore);
        app.displays().manage(refStore);
        dataDisplay = app.displays().createDisplay(dataStore);
        refDisplay = app.displays().createDisplay(refStore);
        
        data_summary =  new DefaultSummaryMetadata.Builder();
            data_summary//.name("RMI imaging data")
                 .prefix("data")
                //.userName("John Doe").profileName("John's Profile")
                //.microManagerVersion("made-up version")
                //.metadataVersion("manual metadata").computerName("my arduino")
                .directory(acqPath+"\\"+sAcqName)
                //.comments("Actual imaging data")
                .channelGroup("Channels")
                .channelNames(dataNames)
                //.zStepUm(123456789.012345).waitInterval(-1234.5678)
                //.customIntervalsMs(new Double[] {12.34, 56.78})
                //.axisOrder(new String[] {"Time", "Channel"})
                //.intendedDimensions((new DefaultCoords.Builder()).index("Time", 10).index("Channel", 2).build())
                //.startDate("The age of Aquarius")
                //.stagePositions(new MultiStagePosition[] {new MultiStagePosition("some xy stage", 0, 0, "some z stage", 0) })
                //.userData((new DefaultPropertyMap.Builder()).putString("Ha ha I'm some user data", "and I'm the value").putInt("I'm a number", 42)
                .build();
            
            ref_summary = new DefaultSummaryMetadata.Builder();
            ref_summary//.name("RMI reference data")
                 .prefix("ref")
                .directory(acqPath+"\\"+sRefName)
                //.comments("Reference stack containing all five channels as a")
                .channelGroup("Channels")
                .channelNames(refNames)
                .build();
        
        refStore.setSummaryMetadata(ref_summary.build());
        dataStore.setSummaryMetadata(data_summary.build());            
//        dataStore.setSummaryMetadata(dataStore.getSummaryMetadata().copy().channelNames(dataNames).build());
//        refStore.setSummaryMetadata(refStore.getSummaryMetadata().copy().channelNames(refNames).build());
        
    /*    DisplaySettingsBuilder dsb = (DisplaySettingsBuilder) dataDisplay.getDisplaySettings().copy();
        dsb = dsb.channelColors(dataCols);
        Integer[] maxs={4096,4096};
        Integer[] mins={0,0};
        Double[] gammas={1.0,1.0};
        dsb = dsb.channelContrastMaxes(maxs);
        dsb = dsb.channelContrastMins(mins);
        dsb = dsb.channelGammas(gammas);
        DisplaySettings ds = dsb.build();
        dataDisplay.setDisplaySettings(ds);
        refDisplay.setDisplaySettings(refDisplay.getDisplaySettings().copy().channelColors(refCols).build());
      */  
        
        bAcqNotFastEnough = false;
        adjustedInterval = (int)Math.round( (float)(recInterval)*1000.0 - (float)_sweep_delay);
        if ( adjustedInterval < 0 ) 
        {
            adjustedInterval = 0;
            bAcqNotFastEnough = true;
        }                        
        rmi_form.updateIntervalIndicator(adjustedInterval);

        setBinning(binRec);

        if (bAutoRef){snapReferenceImage();}
        
        DisplayWindow live_win = app.live().getDisplay();
        if (live_win !=null) live_win.getImageWindow().setVisible(false);        
        rmi_form.renderUI(RSTATE.REC_IDLE);        
        return 1;
    }

    public void setBinning(String bin) throws Exception {
        //if ((bin!="1")&&(bin!="2")&&(bin!="4")&&(bin!="8")) return;
        if (getRecState() != RSTATE.REC_RUNNING)
        {
            core.setProperty(core.getCameraDevice(), "Binning", bin);
        }    
    }

    int setupNewExperiment(boolean forceNewStorage) throws IOException {
        if (rmi_form.SetHomeDirectory(DataHome)==0) return 0;
        String path = DataHome+"\\"+DataFile;
        // setup data storage
        File f = new File(path);
        if (!forceNewStorage){
            if (f.isDirectory()&& (nTrialCnt>0)) 
            {
                Object[] options = {"Continue","New autonamed folder","Cancel"};
                int n = JOptionPane.showOptionDialog(null, "Continue experiment "+ DataFile +" ?",    "Warning", JOptionPane.YES_NO_CANCEL_OPTION,    JOptionPane.QUESTION_MESSAGE,    null,    options,    options[1]);
                switch (n){
                    case 0: //continue to current folder
                        sAcqName = String.format("data-%s-%d",DataFile, nTrialCnt);
                        sRefName = String.format("ref-%s-%d",DataFile, nTrialCnt);
                        bExperimentIsSetUp = true;
                        return 1;
                    case 1: //create new folder
                        newDataFile();
                        path = DataHome+"\\"+DataFile;
                        f = new File(path);
                        break;
                    case 2: return 0;
                }
            }
        }
        else
        {
            if (f.isDirectory()) 
            {
                JOptionPane.showMessageDialog(null,"Provide a new acq. name!");
                return 0;
            } 
            
        }
        
        f.mkdir();
        acqPath = path;
        nTrialCnt=0;
        sAcqName = String.format("data-%s-%d",DataFile, nTrialCnt);
        sRefName = String.format("ref-%s-%d",DataFile, nTrialCnt);
        bExperimentIsSetUp = true;
        createDescriptionFile();        
        forceNewStorage = false;
        rmi_comment_form.updateContent();
        return 1;
    }

    void snapReferenceImage() throws InterruptedException,  Exception {
        if (bExperimentIsSetUp)
        {
            
            Coords.CoordsBuilder builder = app.data().getCoordsBuilder();
            builder = builder.stagePosition(0).z(0).time(0);
            
            for (int i=0; i<5; i++)
            {
                setFilterExp(i);
                core.sleep(shutterDelay);
                // Snap an image. Don't display it in the snap/live display.
                refStore.putImage(app.live().snap(false).get(0).copyAtCoords(builder.channel(i).build()));
            }
        }
        else
            JOptionPane.showMessageDialog(null,"Initialization error!");
    }

    void deinitAcquisition() throws InterruptedException {
        if (bWriteToDisk)
        {
            //dataStore.freeze();
            //refStore.freeze();
        }
        else
        {
            dataStore.save(Datastore.SaveMode.MULTIPAGE_TIFF, acqPath);
            refStore.save(Datastore.SaveMode.MULTIPAGE_TIFF, acqPath);        
            //dataStore.save(Datastore.SaveMode.SINGLEPLANE_TIFF_SERIES, acqPath);
            //refStore.save(Datastore.SaveMode.SINGLEPLANE_TIFF_SERIES, acqPath);        
        }
        dataStore=null;
        refStore=null;
        DisplayWindow live_win = app.live().getDisplay();
        if (live_win !=null) live_win.getImageWindow().setVisible(true);        

        rmi_form.renderUI(RSTATE.REC_STOP);
        bExperimentIsSetUp = false;
        nTrialCnt++;
        _state = RSTATE.REC_IDLE;
    }

    void continueExperiment() {
        //String p = DataHome+"\\"+DataFile;
        sAcqName = String.format("data-%s-%d",DataFile, nTrialCnt);
        sRefName = String.format("ref-%s-%d",DataFile, nTrialCnt);
        bExperimentIsSetUp = true;
    }

    int newStorage() throws IOException 
    {
        return setupNewExperiment(true);
    }

    private void createDescriptionFile() throws FileNotFoundException, UnsupportedEncodingException, IOException {
        String newDescriptionFilePath = acqPath+"\\description.txt"; 
        
        //if ((fDescFile!=null) && (fDescFile.exists())) 
        //{ fDescFileWriter.close();fDescFileChannel.close();}
        //fDescription = new PrintWriter(newDescriptionFilePath, "UTF-8");
        
        fDescFile = new File(newDescriptionFilePath);
        fDescFileWriter = new PrintWriter(fDescFile,"UTF-8");
        //fDescFileChannel = new FileOutputStream(fDescFile, true).getChannel();
        //fDescFileChannel.force(true);
        File lastDescr = new File(lastCommentFile); //for ex foo.txt
        FileReader reader = null;
        String content="FILE : "+sAcqName+"\n\nDATE/TIME : "+expDate + " at " +expTime+"\n\nTITLE\n\nCONDITIONS\nTemp : ____°C\n\nSolutions\n\nCOMMENTS\n\nACTIONS\n\n";
        sDescription = content;
        if (lastDescr.exists()&&(lastDescr.length()>2))
        try {
            reader = new FileReader(lastDescr);
            char[] chars = new char[(int) lastDescr.length()-1];
            reader.read(chars);
            content = new String(chars);
            int s=-1;int e=-1;
            s = content.indexOf("DATE/TIME");
            if (s>=0) e = content.indexOf("\n\n",s)+2;
            if ((s>=0)&&(e>=0))
            {
                String sub = content.substring(s, e);
                sDescription = content.replace(sub, "DATE/TIME : "+expDate + " at " +expTime +"\n\n");
            }
            s=-1;e=-1;
            s = content.indexOf("FILE");
            if (s>=0) e = content.indexOf("\n\n",s)+2;
            if ((s>=0)&&(e>=0))
            {
                String sub = content.substring(s, e);
                sDescription = sDescription.replace(sub, "FILE : "+sAcqName+"\n\n");
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(reader !=null){reader.close();}
        }
        lastCommentFile = newDescriptionFilePath;
        fDescFileWriter.print(sDescription);
        fDescFileWriter.flush();        
        fDescFileWriter.close();
        
    }

    @Override
    public String getSubMenu() {
        return "Ratiometric imaging";
    }

    @Override
    public void onPluginSelected() {
        
    }

    void openShutter(boolean selected) throws Exception {
        
        //int state = Integer.valueOf(core.getProperty(stateDeviceName, "State"));
        //if (selected) state|=2<<13;
        //else state&=~(2<<13);
        //core.setProperty(stateDeviceName, "State", String.valueOf(state));
    }
  
    public class AcqTask extends TimerTask {
        @Override
        public void run() {
             if (_state == RSTATE.REC_RUNNING){
                try {
                    curTime = curCycle*recInterval;
                    doImagingSweep(curCycle);
                    curCycle++;
                    rmi_form.renderUI(_state);
                    if (curCycle >= recCyclesTotal) recStop();
                } catch (Exception ex) {
                    Logger.getLogger(RMI.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (_state == RSTATE.REC_STOP){
                 try {
                     deinitAcquisition();
                     acqTask.cancel();
                 } catch (InterruptedException ex) {
                     Logger.getLogger(RMI.class.getName()).log(Level.SEVERE, null, ex);
                 }
            }
        }
    };
     public class ClockTask extends TimerTask {
        @Override
        public void run() {
            if (_state == RSTATE.REC_IDLE){
                    rmi_form.renderUI(RSTATE.REC_RUNNING);
            }
        }
    };

    void recStop() throws InterruptedException  {
        _state = RSTATE.REC_STOP;
        clockTask.cancel();
        rmi_form.renderUI(RSTATE.REC_STOP);
       
    }
    void recReStart() throws Exception {
        if (InitAcquisition()==0) {return;}
        acqPaused = false;    
        msecStart = System.nanoTime();
        rmi_form.renderUI(RSTATE.REC_START);
        _state = RSTATE.REC_RUNNING;
        acqTask = new AcqTask();
        clockTask = new ClockTask();
        timer.scheduleAtFixedRate(acqTask, 0, (long)(recInterval*1000));//this line starts the timer at the 
        timer.scheduleAtFixedRate(clockTask, 0, 1000);//this line starts the timer at the 
        
    }
    public void doImagingSweep(int _sweepNum) throws  Exception
    {

        Coords.CoordsBuilder builder = app.data().getCoordsBuilder();
        builder = builder.stagePosition(0).z(0).time(_sweepNum);
        int cnt=0;
        for (int i=0; i<5; i++)
        {
            if (channels[i].capture) 
            {
                setFilterExp(i);
                core.sleep(shutterDelay);
                // Snap an image. Don't display it in the snap/live display.
                builder = builder.channel(cnt);
                Coords crds= builder.build();
                Image image = app.live().snap(false).get(0).copyAtCoords(builder.build());
                // now copy metadata and set the timestamp in it
                MetadataBuilder md = image.getMetadata().copy();
                if (_sweepNum==0) 
                { 
                    elapsedMilliseconds = startMilliseconds = System.currentTimeMillis();
                }
                else 
                    elapsedMilliseconds = System.currentTimeMillis() - startMilliseconds;
                md.elapsedTimeMs((double)elapsedMilliseconds);
                dataStore.putImage(image.copyWithMetadata(md.build()));
                cnt++;
            }
        }
        
      // Run data processin in separate thread
      
    
    }
    private boolean acqPaused;
    boolean liveMode=false;
    public RSTATE getRecState(){
        return _state;
    }
    public void acqExit(){_state = RSTATE.REC_EXIT;}
    void runLive(boolean go) throws Exception {
        if (go)
        {
            setBinning(binLive);
            setFilterExp(filterSelected);
            
        }
        app.live().setLiveMode(go);
        //app.enableLiveMode(go);
    }

    void runSnap() throws Exception {
        setBinning(binLive);
        setFilterExp(filterSelected);
        //app.snapSingleImage();
        List<Image> images = app.live().snap(true);
    }

    void TogglePausedState() {
        acqPaused = !acqPaused;
        if (acqPaused)
        {
            _state = RSTATE.REC_IDLE;
            rmi_form.setResumeButton(true);
            DisplayWindow live_win = app.live().getDisplay();
            if (live_win !=null) live_win.getImageWindow().setVisible(true);        
            
        }
        else
        {
            _state = RSTATE.REC_RUNNING;
            rmi_form.setResumeButton(false);
            DisplayWindow live_win = app.live().getDisplay();
            if (live_win !=null) live_win.getImageWindow().setVisible(false);        
        }
        //rmi_form.renderUI(_state);
    }
}


