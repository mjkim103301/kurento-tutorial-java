package org.kurento.tutorial.groupcall;

import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.springframework.beans.factory.annotation.Autowired;

public class KurentoManager {
  //  @Autowired
  //  private KurentoClient kurento;

    private MediaPipeline pipeline;

    public KurentoManager(){
 //       pipeline=kurento.createMediaPipeline();
    }
    public MediaPipeline getNewPipeline(){
       // System.out.println("[KurentoManager] kurento: "+kurento);
        //pipeline=kurento.createMediaPipeline();
        System.out.println("[KurentoManager getNewPipeline] pipeline: "+pipeline);
        return pipeline;
    }
    public MediaPipeline getPipeline(){
        return pipeline;
    }
}
