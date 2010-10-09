package com.muriloq.gwt.phoenix.client;

import gwt.g2d.client.graphics.Surface;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * @author muriloq@gmail.com
 */
public class GwtPhoenix implements EntryPoint {

    private final PhoenixServiceAsync phoenixService = GWT.create(PhoenixService.class);
    private Phoenix phoenix;

    private long sleepTime;
    private long timeNow;
    private long timeBefore;
    private long interruptCounter;

    /**
     * This is the entry point method.
     */
    public void onModuleLoad() {
        Surface surface = new Surface(Phoenix.WIDTH_PIXELS * Phoenix.SCALE_PIXELS, Phoenix.HEIGHT_PIXELS * Phoenix.SCALE_PIXELS);
        RootPanel.get().add(surface);
        surface.getElement().setAttribute("style", "width: " + Phoenix.WIDTH_PIXELS + "px;");
        surface.getElement().focus();

        phoenix = new Phoenix(surface);

        surface.addKeyDownHandler(new KeyDownHandler() {
            @Override
            public void onKeyDown(KeyDownEvent event) {
                phoenix.doKey(1, event.getNativeKeyCode());
            }
        });

        surface.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent event) {
                phoenix.doKey(0, event.getNativeKeyCode());
            }
        });

        phoenixService.loadRoms(new AsyncCallback<byte[]>() {

            private Timer timer;

            @Override
            public void onFailure(Throwable caught) {
                caught.printStackTrace();
            }

            @Override
            public void onSuccess(byte[] result) {

                phoenix.loadRoms(result);
                // phoenix.initSFX(this);
                phoenix.decodeChars();
                phoenix.hiload();

                timer = new Timer() {

                    public void run() {
                        timeBefore = System.currentTimeMillis();
                        boolean busy = false;
                        while (true) {
                            phoenix.cycles++;
                            int pc = phoenix.PC();

                            // After rendering a frame, the program enters in a
                            // busy wait
                            // that we don't need emulate. Skipping it increases
                            // performance
                            // drastically. Here's the 8085 asm code:
                            //
                            // 128 MVI H,78h
                            // 130 MOV A,(HL) // HL=0x78** : memory mapped
                            // dipswitches and VSYNC register
                            // 131 AND A,80H // BIT 7 is true during VBLANK
                            // 133 JZ 128 // busy wait until VBLANK
                            //
                            // Testing if VBLANK is true actually resets VBLANK
                            // (it's a test and set operation).
                            // So we need to run the busy wait at least once:
                            // that's why we need the "busy" flag.
                            if ((!busy) && (pc == 128))
                                busy = true;
                            else if (busy && (pc == 128)) {
                                phoenix.cycles = 0;
                            }

                            if (phoenix.cycles == 0) {
                                phoenix.interrupt();
                                interruptCounter++;
                                timeNow = System.currentTimeMillis();
                                int msPerFrame = (int) (timeNow - timeBefore);
                                sleepTime = 1000 / 60 - msPerFrame;
                                
                                phoenix.cycles = -phoenix.cyclesPerInterrupt;
                                
                                if (phoenix.getFramesPerSecond() > 60) {
                                    int frameSkip = phoenix.getFrameSkip();
                                    phoenix.setFrameSkip(frameSkip > 1 ? frameSkip -1 : 1);
                                }
                                
                                if (phoenix.getFramesPerSecond() < 60) {
                                    int frameSkip = phoenix.getFrameSkip();
                                    phoenix.setFrameSkip(frameSkip < 5 ? frameSkip + 1 : 5);
                                }
                                
                                if (sleepTime > 0) {
                                    timer.schedule((int) sleepTime);
                                } else {
                                    timer.schedule(1);
                                }
                                break;
                            }
                            phoenix.execute();
                        }
                    }
                };
                // Start program execution
                timer.schedule(1);
            }
        });
    }
}