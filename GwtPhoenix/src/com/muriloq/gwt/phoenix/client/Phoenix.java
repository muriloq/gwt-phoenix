package com.muriloq.gwt.phoenix.client;

import java.util.ArrayList;

import gwt.g2d.client.graphics.Color;
import gwt.g2d.client.graphics.KnownColor;
import gwt.g2d.client.graphics.Surface;
import gwt.g2d.client.graphics.canvas.CanvasElement;
import gwt.g2d.client.graphics.canvas.ImageDataAdapter;

import com.google.gwt.event.dom.client.KeyCodes;

/*
	Phoenix Arcade Emulator for GWT
    Official Home-page: http://gwt-phoenix.appspot.com

    Emulator by Murilo Saraiva de Queiroz (muriloq@gmail.com) based on 
    Phoenix Emulator by Richard Davies (R.Davies@dcs.hull.ac.uk) and MAME 
    project, by Nicola Salmoria (MC6489@mclink.it) and others.

    The emulator structure, and many solutions are based in Jasper, the 
    Java Spectrum Emulator, by Adam Davidson & Andrew Pollard.
        Used with permission.
    
    The machine architecture information is from Ralph Kimmlingen
    (ub2f@rz.uni-karlsruhe.de).

Phoenix Hardware Specification
Resolution 26x8 = 208 columns x 32x8 = 256 lines
Phoenix memory map

  0000-3fff 16Kb Program ROM
  4000-43ff 1Kb Video RAM Charset A (4340-43ff variables)
  4400-47ff 1Kb Work RAM
  4800-4bff 1Kb Video RAM Charset B (4840-4bff variables)
  4c00-4fff 1Kb Work RAM
  5000-53ff 1Kb Video Control write-only (mirrored)
  5400-47ff 1Kb Work RAM
  5800-5bff 1Kb Video Scroll Register (mirrored)
  5c00-5fff 1Kb Work RAM
  6000-63ff 1Kb Sound Control A (mirrored)
  6400-67ff 1Kb Work RAM
  6800-6bff 1Kb Sound Control B (mirrored)
  6c00-6fff 1Kb Work RAM
  7000-73ff 1Kb 8bit Game Control read-only (mirrored)
  7400-77ff 1Kb Work RAM
  7800-7bff 1Kb 8bit Dip Switch read-only (mirrored)
  7c00-7fff 1Kb Work RAM
  
  memory mapped ports:
  
    read-only:
    7000-73ff IN
    7800-7bff Dip-Switch Settings (DSW)
    
    * IN (all bits are inverted)
    * bit 7 : barrier
    * bit 6 : Left
    * bit 5 : Right
    * bit 4 : Fire
    * bit 3 : -
    * bit 2 : Start 2
    * bit 1 : Start 1
    * bit 0 : Coin
    
    * Dip-Switch Settings (DSW)
    * bit 7 : VBlank
    * bit 6 : free play (pleiads only)
    * bit 5 : attract sound 0 = off 1 = on (pleiads only?)
    * bit 4 : coins per play	0 = 1 coin	1 = 2 coins
    * bit 3 :\ bonus
    * bit 2 :/ 00 = 3000	01 = 4000  10 = 5000  11 = 6000
    * bit 1 :\ number of lives
    * bit 0 :/ 00 = 3  01 = 4  10 = 5  11 = 6
     
    Pallete
    0 bit 5 of video ram value (divides 256 chars in 8 color sections)
    1 bit 6 of video ram value (divides 256 chars in 8 color sections)
    2 bit 7 of video ram value (divides 256 chars in 8 color sections)
    3 bit 0 of pixelcolor  (either from CHAR-A or CHAR-B, depends on Bit5)
    4 bit 1 of pixelcolor  (either from CHAR-A or CHAR-B, depends on Bit5) 
    5 0 = CHAR-A, 1 = CHAR-B
    6 palette flag (see video control reg.)
    7 always 0
*/



public class Phoenix extends i8080 {
	private Surface canvasGraphics;
	private Surface backGraphics;
	private Surface frontGraphics;
	private ImageDataAdapter backImageData;
	private ImageDataAdapter frontImageData;
	
    private Color characters[][]; // decoded characters, for each palette

//    private AudioClip laserSFX = null;
//    private AudioClip explosionSFX = null;
//    private AudioClip blowSFX = null;
//    private AudioClip shieldSFX = null;
//    private AudioClip hitSFX = null;
    private int savedHiScore=0;

