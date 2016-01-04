/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package RMI;

import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sg
 */
enum PSTATE {PROC_IDLE,PROC_RUNNING,PROC_PRERUN,PROC_POSTRUN,PROC_EXIT};
public class ProcThread extends Thread{
    private final RMI rmi;
    public ProcThread(RMI _rmi)
    {
        rmi=_rmi;
    };
    public void setROIs(){
        RoiManager rm = RoiManager.getInstance();
        Roi[] rois = rm.getSelectedRoisAsArray();
        
    };
    private static final Logger LOG = Logger.getLogger(ProcThread.class.getName());
    private PSTATE _state = PSTATE.PROC_IDLE;
    public void run() 
    { 
      boolean go=true;
      while (go){
          switch (_state)
          {
            case PROC_IDLE:
            {
                try { Thread.sleep(100); } catch (InterruptedException ex) {Logger.getLogger(ProcThread.class.getName()).log(Level.SEVERE, null, ex); }
            }
            break;
            case PROC_PRERUN:

            break;
            case PROC_RUNNING:

            break;
            case PROC_POSTRUN:

            break;
            case PROC_EXIT:
              
              
          }
      }
    }
}
