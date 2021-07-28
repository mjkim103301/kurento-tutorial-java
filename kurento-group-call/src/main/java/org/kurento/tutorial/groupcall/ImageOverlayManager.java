package org.kurento.tutorial.groupcall;

import org.kurento.client.ImageOverlayFilter;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.springframework.beans.factory.annotation.Autowired;

public class ImageOverlayManager extends RoomManager{
    private String[] imageUris={"/home/ubuntu/image/flower.jpg", "/home/ubuntu/image/bird.jpg","/home/ubuntu/image/cat.jpg", "/home/ubuntu/image/cat2.jpg","/home/ubuntu/image/pets.jpg"};
    private int imageIndex;
    private int imageSize=5;
    private String imageId="Test Image";
    private String imageUri=imageUris[imageIndex];


    private MediaPipeline pipeline;
    private ImageOverlayFilter imageOverlayFilter;
    private WebRtcEndpoint webRtcEndpoint;
    public ImageOverlayManager(){}
    public ImageOverlayManager(MediaPipeline pipeline){
        pipeline=pipeline;
       // System.out.println("[ImageOverlayManager] pipeline: "+pipeline);
        imageOverlayFilter=new ImageOverlayFilter.Builder(pipeline).build();
       webRtcEndpoint= new WebRtcEndpoint.Builder(pipeline).build();
    }



    public boolean nextImage(){
        if(imageIndex>=imageSize-1){
            return false;
        }

        imageUri=imageUris[imageIndex];
//        imageOverlayFilter.removeImage(imageId);
//        imageOverlayFilter.addImage(imageId, imageUri, 0.6f, 0.4f, 0.4f, 0.4f, true, true);
//        webRtcEndpoint.connect(imageOverlayFilter);
//        imageOverlayFilter.connect(webRtcEndpoint);
        return true;
    }

    public boolean previousImage(){
        if(imageIndex<=0){
            return false;
        }

        imageUri=imageUris[imageIndex];
//        imageOverlayFilter.removeImage(imageId);
//        imageOverlayFilter.addImage(imageId, imageUri, 0.6f, 0.4f, 0.4f, 0.4f, true, true);
//        webRtcEndpoint.connect(imageOverlayFilter);
//        imageOverlayFilter.connect(webRtcEndpoint);
        return true;
    }

    public void addImage() {
        System.out.println("[ImageOverlay addImage] imageId: "+imageId+" imageUri: "+imageUri);
        imageOverlayFilter.addImage(imageId, imageUri, 0.6f, 0.4f, 0.4f, 0.4f, true, true);
        webRtcEndpoint.connect(imageOverlayFilter);
        imageOverlayFilter.connect(webRtcEndpoint);
    }

    public void removeImage() {
//        imageOverlayFilter.removeImage(imageId);
//        webRtcEndpoint.connect(imageOverlayFilter);
//        imageOverlayFilter.connect(webRtcEndpoint);
    }
}