    public static final int WIDTH_PIXELS = 208;
    public static final int HEIGHT_PIXELS = 256;
    public static final int SCALE_PIXELS = 1;
    private static final int WIDTH = Phoenix.WIDTH_PIXELS * Phoenix.SCALE_PIXELS;
	private static final int HEIGHT = Phoenix.HEIGHT_PIXELS * Phoenix.SCALE_PIXELS;

    private byte[] chr = new byte [0x2000]; // CHARSET roms

    private boolean vBlank = false;    
    private int scrollRegister   = 0;
    private int oldScrollRegister  = 0;
    private int palette = 0;
    
    private boolean backgroundRefresh = true;
    private boolean foregroundRefresh = true;
    private ArrayList<Integer> dirtyForeground = new ArrayList<Integer>();
    private ArrayList<Integer> dirtyBackground = new ArrayList<Integer>();
    
    private int[] gameControl = new int [8];
    private int interruptCounter = 0;
    private int frameSkip = 1;
    public  long timeOfLastFrameInterrupt = 0;
    private long  timeNow;
    private long  timeBefore;
    private float framesPerSecond;
    public int msPerFrame = 1000/60;
    
    // R, G, B, Alpha
    Color OPAQUE_BLACK = new Color(0, 0, 0, 1.0); // opaque!
    Color BLACK = new Color(0, 0, 0, 0.0); // transparent!
    Color WHITE = new Color(0xdb, 0xdb, 0xdb, 1.0);
    Color RED = new Color(0xff, 0, 0, 1.0);
    Color GREEN = new Color(0, 0xff, 0, 1.0);
    Color BLUE = new Color(0x24, 0x24, 0xdb, 1.0);
    Color CYAN = new Color(0, 0xff, 0xdb, 1.0);
    Color YELLOW = new Color(0xff, 0xff, 00, 1.0);
    Color PINK = new Color(0xff, 0xb6, 0xdb, 1.0);
    Color ORANGE = new Color(0xff, 0xb6, 0x49, 1.0);
    Color LTPURPLE = new Color(0xff, 0x24, 0xb6, 1.0);
    Color DKORANGE = new Color(0xff, 0xb6, 0x00, 1.0);
    Color DKPURPLE = new Color(0xb6, 0x24, 0xff, 1.0);
    Color DKCYAN = new Color(0x00, 0xdb, 0xdb, 1.0);
    Color DKYELLOW = new Color(0xdb, 0xdb, 0x00, 1.0);
    Color BLUISH = new Color(0x95, 0x95, 0xff, 1.0);
    Color PURPLE = new Color(0xff, 0x00, 0xff, 1.0);
    
