/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.kurento.tutorial.magicmirror;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import org.kurento.client.*;
import org.kurento.jsonrpc.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * Magic Mirror handler (application and media logic).
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.0
 */
public class MagicMirrorHandler extends TextWebSocketHandler {

  private static final Gson gson = new GsonBuilder().create();
  private final Logger log = LoggerFactory.getLogger(MagicMirrorHandler.class);

  private final ConcurrentHashMap<String, UserSession> users = new ConcurrentHashMap<>();

  @Autowired
  private KurentoClient kurento;

  private WebRtcEndpoint webRtcEndpoint;

  private ImageOverlayFilter imageOverlayFilter;
  private int imageIndex;
  private String[] imageUris={"/home/ubuntu/image/flower.jpg", "/home/ubuntu/image/bird.jpg", "/home/ubuntu/image/flower.jpg", "/home/ubuntu/image/bird.jpg"};

  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    JsonObject jsonMessage = gson.fromJson(message.getPayload(), JsonObject.class);

    log.debug("Incoming message: {}", jsonMessage);

    switch (jsonMessage.get("id").getAsString()) {
      case "start":
        start(session, jsonMessage);
        break;
      case "stop": {
        UserSession user = users.remove(session.getId());
        if (user != null) {
          user.release();
        }
        break;
      } case "prev":{
        System.out.println("prev");
        prev(session, jsonMessage);
        break;
      }
      case "next":{
        System.out.println("next");
        next(session, jsonMessage);
        break;
      }
      case "onIceCandidate": {
        JsonObject jsonCandidate = jsonMessage.get("candidate").getAsJsonObject();

        UserSession user = users.get(session.getId());
        if (user != null) {
          IceCandidate candidate = new IceCandidate(jsonCandidate.get("candidate").getAsString(),
              jsonCandidate.get("sdpMid").getAsString(),
              jsonCandidate.get("sdpMLineIndex").getAsInt());
          user.addCandidate(candidate);
        }
        break;
      }
      default:
        sendError(session, "Invalid message with id " + jsonMessage.get("id").getAsString());
        break;
    }
  }

  private void start(final WebSocketSession session, JsonObject jsonMessage) {
    try {
      // User session
      UserSession user = new UserSession();
      MediaPipeline pipeline = kurento.createMediaPipeline();
      user.setMediaPipeline(pipeline);
      //WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();
      webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();
      user.setWebRtcEndpoint(webRtcEndpoint);
      users.put(session.getId(), user);

      // ICE candidates
      webRtcEndpoint.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {

        @Override
        public void onEvent(IceCandidateFoundEvent event) {
          JsonObject response = new JsonObject();
          response.addProperty("id", "iceCandidate");
          response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
          try {
            synchronized (session) {
              session.sendMessage(new TextMessage(response.toString()));
            }
          } catch (IOException e) {
            log.debug(e.getMessage());
          }
        }
      });

      // Media logic
      FaceOverlayFilter faceOverlayFilter = new FaceOverlayFilter.Builder(pipeline).build();

      //String appServerUrl = System.getProperty("app.server.url",
      //    MagicMirrorApp.DEFAULT_APP_SERVER_URL);
      String appServerUrl = "http://files.openvidu.io";
      faceOverlayFilter.setOverlayedImage(appServerUrl + "/img/mario-wings.png", -0.35F, -1.2F,
          1.6F, 1.6F);

      webRtcEndpoint.connect(faceOverlayFilter);
      faceOverlayFilter.connect(webRtcEndpoint);

      //image 필터 씌우기
      imageOverlayFilter=new ImageOverlayFilter.Builder(pipeline).build();
      String imageId = "testImage";
      String imageUri = imageUris[0];
      System.out.println("image start imageId: "+imageId+" imageUri: "+imageUri);
      //imageOverlayFilter.removeImage(imageId);
      imageOverlayFilter.addImage(imageId, imageUri, 0.4f, 0.4f, 0.4f, 0.4f, true, true);
      webRtcEndpoint.connect(imageOverlayFilter);
      imageOverlayFilter.connect(webRtcEndpoint);



      // SDP negotiation (offer and answer)
      String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
      String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);

      JsonObject response = new JsonObject();
      response.addProperty("id", "startResponse");
      response.addProperty("sdpAnswer", sdpAnswer);

      synchronized (session) {
        session.sendMessage(new TextMessage(response.toString()));
      }

      webRtcEndpoint.gatherCandidates();

    } catch (Throwable t) {
      sendError(session, t.getMessage());
    }
  }

  private void prev(final WebSocketSession session, JsonObject jsonMessage) {
    try {
      String imageId = "testImage";
      String imageUri = imageUris[imageIndex];

      imageOverlayFilter.removeImage(imageId);
      imageOverlayFilter.addImage(imageId, imageUri, 0.4f, 0.4f, 0.4f, 0.4f, true, true);
      webRtcEndpoint.connect(imageOverlayFilter);
      imageOverlayFilter.connect(webRtcEndpoint);
      if (imageIndex > 0) {
        imageIndex--;
      } else {
        JsonObject response = new JsonObject();
        response.addProperty("prev", "맨 처음 사진입니다.");


        synchronized (session) {
          session.sendMessage(new TextMessage(response.toString()));
        }
      }
    } catch (Throwable t) {
      sendError(session, t.getMessage());
    }
  }

  private void next(final WebSocketSession session, JsonObject jsonMessage) {
    try{

      String imageId="testImage";
      String imageUri=imageUris[imageIndex];

      imageOverlayFilter.removeImage(imageId);
      imageOverlayFilter.addImage(imageId, imageUri, 0.4f,0.4f,0.4f,0.4f,true,true);
      webRtcEndpoint.connect(imageOverlayFilter);
      imageOverlayFilter.connect(webRtcEndpoint);
      if(imageIndex<3){
        imageIndex++;
      }else{
        JsonObject response = new JsonObject();
        response.addProperty("next", "마지막 사진입니다.");


        synchronized (session) {
          session.sendMessage(new TextMessage(response.toString()));
        }
      }
    }catch (Throwable t) {
      sendError(session, t.getMessage());
    }
  }


  private void sendError(WebSocketSession session, String message) {
    try {
      JsonObject response = new JsonObject();
      response.addProperty("id", "error");
      response.addProperty("message", message);
      session.sendMessage(new TextMessage(response.toString()));
    } catch (IOException e) {
      log.error("Exception sending message", e);
    }
  }
}