    // pallete x charset x character = color 
    // 4 colors per pixel * 8 groups of characters * 2 charsets * 2 pallettes
    Color colorTable[]={
        /* charset A pallette A */
        BLACK,BLACK,CYAN,CYAN,      // Background, Unused, Letters, asterisks
        BLACK,YELLOW,RED,WHITE,     // Background, Ship middle, Numbers/Ship, Ship edge
        BLACK,YELLOW,RED,WHITE,     // Background, Ship middle, Ship, Ship edge/bullets
        BLACK,PINK,PURPLE,YELLOW,   // Background, Bird eyes, Bird middle, Bird Wings
        BLACK,PINK,PURPLE,YELLOW,   // Background, Bird eyes, Bird middle, Bird Wings
        BLACK,PINK,PURPLE,YELLOW,   // Background, Bird eyes, Bird middle, Bird Wings
        BLACK,WHITE,PURPLE,YELLOW,  // Background, Explosions
        BLACK,PURPLE,GREEN,WHITE,   // Background, Barrier
        /* charset A pallette B */
        BLACK,BLUE,CYAN,CYAN,       // Background, Unused, Letters, asterisks
        BLACK,YELLOW,RED,WHITE,     // Background, Ship middle, Numbers/Ship, Ship edge
        BLACK,YELLOW,RED,WHITE,     // Background, Ship middle, Ship, Ship edge/bullets
        BLACK,YELLOW,GREEN,PURPLE,  // Background, Bird eyes, Bird middle, Bird Wings
        BLACK,YELLOW,GREEN,PURPLE,  // Background, Bird eyes, Bird middle, Bird Wings
        BLACK,YELLOW,GREEN,PURPLE,  // Background, Bird eyes, Bird middle, Bird Wings
        BLACK,WHITE,RED,PURPLE,     // Background, Explosions
        BLACK,PURPLE,GREEN,WHITE,   // Background, Barrier
        /* charset B pallette A */
        BLACK,RED,BLUE,WHITE,           // Background, Starfield
        BLACK,PURPLE,BLUISH,DKORANGE,   // Background, Planets
        BLACK,DKPURPLE,GREEN,DKORANGE,  // Background, Mothership: turrets, u-body, l-body
        BLACK,BLUISH,DKPURPLE,LTPURPLE, // Background, Motheralien: face, body, feet
        BLACK,PURPLE,BLUISH,GREEN,      // Background, Eagles: face, body, shell
        BLACK,PURPLE,BLUISH,GREEN,      // Background, Eagles: face, body, feet
        BLACK,PURPLE,BLUISH,GREEN,      // Background, Eagles: face, body, feet
        BLACK,PURPLE,BLUISH,GREEN,      // Background, Eagles: face, body, feet
        /* charset B pallette B */
        BLACK,RED,BLUE,WHITE,           // Background, Starfield
        BLACK,PURPLE,BLUISH,DKORANGE,   // Background, Planets
        BLACK,DKPURPLE,GREEN,DKORANGE,  // Background, Mothership: turrets, upper body, lower body
        BLACK,BLUISH,DKPURPLE,LTPURPLE, // Background, Motheralien: face, body, feet
        BLACK,BLUISH,LTPURPLE,GREEN,    // Background, Eagles: face, body, shell
        BLACK,BLUISH,LTPURPLE,GREEN,    // Background, Eagles: face, body, feet
        BLACK,BLUISH,LTPURPLE,GREEN,    // Background, Eagles: face, body, feet
        BLACK,BLUISH,LTPURPLE,GREEN,    // Background, Eagles: face, body, feet
    };
	private int sleepTime;
    private CanvasElement backCanvas;
    private CanvasElement frontCanvas;
    private boolean scrollRefresh;
	

//    private Sound sound;

    public Phoenix(Surface canvas){
        // Phoenix runs at 0.74 Mhz (?)
        super(0.74);
        
		this.canvasGraphics = canvas;
		this.backGraphics = new Surface(WIDTH, HEIGHT);
		this.backImageData = backGraphics.createImageData(WIDTH, HEIGHT);
		this.frontGraphics = new Surface(WIDTH, HEIGHT);
		this.frontImageData = frontGraphics.createImageData(WIDTH, HEIGHT);


        for ( int i=0;i<8;i++ ) gameControl[i]=1;
//        sound = new Sound();
    }


    /** Byte access */
    public void pokeb( int addr, int newByte ) {

        addr &= 0xffff;

        if ( addr >=  0x5800 && addr <= 0x5bff ) {
            scrollRegister = newByte;
            if ( scrollRegister != oldScrollRegister ) {
                oldScrollRegister = scrollRegister;
                scrollRefresh = true;
            }
        }
        
        // Write on foreground
        if ( (addr >= 0x4000) && (addr <= 0x4340) ){
            dirtyForeground.add(addr);
            foregroundRefresh = true; 
        }
        
        if ( (addr >= 0x4800)&&(addr <= 0x4b40) ) {
            dirtyBackground.add(addr);
            backgroundRefresh = true;  
        }

        if ( addr >= 0x5000 && addr <= 0x53ff ) {
            palette = newByte & 0x01; 
        }
        
//        if ( addr >= 0x6000 && addr <= 0x63ff && sound!=null) {
//            if ( peekb(addr)!=newByte ) {
//                mem[addr] = newByte;
//                //System.out.println ("Sound Control A:"+newByte);
//                //sound.updateControlA((byte)newByte);
//                if ( newByte==143 ) explosionSFX.play ();
//                if ( (newByte>101)&&(newByte<107) ) laserSFX.play ();
//                if ( newByte==80 ) blowSFX.play ();
//            }
//        }

//        if ( addr >= 0x6800 && addr <= 0x6bff && sound!=null ) {
//            if ( peekb(addr)!=newByte ) {
//                mem[ addr ] = newByte;
//                //System.out.println ("Sound Control B:"+newByte);  
//                sound.updateControlB((byte) newByte);
//                if ( newByte==12 ) shieldSFX.play ();
//                if ( newByte==2 ) hitSFX.play ();
//            }
//        }

        // Hi Score Saving - Thanks MAME ! :)
        if ( addr == 0x438c ) {
            if ( newByte == 0x0f ) {
                mem[addr]=newByte;
                int hiScore = getScore(0x4388);
                if ( hiScore > savedHiScore ) hisave();
                if ( hiScore < savedHiScore ) hiload();
            }
        }

        if ( addr >= 0x4000 ) {   // 0x0000 - 0x3fff Program ROM 
            mem [addr]=newByte; 
        }

        return;
    }

    /** Word access */
    public void pokew( int addr, int word ) {
        addr &= 0xffff;
        int _mem[] = mem;
        if ( addr >= 0x4000 ) {
            _mem[ addr ] = word & 0xff;
            if ( ++addr != 65536 ) {
                _mem[ addr ] = word >> 8;
            }
        }
        return;
    }

    public int peekb(int addr) {
        addr &= 0xffff;
        
        // are we reading dip switch memory ?
        if (addr >= 0x7800 && addr <= 0x7bff) { 
            // if SYNC bit of switch is 1
            if (vBlank) { 
                vBlank = false; // set it to 0
                return 128;     // return value where bit 7 is 1
            } else
                return 0;       // return value where bit 7 is 0
        }
        
        // are we reading the joystick ?
        if (addr >= 0x7000 && addr <= 0x73ff) {
            int c = 0;
            for (int i = 0; i < 8; i++)
                c |= gameControl[i] << i;
            return c;
        }

        // we are reading a standard memory address
        else
            return mem[addr];
    }

    public int peekw( int addr ) {
        addr &= 0xffff;
        int t = peekb( addr );
        addr++;
        return t | (peekb( addr ) << 8);
    }

    
//    public void initSFX(Applet a) {
//        // Sound Effects loading
//        applet = a;
//        URL baseURL = applet.getDocumentBase();
//        laserSFX = applet.getAudioClip(baseURL,"laser.au");
//        laserSFX.play();
//        laserSFX.stop();
//
//        explosionSFX = applet.getAudioClip(baseURL,"explo.au");
//        explosionSFX.play();
//        explosionSFX.stop();
//
//        blowSFX = applet.getAudioClip(baseURL,"blow.au");
//        blowSFX.play();
//        blowSFX.stop();
//
//        shieldSFX = applet.getAudioClip(baseURL,"shield.au");
//        shieldSFX.play();
//        shieldSFX.stop();
//
//        hitSFX = applet.getAudioClip(baseURL,"hit.au");
//        hitSFX.play();
//        hitSFX.stop();
//
//    }

    // The Hi Score is BCD (Binary Coded Decimal).
    // We convert this to integer here.
    public int getScore(int Addr) {
        int score=0;
        score += (int) (peekb (Addr+3)/16) * 10     + (peekb (Addr+3) % 16);
        score += (int) (peekb (Addr+2)/16) * 1000   + (peekb (Addr+2) % 16)*100;
        score += (int) (peekb (Addr+1)/16) * 100000   + (peekb (Addr+1) % 16)*10000;
        score += (int) (peekb (Addr)  /16) * 10000000 + (peekb (Addr) % 16)*1000000;
        return score;
    }

    public void hisave () {
        // Hi score saving. Again, thanks MAME project... :)
        int OneScore=getScore(0x4380);
        int TwoScore=getScore(0x4384);
        int HiScore=getScore(0x4388);
        int HiAddress = 0x4388;
        if ( OneScore > HiScore ) HiAddress=0x4380;
        if ( TwoScore > HiScore ) HiAddress=0x4384;


//        try {
//            // URL baseURL = applet.getDocumentBase();
//            OutputStream os;
//            /*
//            if (baseURL != null) {
//            URL scoreURL = new URL (baseURL, "hiscore.sav");
//            URLConnection connection = new URLConnection (scoreURL);
//            os = connection.getOutputStream();				
//            }
//            else {
//            */
//            File scoreFile = new File ("hiscore.sav");
//            os = new FileOutputStream (scoreFile);
//            //}
//            for ( int i=0;i<4;i++ ) {
//                os.write ((byte) peekb(HiAddress+i));
//            }
//            os.flush();
//            os.close();
//        } catch ( Exception e ) {
//            System.out.println ("Error saving high score");
//        }
        savedHiScore = getScore (HiAddress);
        System.out.println ("High Score: "+savedHiScore+" saved.");
    }

    public void hiload () {
        int HiAddress = 0x4388;
//        try {
//            // URL baseURL = applet.getDocumentBase();
//            InputStream is;
//            /*
//            if (baseURL != null) {
//            URL scoreURL = new URL (baseURL, "hiscore.sav");
//            URLConnection connection = new URLConnection (scoreURL);
//            os = connection.getOutputStream();				
//            }
//            else {
//            */
//            File scoreFile = new File ("hiscore.sav");
//            is = new FileInputStream (scoreFile);
//            //}
//            for ( int i=0;i<4;i++ ) mem [HiAddress+i]=is.read ();
//            is.close();
//        } catch ( Exception e ) {
//            System.out.println ("Error loading high score");
//        }
        // Force hi score atualizing
        pokeb(0x41e1, (peekb(0x4389) / 16)+0x20);
        pokeb(0x41c1, (peekb(0x4389) & 0xf)+0x20);
        pokeb(0x41a1, (peekb(0x438a) / 16)+0x20);
        pokeb(0x4181, (peekb(0x438a) & 0xf)+0x20);
        pokeb(0x4161, (peekb(0x438b) / 16)+0x20);
        pokeb(0x4141, (peekb(0x438b) & 0xf)+0x20);

        savedHiScore = getScore(HiAddress);
        System.out.println ("High Score: "+savedHiScore+" loaded.");
    }

    /** 
     * 0x0000 - 0x3FFF: Program ROM
     * 0x4000 - 0x5FFF: Graphics ROM
     * @param buffer
     */
    public void loadRoms(byte[] buffer){
        for ( int i=0;i<=0x3fff;i++ ) {
            mem[i]=(buffer[i]+256)&0xff;
        }
        for ( int i=0;i<=0x1fff;i++ ) {
            chr[i]=buffer[i+0x4000];
        }
    }
 
    public final int interrupt() {
        interruptCounter++;

        vBlank = true;

        if (interruptCounter % getFrameSkip() == 0)
            screenRefresh();

        // Update speed indicator every second
        if ((interruptCounter % 60) == 0) {
            timeNow = System.currentTimeMillis();
            msPerFrame = (int) (timeNow - timeBefore); // ms / frame
            framesPerSecond = 1000 / (msPerFrame / 60); // frames / s
            timeBefore = timeNow;
        }

        return super.interrupt();
    }


    public void screenRefresh () {
        if ( (!backgroundRefresh && !foregroundRefresh) && !scrollRefresh) return; 
        
        if (backgroundRefresh) {
            for (int a: dirtyBackground) {
                int base = a - 0x4800;
                int x = 25 - (base / 32);
                int y = base % 32;
                int character = mem[a];
                for ( int i=0;i<8;i++ ) {
                    for ( int j=0;j<8;j++ ) {
                        Color c = characters[palette][character*64+j+i*8];
                        backImageData.setColor(x*8+j,y*8+i, c);
                    }
                }
            }
            backGraphics.putImageData(backImageData, 0, 0);
            backCanvas = backGraphics.getCanvas();
            backgroundRefresh = false;
            dirtyBackground.clear();
        }
         
        if (foregroundRefresh) {
            for (int a: dirtyForeground) {
                int base = a - 0x4000;
                int x = 25 - (base / 32);
                int y = base % 32;
                int character = mem[a];
                for ( int i=0;i<8;i++ ) {
                    for ( int j=0;j<8;j++ ) {
                        Color c = characters[palette][64*256+character*64+j+i*8];
                        frontImageData.setColor(x*8+j,y*8+i, c);
                    }
                }
            }
            frontGraphics.putImageData(frontImageData, 0, 0);
            frontCanvas = frontGraphics.getCanvas();
            foregroundRefresh = false;
            dirtyForeground.clear();
        }

        canvasGraphics.setFillStyle(OPAQUE_BLACK);
        canvasGraphics.fillRectangle(0,0,WIDTH,HEIGHT);
        
		canvasGraphics.drawImage(backCanvas, 0,HEIGHT-scrollRegister);
        canvasGraphics.drawImage(backCanvas, 0,-scrollRegister);
        scrollRefresh = false; 
        
        canvasGraphics.drawImage(frontCanvas, 0,0); 
        
        canvasGraphics.setFillStyle(DKYELLOW);
        canvasGraphics.fillText(Integer.toString((int)framesPerSecond),0,255);
        if (getFrameSkip() != 1){
            canvasGraphics.setFillStyle(GREEN);
            canvasGraphics.fillText(Integer.toString((int)getFrameSkip()),WIDTH-16,255);
        }
    }

    public void decodeChars () {
        characters = new Color[2][512*64];

        for ( int s=0;s<2;s++ ) {             // Charset
            for ( int c=0;c<256;c++ ) {         // Character
                byte[][] block = new byte[8][8];
                for ( int plane=0; plane<2;plane++ ) {  // Bit plane
                    for ( int line=0; line<8; line++ ) {  // line
                        byte b = (byte) chr[s*4096+c*8+plane*256*8+line];
                        byte[] bin= new byte[8];                         // binary representation 
                        bin[0]=(byte) ((b & 1)>>0);
                        bin[1]=(byte) ((b & 2)>>1);
                        bin[2]=(byte) ((b & 4)>>2);
                        bin[3]=(byte) ((b & 8)>>3);
                        bin[4]=(byte) ((b & 16)>>4);
                        bin[5]=(byte) ((b & 32)>>5);
                        bin[6]=(byte) ((b & 64)>>6);
                        bin[7]=(byte) ((b & 128)>>7);
                        for ( int col=0;col<8;col++ ) {   // Coluna
                            block[line][col]+= (1+(1-plane))*bin[col];
                            int pixelColorIndex = 0; 
                             pixelColorIndex = (( (c & 0xff) >> 5 )&0xff)*4; // bits 5-7 of video ram value
                             pixelColorIndex += block[line][col];            // pixel color
                             pixelColorIndex += (1-s) * 64;                  // charset

//                             // Draw characters on screen
//                             if ( (block[line][col]>0) ) {
//                                 canvasGraphics.setFillStyle(colorTable[pixelColorIndex]);
//                             } else {
//                                 canvasGraphics.setFillStyle(BLACK);
//                             }
//                             canvasGraphics.fillRectangle(7-line+(c%26)*8,col+((int)c/26)*8+s*160,1,1);

                             // Palette A
                             Color color = colorTable[pixelColorIndex];
                             if (color.getColorCode().equals(BLACK.getColorCode())) 
                                 color = KnownColor.TRANSPARENT; 
                             characters[0][s*256*64+c*64+col*8+7-line] = color;
                             
                             // Palette B
                             color = colorTable[pixelColorIndex+32];
                             if (color.getColorCode().equals(BLACK.getColorCode())) 
                                 color = KnownColor.TRANSPARENT; 
                             characters[1][s*256*64+c*64+col*8+7-line] = color;

                        } // for col
                    } // for line
                } // for plane
            } // for c

        } // for s
    }

    public final boolean doKey( int down, int ascii) {
    	switch ( ascii ) {
        case '3':  	gameControl[0]=1-down;    break; // Coin
        case '1':  	gameControl[1]=1-down;    break; // Start 1
        case '2':  	gameControl[2]=1-down;    break; // Start 2
        case 32 :  	gameControl[4]=1-down;    break; // Fire 
        case KeyCodes.KEY_RIGHT:   gameControl[5]=1-down;   break; // Right
        case KeyCodes.KEY_LEFT:   	 gameControl[6]=1-down;  break; // Left
        case KeyCodes.KEY_DOWN:   gameControl[7]=1-down;    break; // Barrier
        case KeyCodes.KEY_PAGEDOWN:  setFrameSkip(getFrameSkip() - down); 
            if ( getFrameSkip() < 1 ) setFrameSkip(1);             // Decrease frame skip
            break;
        case KeyCodes.KEY_PAGEUP:  setFrameSkip(getFrameSkip() + down);       // Increase frame skip
            break;
        }
        return true;
    }

	public float getSleepTime() {
		return sleepTime;
	}

    public void setFrameSkip(int frameSkip) {
        this.frameSkip = frameSkip;
    }

    public int getFrameSkip() {
        return frameSkip;
    }


    public float getFramesPerSecond() {
        return framesPerSecond;
    }
	
}