import javax.swing.*;
import javax.sound.midi.*;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.event.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class BulletHellGame extends JPanel
        implements ActionListener, KeyListener, MouseListener, MouseMotionListener {

    static final int WIDTH = 600;
    static final int HEIGHT = 800;

    static final int STATE_MENU = 0;
    static final int STATE_SETTINGS = 1;
    static final int STATE_DIFF_SEL = 2;
    static final int STATE_CLASS_SEL = 5;
    static final int STATE_PLAYING = 3;
    static final int STATE_GAME_OVER = 4;
    static final int STATE_WAVE10_CHOICE = 9;
    static final int STATE_SHOP = 7;
    static final int STATE_PAUSE = 8;

    static final int FIRE_MOUSE = 0;
    static final int FIRE_SPACE = 1;

    static final int PU_DOUBLE_SHOT = 0;
    static final int PU_SHIELD = 1;
    static final int PU_SPEED_BOOST = 2;
    static final int PU_SCORE_BURST = 3;
    static final int PU_HEAL = 4;
    static final int PU_COUNT = 5;

    // Machine Gunner heat
    static final int MAX_HEAT = 100;
    static final int HEAT_PER_SHOT = 5;
    static final int HEAT_COOL_RATE = 1;
    static final int OVERHEAT_FRAMES = 120;
    static final int FIRE_RATE = 4;

    // Classes (Storm=6 and Bomber=3 removed)
    static final int CLASS_MACHINE_GUNNER = 0;
    static final int CLASS_NOVA = 1;
    static final int CLASS_PHANTOM = 2;
    static final int CLASS_VIPER = 3;
    static final int CLASS_COUNT = 4;

    static final int NOVA_CHARGE_FRAMES = 180;
    static final int NOVA_COOLDOWN_FRAMES = 60;
    static final int NOVA_LASER_FRAMES = 18;
    static final int NOVA_LASER_WIDTH = 18;
    // Phantom
    static final int PHANTOM_DASH_CD = 180;
    static final int PHANTOM_DECOY_LIFE = 120;

    // Viper
    static final int VIPER_FIRE_RATE = 30;
    static final int VIPER_MAX_SNAKES = 6;

    // ── Explosion particles ───────────────────────────────────────────
    private final ArrayList<ExplosionParticle> explosionParticles = new ArrayList<>();

    // ── Scenery ───────────────────────────────────────────────────────
    static final int SCENE_SPACE = 0;
    static final int SCENE_MARS = 1;
    static final int SCENE_EARTH = 2;
    static final int SCENE_ALIEN = 3;
    static final int SCENE_ASTEROID = 4;
    static final int SCENE_NEBULA = 5;
    static final int SCENE_VOID = 6;
    static final int SCENE_STORM = 7;
    static final int SCENE_ICE = 8;
    static final int SCENE_JUNGLE = 9;
    static final int SCENE_JAPAN = 10;
    static final int SCENE_KITSUNE = 11;
    static final int SCENE_COUNT = 12;
    private int currentScene = SCENE_SPACE;

    // ── Shop ─────────────────────────────────────────────────────────
    // These are SHOP-ONLY permanent upgrades — never drop from boss
    static final int SHOP_EXTRA_LIFE = 0; // +1 life (instant)
    static final int SHOP_SPEED_BOOST = 1; // move faster — permanent
    static final int SHOP_RAPID_FIRE = 2; // 2× fire rate — permanent
    static final int SHOP_BULLET_TIME = 3; // enemy bullets 50% slower — permanent
    static final int SHOP_SCORE_RUSH = 4; // 2× score — permanent
    static final int SHOP_PHASE_SHIFT = 5; // invincible frame on dash — permanent
    static final int SHOP_NUKE = 6; // clear all bullets (instant)
    static final int SHOP_REPAIR = 7; // restore 1 HP (instant)
    static final int SHOP_VOID_MAGNET = 8; // repel bullets — timed, Q to activate
    static final int SHOP_ECHO_SHOT = 9; // echo bullet — permanent
    static final int SHOP_DEATH_MARK = 10; // next hit kills boss — one use
    static final int SHOP_POOL_SIZE = 11;
    static final int SHOP_OFFERED = 3;

    static final String[] SHOP_NAMES = {
            "BLOOD PACT", // extra life
            "AETHER STRIDE", // speed
            "FRENZY CORE", // rapid fire
            "CLOCK FRACTURE", // bullet time
            "GOLD RUSH", // score rush
            "GHOST WALK", // phase shift / invincibility
            "SINGULARITY", // nuke
            "NANO MEND", // repair
            "VOID MAGNET", // repel bullets
            "ECHO SHOT", // echo bullet
            "DEATH MARK" // one-hit kill
    };
    static final String[] SHOP_DESCS = {
            "+1 life permanently",
            "Move speed +2 forever",
            "2× fire rate forever",
            "Bullets half speed (10s)",
            "2× score per kill forever",
            "Invincible on hit (5s)",
            "Clears all enemy bullets",
            "Restore 1 HP",
            "Repels bullets (8s) [Q]",
            "Fires an echo bullet forever",
            "Next hit instantly kills boss"
    };
    static final String[] SHOP_FLAVORS = {
            "\"Death? Not today.\"",
            "\"They can't hit what they can't see.\"",
            "\"Blink and you miss it.\"",
            "\"Time bends. You don't.\"",
            "\"Every kill, doubled.\"",
            "\"You were never here.\"",
            "\"Everything. Gone.\"",
            "\"Patching wounds mid-war.\"",
            "\"Nothing comes close.\"",
            "\"One shot. Two wounds.\"",
            "\"One touch. Game over.\""
    };
    // 0=Common 1=Rare 2=Legendary
    static final int[] SHOP_RARITY = { 1, 0, 1, 2, 1, 2, 1, 0, 1, 0, 2 };
    static final String[] RARITY_LABEL = { "COMMON", "RARE", "LEGENDARY" };
    static final Color[] RARITY_COLOR = {
            new Color(160, 160, 180),
            new Color(80, 160, 255),
            new Color(255, 180, 0)
    };
    static final int[] SHOP_COSTS = { 500, 150, 280, 500, 250, 550, 350, 400, 420, 200, 1200 };
    // 3 large cards centered in a single row
    private static final int SC_W = 172, SC_H = 150, SC_GAP = 18;
    private static final int SC_Y = 210;
    private static final int SC_X0 = WIDTH / 2 - (SC_W * 3 + SC_GAP * 2) / 2;
    private final Rectangle[] btnShopItems = {
            new Rectangle(SC_X0, SC_Y, SC_W, SC_H),
            new Rectangle(SC_X0 + SC_W + SC_GAP, SC_Y, SC_W, SC_H),
            new Rectangle(SC_X0 + (SC_W + SC_GAP) * 2, SC_Y, SC_W, SC_H),
    };
    private final boolean[] shopBought = new boolean[SHOP_OFFERED]; // per-slot
    private int[] shopOfferedItems = new int[] { 0, 1, 2 }; // filled in openShop()
    private final Rectangle btnShopContinue = new Rectangle(WIDTH / 2 - 110, 400, 220, 54);
    // ── Shop permanent upgrades (active until game over) ─────────────
    private boolean shopSpeedBoost = false;
    private boolean shopRapidFire = false;
    private boolean shopBulletTime = false;
    private boolean shopScoreRush = false;
    private boolean shopPhaseShift = false;
    private int ghostWalkTimer = 0;
    private int bulletTimeTimer = 0;
    private boolean hasSingularity = false;
    private boolean voidMagnetReady = false;
    private boolean voidMagnetActive = false;
    private int voidMagnetTimer = 0;
    private boolean shopEchoShot = false;
    private int echoShotCD = 0;
    private boolean hasDeathMark = false;
    private int powerUpDropCD = 0;
    private int sceneTransAlpha = 0;
    private final int[] s1x = new int[60], s1y = new int[60], s1b = new int[60];
    private final int[] s2x = new int[35], s2y = new int[35], s2b = new int[35];
    private final int[] s3x = new int[15], s3y = new int[15];
    private final int[] nebX = new int[8], nebY = new int[8], nebR = new int[8];
    private final Color[] nebCol = new Color[8];
    private final int[] astX = new int[10], astY = new int[10], astR = new int[10], astSpd = new int[10];
    private final int[] crtX = new int[12], crtY = new int[12], crtR = new int[12];
    private final int[] rockX = new int[8], rockY = new int[8], rockW = new int[8], rockH = new int[8];
    private final int[] bldX = new int[18], bldW = new int[18], bldH = new int[18];
    private final boolean[] bldLit = new boolean[18];
    private final int[] winX = new int[40], winY = new int[40];
    private final int[] islX = new int[6], islY = new int[6], islW = new int[6];
    private final int[] orbX = new int[10], orbY = new int[10], orbR = new int[10];
    private final Color[] orbCol = new Color[10];
    private final int[] lbX1 = new int[4], lbY1 = new int[4], lbX2 = new int[4], lbY2 = new int[4];
    private int lbTimer = 0;
    private int planet1X, planet1Y, planet1R;
    private Color planet1Col, planet1RingCol;
    private boolean planet1HasRing;
    private int planet2X, planet2Y, planet2R;
    private Color planet2Col;

    // Game state
    private int gameState = STATE_MENU;
    private int score = 0;
    private int wave = 1;
    private final int[] highScores = new int[3];
    private java.util.prefs.Preferences prefs;
    private final Rectangle btnContinueEndless = new Rectangle(WIDTH / 2 - 120, 370, 240, 54);
    private final Rectangle btnEndRun = new Rectangle(WIDTH / 2 - 120, 440, 240, 54);
    private int frameCount = 0;
    private boolean bossTransition = false;

    // Settings
    private int fireMode = FIRE_MOUSE;
    private boolean musicEnabled = true;
    private int musicVolPct = 75;
    private int difficulty = 1;
    private int selectedClass = CLASS_MACHINE_GUNNER;

    // Input
    private boolean mouseFireHeld = false;
    private boolean draggingSlider = false;
    private final boolean[] keys = new boolean[256];
    private int mouseX = WIDTH / 2;
    private int mouseY = 0;

    // Machine Gunner heat
    private int heat = 0;
    private boolean overheated = false;
    private int overheatTimer = 0;
    private boolean firingThisFrame = false;

    // Nova state
    private boolean novaCharging = false;
    private int novaChargeTimer = 0;
    private boolean novaLaserActive = false;
    private int novaLaserTimer = 0;
    private int novaCooldownTimer = 0;

    private int novaBeamX1, novaBeamY1, novaBeamX2, novaBeamY2;
    private double novaAccumulatedDmg = 0;
    private final ArrayList<NovaParticle> novaParticles = new ArrayList<>();

    // Phantom
    private int phantomDashCD = 0;
    private boolean phantomInvinc = false;
    private int phantomInvincT = 0;
    private double phantomDecoyX = -999, phantomDecoyY = -999;
    private int phantomDecoyT = 0;
    private int phantomAfterX, phantomAfterY, phantomAfterT = 0;
    private int phantomBurstCount = 0, phantomBurstCD = 0;

    // Sentinel

    // Viper
    private final ArrayList<Snake> snakes = new ArrayList<>();
    private int viperFireCD = 0;
    private int viperHitCount = 0;
    private final ArrayList<DamageIndicator> damageIndicators = new ArrayList<>();
    private int viperPoisonStacks = 0;
    private int viperPoisonTimer = 0;
    private int viperPoisonTickTimer = 0;

    // PowerUp timers
    private boolean hasShield = false;
    private boolean doubleShot = false; // ← add this line
    private int shieldTimer = 0;
    private int doubleShotTimer = 0;
    private int speedBoostTimer = 0;

    private String pickupMsg = "";
    private int pickupTimer = 0;

    // Game objects
    private Player player;
    private Boss boss;
    private final ArrayList<Bullet> playerBullets = new ArrayList<>();
    private final ArrayList<Bullet> enemyBullets = new ArrayList<>();
    private final ArrayList<PowerUp> powerUps = new ArrayList<>();
    private final Random rand = new Random();

    // MIDI
    private Sequencer sequencer;
    private Synthesizer synth;

    // Stars
    private final int[] starX = new int[120];
    private final int[] starY = new int[120];
    private final int[] starSz = new int[120];

    private Timer gameTimer;

    // Shake
    private int shakeTimer = 0;
    private int shakeIntensity = 0;

    // Sound
    private SourceDataLine soundLine;
    private int soundCooldown = 0;

    // Buttons
    private final Rectangle btnStart = new Rectangle(WIDTH / 2 - 110, 360, 220, 54);
    private final Rectangle btnSettings = new Rectangle(WIDTH / 2 - 110, 430, 220, 54);
    private final Rectangle btnQuit = new Rectangle(WIDTH / 2 - 110, 500, 220, 54);

    private final Rectangle btnFireMouse = new Rectangle(WIDTH / 2 - 120, 175, 240, 46);
    private final Rectangle btnFireSpace = new Rectangle(WIDTH / 2 - 120, 231, 240, 46);
    private final Rectangle btnMusicToggle = new Rectangle(WIDTH / 2 - 120, 355, 240, 46);
    private final Rectangle sliderTrack = new Rectangle(WIDTH / 2 - 110, 440, 220, 18);
    private final Rectangle btnSettBack = new Rectangle(WIDTH / 2 - 100, 700, 200, 50);
    // Pause menu buttons
    private final Rectangle btnPauseResume = new Rectangle(WIDTH / 2 - 110, 250, 220, 50);
    private final Rectangle btnPauseSettings = new Rectangle(WIDTH / 2 - 110, 315, 220, 50);
    private final Rectangle btnPauseQuit = new Rectangle(WIDTH / 2 - 110, 380, 220, 50);
    private boolean pauseInSettings = false;
    private boolean pauseConfirmQuit = false;
    private final Rectangle btnPauseConfirmYes = new Rectangle(WIDTH / 2 - 110, 315, 220, 50);
    private final Rectangle btnPauseConfirmNo = new Rectangle(WIDTH / 2 - 110, 380, 220, 50);

    private final Rectangle btnDiffEasy = new Rectangle(WIDTH / 2 - 130, 200, 260, 90);
    private final Rectangle btnDiffNormal = new Rectangle(WIDTH / 2 - 130, 300, 260, 90);
    private final Rectangle btnDiffHard = new Rectangle(WIDTH / 2 - 130, 400, 260, 90);
    private final Rectangle btnDiffBack = new Rectangle(WIDTH / 2 - 100, 510, 200, 46);

    private final Rectangle btnClassBack = new Rectangle(WIDTH / 2 - 100, 600, 200, 46);
    // 5 class cards in a single row (centered)
    // Row 1: 3 cards, Row 2: 2 cards centered — fills the screen properly
    private static final int CW = 130, CH = 240, CGAP = 14;
    private static final int CROW1Y = 160;
    private static final int ROW1X = (WIDTH - CW * 4 - CGAP * 3) / 2;
    private final Rectangle[] btnClass = {
            new Rectangle(ROW1X + 0 * (CW + CGAP), CROW1Y, CW, CH),
            new Rectangle(ROW1X + 1 * (CW + CGAP), CROW1Y, CW, CH),
            new Rectangle(ROW1X + 2 * (CW + CGAP), CROW1Y, CW, CH),
            new Rectangle(ROW1X + 3 * (CW + CGAP), CROW1Y, CW, CH),
    };

    // =================================================================
    public BulletHellGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(6, 6, 22));
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        // Clamp cursor inside game window
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                clampCursor(e);
            }

            @Override
            public void mouseDragged(java.awt.event.MouseEvent e) {
                clampCursor(e);
            }

            private void clampCursor(java.awt.event.MouseEvent e) {
                try {
                    java.awt.Point loc = getLocationOnScreen();
                    int ax = loc.x + e.getX(), ay = loc.y + e.getY();
                    int cx = Math.max(loc.x, Math.min(ax, loc.x + WIDTH - 1));
                    int cy = Math.max(loc.y, Math.min(ay, loc.y + HEIGHT - 1));
                    if (cx != ax || cy != ay)
                        new java.awt.Robot().mouseMove(cx, cy);
                } catch (Exception ex) {
                }
            }
        });
        Random sr = new Random(7777);
        for (int i = 0; i < starX.length; i++) {
            starX[i] = sr.nextInt(WIDTH);
            starY[i] = sr.nextInt(HEIGHT);
            starSz[i] = sr.nextInt(3) + 1;
        }
        initScenery();
        prefs = java.util.prefs.Preferences.userNodeForPackage(BulletHellGame.class);
        highScores[0] = prefs.getInt("hs_easy", 0);
        highScores[1] = prefs.getInt("hs_normal", 0);
        highScores[2] = prefs.getInt("hs_hard", 0);
        initMusic();
        initSound();
        gameTimer = new Timer(16, this);
        gameTimer.start();
    }

    // ── Sound ─────────────────────────────────────────────────────────
    private void initSound() {
        try {
            AudioFormat fmt = new AudioFormat(44100, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
            soundLine = (SourceDataLine) AudioSystem.getLine(info);
            soundLine.open(fmt, 4096);
            soundLine.start();
        } catch (Exception ex) {
            System.err.println("Sound init failed: " + ex.getMessage());
        }
    }

    private void playSpacegunSound() {
        if (soundLine == null)
            return;
        new Thread(() -> {
            try {
                int sr = 44100, samples = sr * 80 / 1000;
                byte[] buf = new byte[samples * 2];
                for (int i = 0; i < samples; i++) {
                    double t = (double) i / sr, freq = Math.max(80, 1400 - 13750 * t);
                    double s = (Math.sin(2 * Math.PI * freq * t) * 0.82 + (Math.random() * 2 - 1) * 0.18)
                            * Math.exp(-t * 38) * 22000;
                    short v = (short) Math.max(-32768, Math.min(32767, (int) s));
                    buf[i * 2] = (byte) (v & 0xFF);
                    buf[i * 2 + 1] = (byte) ((v >> 8) & 0xFF);
                }
                soundLine.write(buf, 0, buf.length);
            } catch (Exception ignored) {
            }
        }, "sfx-gun").start();
    }

    private void playNovaLaserSound() {
        if (soundLine == null)
            return;
        new Thread(() -> {
            try {
                int sr = 44100, samples = sr * 220 / 1000;
                byte[] buf = new byte[samples * 2];
                for (int i = 0; i < samples; i++) {
                    double t = (double) i / sr, freq = 200 + 3200 * t;
                    double wave = Math.sin(2 * Math.PI * freq * t) * 0.6
                            + Math.sin(2 * Math.PI * freq * 1.5 * t) * 0.25;
                    double crackle = (Math.random() * 2 - 1) * 0.3 * Math.exp(-t * 8);
                    double env = Math.min(1.0, t * 20) * Math.exp(-t * 3.5);
                    short v = (short) Math.max(-32768, Math.min(32767, (int) ((wave + crackle) * env * 28000)));
                    buf[i * 2] = (byte) (v & 0xFF);
                    buf[i * 2 + 1] = (byte) ((v >> 8) & 0xFF);
                }
                soundLine.write(buf, 0, buf.length);
            } catch (Exception ignored) {
            }
        }, "sfx-nova").start();
    }

    private void playBossLaserSound() {
        if (soundLine == null)
            return;
        new Thread(() -> {
            try {
                int sr = 44100, samples = sr * 350 / 1000;
                byte[] buf = new byte[samples * 2];
                for (int i = 0; i < samples; i++) {
                    double t = (double) i / sr;
                    double freq = 600 - 400 * t;
                    double wave = Math.sin(2 * Math.PI * freq * t) * 0.5
                            + Math.sin(2 * Math.PI * freq * 2.1 * t) * 0.3
                            + (Math.random() * 2 - 1) * 0.2;
                    double env = Math.min(1.0, t * 15) * Math.exp(-t * 2.5);
                    short v = (short) Math.max(-32768, Math.min(32767, (int) (wave * env * 30000)));
                    buf[i * 2] = (byte) (v & 0xFF);
                    buf[i * 2 + 1] = (byte) ((v >> 8) & 0xFF);
                }
                soundLine.write(buf, 0, buf.length);
            } catch (Exception ignored) {
            }
        }, "sfx-boss-laser").start();
    }

    // ── MIDI ──────────────────────────────────────────────────────────
    private void initMusic() {
        try {
            synth = MidiSystem.getSynthesizer();
            synth.open();
            sequencer = MidiSystem.getSequencer(false);
            sequencer.open();
            sequencer.getTransmitter().setReceiver(synth.getReceiver());
            sequencer.setSequence(buildSequence());
            sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
            applyMusicVolume();
            if (musicEnabled)
                sequencer.start();
        } catch (Exception ex) {
            System.err.println("MIDI init failed: " + ex.getMessage());
        }
    }

    private void applyMusicVolume() {
        if (synth == null)
            return;
        int v = (int) (musicVolPct / 100.0 * 127);
        for (MidiChannel ch : synth.getChannels())
            if (ch != null)
                ch.controlChange(7, v);
    }

    private Sequence buildSequence() throws InvalidMidiDataException {
        Sequence seq = new Sequence(Sequence.PPQ, 24);
        int us = 400_000;
        Track tt = seq.createTrack();
        MetaMessage mm = new MetaMessage();
        mm.setMessage(0x51, new byte[] { (byte) (us >> 16), (byte) (us >> 8), (byte) us }, 3);
        tt.add(new MidiEvent(mm, 0));
        Track lead = seq.createTrack();
        programChange(lead, 0, 80, 0);
        int[] mel = { 72, 74, 76, 79, 79, 76, 74, 72, 74, 76, 77, 81, 79, 77, 76, 74, 72, 76, 79, 84, 83, 81, 79, 77,
                76, 74, 72, 71, 72, 0, 0, 0 };
        int[] mDur = { 12, 12, 12, 18, 6, 12, 12, 12, 12, 12, 12, 18, 6, 12, 12, 12, 12, 12, 12, 18, 6, 12, 12, 12, 12,
                12, 12, 12, 24, 24, 24, 24 };
        for (int rep = 0; rep < 8; rep++) {
            long t = rep * 480L;
            for (int i = 0; i < mel.length; i++) {
                if (mel[i] > 0)
                    note(lead, 0, mel[i], 90, t, mDur[i] - 2);
                t += mDur[i];
            }
        }
        Track harm = seq.createTrack();
        programChange(harm, 1, 80, 0);
        int[] hm = { 60, 62, 64, 67, 67, 64, 62, 60, 62, 64, 65, 69, 67, 65, 64, 62, 60, 64, 67, 72, 71, 69, 67, 65, 64,
                62, 60, 59, 60, 0, 0, 0 };
        for (int rep = 0; rep < 8; rep++) {
            long t = rep * 480L;
            for (int i = 0; i < hm.length; i++) {
                if (hm[i] > 0)
                    note(harm, 1, hm[i], 58, t, mDur[i] - 2);
                t += mDur[i];
            }
        }
        Track bass = seq.createTrack();
        programChange(bass, 2, 38, 0);
        int[] roots = { 48, 50, 48, 53 }, fifths = { 55, 57, 55, 60 };
        for (int rep = 0; rep < 16; rep++) {
            int r = roots[rep % 4], f = fifths[rep % 4];
            long b = rep * 96L;
            note(bass, 2, r, 105, b, 20);
            note(bass, 2, r, 85, b + 24, 10);
            note(bass, 2, f, 100, b + 48, 20);
            note(bass, 2, r, 80, b + 72, 10);
        }
        Track drums = seq.createTrack();
        for (int bar = 0; bar < 24; bar++) {
            long b = bar * 96L;
            note(drums, 9, 36, 110, b, 6);
            note(drums, 9, 36, 95, b + 48, 6);
            note(drums, 9, 40, 100, b + 24, 5);
            note(drums, 9, 40, 100, b + 72, 5);
            for (int h = 0; h < 8; h++)
                note(drums, 9, 42, 55, b + h * 12, 4);
        }
        return seq;
    }

    private void note(Track t, int ch, int p, int v, long tick, int dur) throws InvalidMidiDataException {
        t.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, ch, p, v), tick));
        t.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, ch, p, 0), tick + dur));
    }

    private void programChange(Track t, int ch, int prog, long tick) throws InvalidMidiDataException {
        t.add(new MidiEvent(new ShortMessage(ShortMessage.PROGRAM_CHANGE, ch, prog, 0), tick));
    }

    // =================================================================
    // GAME LOOP
    // =================================================================
    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameState == STATE_PLAYING)
            update();
        repaint();
    }

    private void update() {
        frameCount++;
        if (shakeTimer > 0)
            shakeTimer--;
        if (soundCooldown > 0)
            soundCooldown--;

        boolean wantsFire = (fireMode == FIRE_SPACE && keys[KeyEvent.VK_SPACE])
                || (fireMode == FIRE_MOUSE && mouseFireHeld);
        int baseSpd = player.speed + (shopSpeedBoost ? 2 : 0);
        int spd = baseSpd;

        switch (selectedClass) {
            case CLASS_MACHINE_GUNNER:
                firingThisFrame = wantsFire && !overheated;
                if (firingThisFrame)
                    spd = Math.max(2, (int) (spd * 0.55));
                updateMachineGunner(wantsFire);
                break;
            case CLASS_NOVA:
                firingThisFrame = false;
                if (novaCharging || novaLaserActive)
                    spd = Math.max(2, (int) (spd * 0.60));
                updateNova(wantsFire);
                break;
            case CLASS_PHANTOM:
                firingThisFrame = wantsFire;
                updatePhantom(wantsFire);
                break;
            case CLASS_VIPER:
                firingThisFrame = wantsFire;
                updateViper(wantsFire);
                break;
        }

        if (keys[KeyEvent.VK_LEFT] || keys[KeyEvent.VK_A])
            player.x -= spd;
        if (keys[KeyEvent.VK_RIGHT] || keys[KeyEvent.VK_D])
            player.x += spd;
        if (keys[KeyEvent.VK_UP] || keys[KeyEvent.VK_W])
            player.y -= spd;
        if (keys[KeyEvent.VK_DOWN] || keys[KeyEvent.VK_S])
            player.y += spd;
        player.x = Math.max(0, Math.min(WIDTH - player.size, player.x));
        player.y = Math.max(0, Math.min(HEIGHT - player.size, player.y));

        if (shieldTimer > 0 && --shieldTimer == 0)
            hasShield = false;
        // Echo Shot: spawn delayed copy of last player bullet direction
        // echo shot removed
        if (ghostWalkTimer > 0 && --ghostWalkTimer == 0)
            shopPhaseShift = false;
        if (bulletTimeTimer > 0 && --bulletTimeTimer == 0)
            shopBulletTime = false;
        if (voidMagnetTimer > 0 && --voidMagnetTimer == 0)
            voidMagnetActive = false;
        if (echoShotCD > 0)
            echoShotCD--;
        if (doubleShotTimer > 0 && --doubleShotTimer == 0)
            doubleShot = false;
        if (speedBoostTimer > 0 && --speedBoostTimer == 0)
            player.speed = 5;
        if (pickupTimer > 0)
            pickupTimer--;

        if (frameCount % 360 == 0 && !bossTransition) {
            int type = (frameCount / 360) % PU_COUNT;
            boolean already = (type == PU_DOUBLE_SHOT && doubleShot) || (type == PU_SHIELD && hasShield)
                    || (type == PU_SPEED_BOOST && speedBoostTimer > 0);
            if (!already)
                powerUps.add(new PowerUp(rand.nextInt(WIDTH - 80) + 40, -60, type));
        }

        for (int i = powerUps.size() - 1; i >= 0; i--) {
            PowerUp p = powerUps.get(i);
            p.update();
            if (p.y > HEIGHT + 30) {
                powerUps.remove(i);
                continue;
            }
            int pcx = player.x + player.size / 2, pcy = player.y + player.size / 2;
            if (player.alive && p.getBounds().intersects(new Rectangle(pcx - 20, pcy - 20, 40, 40))) {
                applyPowerUp(p.type);
                powerUps.remove(i);
            }
        }

        if (!bossTransition) {
            boss.update(frameCount, player);
            spawnBossPattern();
        }

        // Nova laser vs boss — guard boss.alive to prevent double-kill
        if (selectedClass == CLASS_NOVA && novaLaserActive && !bossTransition && boss.alive) {
            if (laserHitsBoss(boss.getBounds())) {
                boss.hp -= 1.5;
                score += (shopScoreRush ? 12 : 6);
                novaAccumulatedDmg += 1.5;
                if (boss.hp <= 0 && boss.alive) {
                    boss.alive = false;
                    bossDefeated();
                }
            }
        } else if (!novaLaserActive && novaAccumulatedDmg > 0) {
            damageIndicators.add(new DamageIndicator(
                    boss.x + boss.width / 2,
                    boss.y + boss.height / 2,
                    "-" + (int) novaAccumulatedDmg, new Color(100, 180, 255)));
            novaAccumulatedDmg = 0;
        }

        // Player bullets vs boss
        for (int i = playerBullets.size() - 1; i >= 0; i--) {
            Bullet b = playerBullets.get(i);
            b.update();
            if (b.y < -40 || b.y > HEIGHT + 40 || b.x < -40 || b.x > WIDTH + 40) {
                playerBullets.remove(i);
                continue;
            }
            if (!bossTransition && boss.alive && boss.getBounds().intersects(b.getBounds())) {
                playerBullets.remove(i);
                boss.hp--;
                score += (shopScoreRush ? 20 : 10);
                damageIndicators.add(new DamageIndicator(
                        boss.x + rand.nextInt(boss.width),
                        boss.y + rand.nextInt(boss.height / 2),
                        "-1", new Color(255, 220, 80)));
                // Drop a field powerup: only 1-in-60 chance AND respect cooldown
                if (powerUpDropCD <= 0 && rand.nextInt(60) == 0) {
                    int dt = rand.nextInt(PU_COUNT);
                    boolean dup = (dt == PU_DOUBLE_SHOT && doubleShot) || (dt == PU_SHIELD && hasShield);
                    if (!dup) {
                        powerUps.add(new PowerUp(boss.x + boss.width / 2, boss.y + boss.height, dt));
                        powerUpDropCD = 180; // ~3 second gap between drops
                    }
                }
                if (powerUpDropCD > 0)
                    powerUpDropCD--;
                if (boss.hp <= 0 && boss.alive) {
                    boss.alive = false; // mark immediately before calling bossDefeated
                    bossDefeated();
                    break;
                }
            }
        }

        // Enemy bullets vs player
        for (int i = enemyBullets.size() - 1; i >= 0; i--) {
            Bullet b = enemyBullets.get(i);
            // Bullet Time: enemy bullets move at ~55% speed (skip update on odd frames)
            if (wave == 10 || !shopBulletTime || frameCount % 2 == 0)
                b.update();
            // Void Magnet: push bullets away from player
            if (voidMagnetActive) {
                double bx = b.x - (player.x + player.size / 2.0);
                double by = b.y - (player.y + player.size / 2.0);
                double dist = Math.sqrt(bx * bx + by * by);
                if (dist < 200 && dist > 0) {
                    b.dx += (bx / dist) * 1.2;
                    b.dy += (by / dist) * 1.2;
                }
            }
            if (b.y > HEIGHT + 10 || b.x < -10 || b.x > WIDTH + 10) {
                enemyBullets.remove(i);
                continue;
            }
            if (player.alive && !phantomInvinc && player.getHitbox().intersects(b.getBounds())) {
                if (hasDeathMark && boss.alive) {
                    hasDeathMark = false;
                    boss.hp = 0;
                    boss.alive = false;
                    enemyBullets.remove(i);
                    bossDefeated();
                    pickupMsg = "DEATH MARK TRIGGERED!";
                    pickupTimer = 90;
                    break;
                }
                if (shopPhaseShift) {
                    enemyBullets.remove(i);
                    continue;
                } // invincible
                if (hasShield) {
                    enemyBullets.remove(i);
                    hasShield = false;
                    shieldTimer = 0;
                    pickupMsg = "SHIELD BROKEN!";
                    pickupTimer = 90;
                    continue;
                }
                enemyBullets.remove(i);
                player.lives--;
                if (player.lives <= 0) {
                    player.alive = false;
                    spawnExplosion(player.x + player.size / 2, player.y + player.size / 2, Color.CYAN, 24);
                    if (score > highScores[difficulty]) {
                        highScores[difficulty] = score;
                        prefs.putInt(new String[] { "hs_easy", "hs_normal", "hs_hard" }[difficulty],
                                highScores[difficulty]);
                    }
                    Timer goTimer = new Timer(600, ev2 -> gameState = STATE_GAME_OVER);
                    goTimer.setRepeats(false);
                    goTimer.start();
                } else
                    enemyBullets.clear();
                break;
            }
        }

        // Boss laser vs player (APEX ONLY)
        if (!bossTransition && boss.alive && boss.laserActive && boss.isApex) {
            if (player.alive && !shopPhaseShift && boss.laserHitsPlayer(player.getHitbox())) {
                if (hasShield) {
                    hasShield = false;
                    shieldTimer = 0;
                    pickupMsg = "SHIELD BROKEN!";
                    pickupTimer = 90;
                } else {
                    player.lives--;
                    if (player.lives <= 0) {
                        player.alive = false;
                        spawnExplosion(player.x + player.size / 2, player.y + player.size / 2, Color.CYAN, 24);
                        if (score > highScores[difficulty]) {
                            highScores[difficulty] = score;
                            prefs.putInt(new String[] { "hs_easy", "hs_normal", "hs_hard" }[difficulty],
                                    highScores[difficulty]);
                        }
                        Timer goTimer = new Timer(600, ev2 -> gameState = STATE_GAME_OVER);
                        goTimer.setRepeats(false);
                        goTimer.start();
                    } else {
                        enemyBullets.clear();
                        boss.laserActive = false;
                    }
                }
            }
        }

        // Nova particles
        Iterator<NovaParticle> it = novaParticles.iterator();
        while (it.hasNext()) {
            if (!it.next().update())
                it.remove();
        }

        // Snakes
        for (int i = snakes.size() - 1; i >= 0; i--) {
            Snake s = snakes.get(i);
            s.update(boss);
            if (s.dead) {
                snakes.remove(i);
                continue;
            }
            if (!bossTransition && boss.alive && boss.getBounds().intersects(s.getBounds())) {
                boss.hp -= 2;
                score += (shopScoreRush ? 30 : 15);
                s.dead = true;
                damageIndicators.add(new DamageIndicator(
                        boss.x + rand.nextInt(boss.width),
                        boss.y + rand.nextInt(boss.height / 2),
                        "-2", new Color(0, 255, 100)));
                viperHitCount++;
                if (viperHitCount >= 5) {
                    viperHitCount = 0;
                    viperPoisonStacks = Math.min(viperPoisonStacks + 1, 3);
                    viperPoisonTimer = 360; // 6 seconds
                    viperPoisonTickTimer = 120; // first tick in 2s
                    pickupMsg = "POISON x" + viperPoisonStacks + "!";
                    pickupTimer = 80;
                }
                if (boss.hp <= 0 && boss.alive) {
                    boss.alive = false;
                    bossDefeated();
                    break;
                }
            }
        }

        // Sentinel orb bullet-reflect

        // Explosion particles
        Iterator<ExplosionParticle> ep = explosionParticles.iterator();
        while (ep.hasNext())
            if (!ep.next().update())
                ep.remove();
        Iterator<DamageIndicator> di = damageIndicators.iterator();
        while (di.hasNext())
            if (!di.next().update())
                di.remove();

        if (viperPoisonTimer > 0 && !bossTransition && boss.alive) {
            viperPoisonTimer--;
            viperPoisonTickTimer--;
            if (viperPoisonTickTimer <= 0) {
                viperPoisonTickTimer = 120; // tick every 2 seconds
                double dmg = boss.maxHp * 0.03 * viperPoisonStacks;
                boss.hp -= dmg;
                score += (int) (dmg / 2);
                pickupMsg = "POISON TICK -" + viperPoisonStacks * 3 + "%!";
                pickupTimer = 60;
                damageIndicators.add(new DamageIndicator(
                        boss.x + boss.width / 2 - 20,
                        boss.y + boss.height / 2,
                        "-" + (int) dmg + " ☠", new Color(240, 210, 255)));
                if (boss.hp <= 0 && boss.alive) {
                    boss.alive = false;
                    bossDefeated();
                }
            }
            if (viperPoisonTimer <= 0) {
                viperPoisonStacks = 0;
                viperPoisonTimer = 0;
            }
        }
        updateScenery();
    }

    // ── FIX: bossDefeated now safe against double-call ────────────────
    private void bossDefeated() {
        score += 500 + wave * 50;
        wave++;
        bossTransition = true;
        viperHitCount = 0;
        viperPoisonStacks = 0;
        viperPoisonTimer = 0;
        viperPoisonTickTimer = 0;
        novaLaserActive = false;
        playerBullets.clear();
        enemyBullets.clear();
        if (wave - 1 == 10) {
            // just beat wave 10 — ask player to continue or end
            if (score > highScores[difficulty]) {
                highScores[difficulty] = score;
                prefs.putInt(new String[] { "hs_easy", "hs_normal", "hs_hard" }[difficulty], highScores[difficulty]);
            }
            Timer choiceDelay = new Timer(1500, ev -> gameState = STATE_WAVE10_CHOICE);
            choiceDelay.setRepeats(false);
            choiceDelay.start();
        } else {
            Timer shopDelay = new Timer(1500, ev -> openShop());
            shopDelay.setRepeats(false);
            shopDelay.start();
        }
    }

    private void openShop() {
        // Randomly pick 3 distinct items from the full pool of 10
        java.util.List<Integer> pool = new java.util.ArrayList<>();
        for (int i = 0; i < SHOP_POOL_SIZE; i++)
            pool.add(i);
        java.util.Collections.shuffle(pool, rand);
        shopOfferedItems[0] = pool.get(0);
        shopOfferedItems[1] = pool.get(1);
        shopOfferedItems[2] = pool.get(2);
        for (int i = 0; i < SHOP_OFFERED; i++)
            shopBought[i] = false;
        gameState = STATE_SHOP;
    }

    private void buyShopItem(int slot) {
        if (slot < 0 || slot >= SHOP_OFFERED)
            return;
        if (shopBought[slot])
            return;
        int idx = shopOfferedItems[slot]; // real item type from the pool
        if (score < SHOP_COSTS[idx])
            return;
        score -= SHOP_COSTS[idx];
        shopBought[slot] = true;
        switch (idx) {
            case SHOP_EXTRA_LIFE:
                player.lives++;
                pickupMsg = "BLOOD PACT — +1 LIFE!";
                break;
            case SHOP_SPEED_BOOST:
                shopSpeedBoost = true;
                pickupMsg = "AETHER STRIDE — SPEED UP!";
                break;
            case SHOP_RAPID_FIRE:
                shopRapidFire = true;
                pickupMsg = "FRENZY CORE — RAPID FIRE!";
                break;
            case SHOP_BULLET_TIME:
                shopBulletTime = true;
                bulletTimeTimer = 600;
                pickupMsg = "CLOCK FRACTURE — SLOW BULLETS! (10s)";
                break;
            case SHOP_SCORE_RUSH:
                shopScoreRush = true;
                pickupMsg = "GOLD RUSH — 2x SCORE!";
                break;
            case SHOP_PHASE_SHIFT:
                shopPhaseShift = true;
                ghostWalkTimer = 300;
                pickupMsg = "GHOST WALK \n 5s INVINCIBILITY!";
                break;
            case SHOP_NUKE:
                hasSingularity = true;
                pickupMsg = "SINGULARITY READY! \n PRESS E TO USE!";
                break;
            case SHOP_VOID_MAGNET:
                voidMagnetReady = true;
                pickupMsg = "VOID MAGNET READY! [Q] TO USE";
                break;
            case SHOP_ECHO_SHOT:
                pickupMsg = "ECHO SHOT — GHOST BULLETS!";
                break;
            case SHOP_DEATH_MARK:
                hasDeathMark = true;
                pickupMsg = "DEATH MARK — ONE HIT KILLS BOSS!";
                break;
            case SHOP_REPAIR:
                int maxLives = new int[] { 5, 3, 2 }[difficulty];
                if (player.lives < maxLives) {
                    player.lives++;
                    pickupMsg = "NANO MEND — HP RESTORED!";
                } else {
                    score += SHOP_COSTS[idx];
                    shopBought[slot] = false;
                    pickupMsg = "Already at full HP!";
                }
                break;
        }
        pickupTimer = 90;
    }

    private void leaveShop() {
        int sceneIndex;
        if (wave >= 6 && wave <= 9) {
            sceneIndex = SCENE_JAPAN;
        } else if (wave == 10) {
            sceneIndex = SCENE_KITSUNE;
        } else {
            sceneIndex = (wave - 1) % SCENE_COUNT;
        }
        setScene(sceneIndex);
        boss = new Boss(WIDTH / 2 - 40, -80, wave);
        bossTransition = false;
        gameState = STATE_PLAYING;
    }

    // ── Machine Gunner ────────────────────────────────────────────────
    private void updateMachineGunner(boolean wantsFire) {
        if (overheated) {
            if (--overheatTimer <= 0) {
                overheated = false;
                heat = 0;
            }
        } else if (!wantsFire)
            heat = Math.max(0, heat - HEAT_COOL_RATE);

        int activeFireRate = (shopRapidFire ? FIRE_RATE / 2 : FIRE_RATE);
        if (!overheated && wantsFire && frameCount % Math.max(1, activeFireRate) == 0) {
            int pcx = player.x + player.size / 2, pcy = player.y + player.size / 2;
            double dx = mouseX - pcx, dy = mouseY - pcy, len = Math.sqrt(dx * dx + dy * dy), bs = 14;
            double bvx = 0, bvy = -bs;
            if (len > 1) {
                bvx = (dx / len) * bs;
                bvy = (dy / len) * bs;
            }
            if (doubleShot) {
                double px = -bvy / bs, py = bvx / bs;
                playerBullets.add(new Bullet(pcx + px * 8, pcy + py * 8, bvx, bvy, Color.CYAN, false));
                playerBullets.add(new Bullet(pcx - px * 8, pcy - py * 8, bvx, bvy, Color.CYAN, false));
                heat += HEAT_PER_SHOT * 2;
            } else {
                playerBullets.add(new Bullet(pcx, pcy, bvx, bvy, Color.CYAN, false));
                heat += HEAT_PER_SHOT;
            }
            if (soundCooldown == 0) {
                playSpacegunSound();
                soundCooldown = FIRE_RATE;
            }
            shakeTimer = Math.min(shakeTimer + 3, 6);
            shakeIntensity = 2;
            if (heat >= MAX_HEAT) {
                heat = MAX_HEAT;
                overheated = true;
                overheatTimer = OVERHEAT_FRAMES;
                shakeTimer = 18;
                shakeIntensity = 5;
                pickupMsg = "OVERHEATED!";
                pickupTimer = OVERHEAT_FRAMES;
            }
        }
    }

    // ── Nova ──────────────────────────────────────────────────────────
    private void updateNova(boolean wantsFire) {
        int pcx = player.x + player.size / 2, pcy = player.y + player.size / 2;
        if (novaCooldownTimer > 0)
            novaCooldownTimer--;
        if (novaLaserActive) {
            if (--novaLaserTimer <= 0) {
                novaLaserActive = false;
                novaCooldownTimer = NOVA_COOLDOWN_FRAMES;
            }
            return;
        }
        if (novaCooldownTimer > 0) {
            novaCharging = false;
            novaChargeTimer = 0;
            return;
        }
        if (wantsFire) {
            novaCharging = true;
            novaChargeTimer = Math.min(novaChargeTimer + 1, NOVA_CHARGE_FRAMES);
            double angle = frameCount * 0.35;
            double radius = 40 + 20 * (1.0 - (double) novaChargeTimer / NOVA_CHARGE_FRAMES);
            for (int i = 0; i < 2; i++) {
                double a = angle + Math.PI * i;
                double px = pcx + Math.cos(a) * radius + (rand.nextDouble() - 0.5) * 8;
                double py = pcy + Math.sin(a) * radius + (rand.nextDouble() - 0.5) * 8;
                double vx = (pcx - px) * 0.12 + (rand.nextDouble() - 0.5) * 1.5;
                double vy = (pcy - py) * 0.12 + (rand.nextDouble() - 0.5) * 1.5;
                float cf = (float) novaChargeTimer / NOVA_CHARGE_FRAMES;
                Color pc = new Color((int) (80 + 175 * cf), (int) (80 + 160 * cf), 255, 200);
                novaParticles.add(new NovaParticle(px, py, vx, vy, pc, 14 + rand.nextInt(10)));
            }
            if (novaChargeTimer >= NOVA_CHARGE_FRAMES)
                fireNovaLaser(pcx, pcy);
        } else {
            if (novaCharging) {
                novaChargeTimer -= 3;
                if (novaChargeTimer <= 0) {
                    novaChargeTimer = 0;
                    novaCharging = false;
                    novaParticles.clear();
                }
            }
        }
    }

    private void fireNovaLaser(int pcx, int pcy) {
        novaCharging = false;
        novaChargeTimer = 0;
        novaLaserActive = true;
        novaLaserTimer = NOVA_LASER_FRAMES;
        double dx = mouseX - pcx, dy = mouseY - pcy, len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1) {
            dx = 0;
            dy = -1;
            len = 1;
        }
        double nx = dx / len, ny = dy / len;
        novaBeamX1 = pcx;
        novaBeamY1 = pcy;
        novaBeamX2 = (int) (pcx + nx * 900);
        novaBeamY2 = (int) (pcy + ny * 900);
        shakeTimer = 22;
        shakeIntensity = 7;
        playNovaLaserSound();
        for (int i = 0; i < 30; i++) {
            double a = rand.nextDouble() * Math.PI * 2, sp = 1.5 + rand.nextDouble() * 4;
            novaParticles.add(new NovaParticle(pcx, pcy, Math.cos(a) * sp, Math.sin(a) * sp,
                    new Color(60, 160 + rand.nextInt(95), 255, 230), 20 + rand.nextInt(15)));
        }
    }

    private boolean laserHitsBoss(Rectangle box) {
        int hw = NOVA_LASER_WIDTH / 2;
        Rectangle fat = new Rectangle(box.x - hw, box.y - hw, box.width + hw * 2, box.height + hw * 2);
        return fat.intersectsLine(novaBeamX1, novaBeamY1, novaBeamX2, novaBeamY2);
    }

    // ── Phantom ───────────────────────────────────────────────────────
    private void updatePhantom(boolean wantsFire) {
        boolean dash = (keys[KeyEvent.VK_SHIFT] || keys[KeyEvent.VK_Z]) && phantomDashCD == 0;
        if (dash) {
            phantomAfterX = player.x;
            phantomAfterY = player.y;
            phantomAfterT = 20;
            phantomDecoyX = player.x + player.size / 2;
            phantomDecoyY = player.y + player.size / 2;
            phantomDecoyT = PHANTOM_DECOY_LIFE;
            double dx = mouseX - (player.x + player.size / 2), dy = mouseY - (player.y + player.size / 2);
            double len = Math.sqrt(dx * dx + dy * dy);
            if (len < 1) {
                dx = 0;
                dy = -120;
                len = 120;
            }
            double dist = 120;
            player.x = (int) Math.max(0, Math.min(WIDTH - player.size, player.x + dx / len * dist));
            player.y = (int) Math.max(0, Math.min(HEIGHT - player.size, player.y + dy / len * dist));
            phantomInvinc = true;
            phantomInvincT = 25;
            phantomDashCD = PHANTOM_DASH_CD;
            shakeTimer = 8;
            shakeIntensity = 3;
            for (Bullet b : enemyBullets) {
                double bDx = phantomDecoyX - b.x, bDy = phantomDecoyY - b.y;
                double bL = Math.sqrt(bDx * bDx + bDy * bDy);
                if (bL < 80 && bL > 1) {
                    b.dx = bDx / bL * Math.sqrt(b.dx * b.dx + b.dy * b.dy);
                    b.dy = bDy / bL * Math.sqrt(b.dx * b.dx + b.dy * b.dy);
                }
            }
        }
        if (phantomBurstCount < 3 && phantomBurstCD == 0 && wantsFire) {
            int pcx = player.x + player.size / 2, pcy = player.y + player.size / 2;
            double dx = mouseX - pcx, dy = mouseY - pcy, len = Math.sqrt(dx * dx + dy * dy), bs = 13;
            double bvx = 0, bvy = -bs;
            if (len > 1) {
                bvx = (dx / len) * bs;
                bvy = (dy / len) * bs;
            }
            double px = -bvy / bs, py = bvx / bs;
            playerBullets.add(new KnifeBullet(pcx + px * 10, pcy + py * 10, bvx, bvy, true));
            playerBullets.add(new KnifeBullet(pcx - px * 10, pcy - py * 10, bvx, bvy, false));
            if (soundCooldown == 0) {
                playSpacegunSound();
                soundCooldown = 6;
            }
            shakeTimer = Math.min(shakeTimer + 2, 5);
            shakeIntensity = 2;
            phantomBurstCount++;
            phantomBurstCD = 8; // gap between knives in burst
            if (phantomBurstCount >= 3) {
                phantomBurstCD = 45; // 0.75 sec cooldown after full burst
            }
        }
        if (phantomBurstCD > 0)
            phantomBurstCD--;
        if (phantomBurstCD == 0 && phantomBurstCount >= 3)
            phantomBurstCount = 0;
    }

    // ── Viper ─────────────────────────────────────────────────────────
    private void updateViper(boolean wantsFire) {
        int rate = Math.max(10, VIPER_FIRE_RATE);
        if (wantsFire && viperFireCD == 0 && snakes.size() < VIPER_MAX_SNAKES) {
            int pcx = player.x + player.size / 2, pcy = player.y + player.size / 2;
            double dx = mouseX - pcx, dy = mouseY - pcy, len = Math.sqrt(dx * dx + dy * dy);
            double bvx = 0, bvy = -7;
            if (len > 1) {
                bvx = (dx / len) * 7;
                bvy = (dy / len) * 7;
            }
            snakes.add(new Snake(pcx, pcy, bvx, bvy));
            if (doubleShot)
                snakes.add(new Snake(pcx, pcy, bvx * 0.85, bvy * 0.85));
            viperFireCD = rate;
            if (soundCooldown == 0) {
                playSpacegunSound();
                soundCooldown = rate;
            }
        }
    }

    // ── Explosion helper ──────────────────────────────────────────────
    private void spawnExplosion(int cx, int cy, Color c, int count) {
        for (int i = 0; i < count; i++) {
            double a = rand.nextDouble() * Math.PI * 2, sp = 2 + rand.nextDouble() * 6;
            explosionParticles
                    .add(new ExplosionParticle(cx, cy, Math.cos(a) * sp, Math.sin(a) * sp, c, 30 + rand.nextInt(20)));
        }
        shakeTimer = 16;
        shakeIntensity = 6;
    }

    // ── Boss patterns ─────────────────────────────────────────────────
    private void spawnBossPattern() {
        if (!boss.alive)
            return;
        int cx = boss.x + boss.width / 2, cy = boss.y + boss.height / 2;
        double dm = new double[] { 0.65, 0.9, 1.25 }[difficulty];
        double spd = 2.2 * dm;

        // Wave 1-2: simple ring
        if (wave <= 2) {
            if (frameCount % 120 == 0) {
                int cnt = 8;
                for (int i = 0; i < cnt; i++) {
                    double a = 2 * Math.PI * i / cnt;
                    enemyBullets.add(new Bullet(cx, cy, Math.cos(a) * spd, Math.sin(a) * spd, Color.RED, true));
                }
            }
        }
        // Wave 3-4: alternating double rings (offset by half)
        else if (wave <= 4) {
            if (frameCount % 90 == 0) {
                int cnt = 10;
                double offset = (frameCount / 90 % 2) * (Math.PI / cnt);
                for (int i = 0; i < cnt; i++) {
                    double a = 2 * Math.PI * i / cnt + offset;
                    enemyBullets.add(new Bullet(cx, cy, Math.cos(a) * spd, Math.sin(a) * spd, Color.RED, true));
                }
            }
        }
        // Wave 5-6: spinning cross + aimed shot
        else if (wave <= 6) {
            if (frameCount % 70 == 0) {
                for (int i = 0; i < 4; i++) {
                    double a = Math.PI / 2 * i + Math.toRadians(frameCount * 1.5);
                    enemyBullets.add(new Bullet(cx, cy, Math.cos(a) * spd, Math.sin(a) * spd, Color.RED, true));
                }
            }
            if (frameCount % 110 == 0) {
                double dx = player.x - cx, dy2 = player.y - cy, len = Math.sqrt(dx * dx + dy2 * dy2);
                if (len > 0)
                    enemyBullets.add(new Bullet(cx, cy, dx / len * spd, dy2 / len * spd, Color.RED, true));
            }
        }
        // Wave 7-8: spiral pair + aimed burst (3 spread shots)
        else if (wave <= 8) {
            if (frameCount % 14 == 0) {
                double a = Math.toRadians(frameCount * 4);
                enemyBullets.add(new Bullet(cx, cy, Math.cos(a) * spd, Math.sin(a) * spd, Color.RED, true));
                enemyBullets.add(new Bullet(cx, cy, -Math.cos(a) * spd, -Math.sin(a) * spd, Color.RED, true));
            }
            if (frameCount % 100 == 0) {
                double dx = player.x - cx, dy2 = player.y - cy, len = Math.sqrt(dx * dx + dy2 * dy2);
                if (len > 0) {
                    double base = Math.atan2(dy2, dx);
                    for (int s = -1; s <= 1; s++)
                        enemyBullets.add(new Bullet(cx, cy, Math.cos(base + s * 0.22) * spd,
                                Math.sin(base + s * 0.22) * spd, Color.RED, true));
                }
            }
        } else if (wave <= 9) {
            if (frameCount % 55 == 0) {
                int ring = (frameCount / 55) % 3;
                int cnt = 7 + ring * 1;
                double rot = Math.toRadians(ring * 20 + frameCount * 0.5);
                for (int i = 0; i < cnt; i++) {
                    double a = 2 * Math.PI * i / cnt + rot;
                    enemyBullets.add(new Bullet(cx, cy, Math.cos(a) * spd, Math.sin(a) * spd, Color.RED, true));
                }
            }
            if (frameCount % 120 == 0) {
                double dx = player.x - cx, dy2 = player.y - cy, len = Math.sqrt(dx * dx + dy2 * dy2);
                if (len > 0) {
                    double base = Math.atan2(dy2, dx);
                    for (int s = -1; s <= 1; s++)
                        enemyBullets.add(new Bullet(cx, cy, Math.cos(base + s * 0.18) * spd,
                                Math.sin(base + s * 0.18) * spd, Color.RED, true));
                }
            }
        }
        // Wave 10: KITSUNE — spirit lance mechanic
        // Fires fast red lances that DECELERATE, leaving visible gaps to dodge
        // Wave 10: KITSUNE — spirit lance mechanic
        else if (wave == 10) {
            // Fox fire ring — slow lingering orbs
            if (frameCount % 100 == 0) {
                int cnt = 8;
                double rot = Math.toRadians(frameCount * 1.2);
                for (int i = 0; i < cnt; i++) {
                    double a = 2 * Math.PI * i / cnt + rot;
                    enemyBullets.add(new KitsuneFoxFireBullet(cx, cy,
                            Math.cos(a) * 1.2, Math.sin(a) * 1.2));
                }
            }
            // Spirit lances — fast aimed shots that decelerate
            if (frameCount % 55 == 0) {
                double dx = player.x - cx, dy2 = player.y - cy;
                double len = Math.sqrt(dx * dx + dy2 * dy2);
                if (len > 0) {
                    double base = Math.atan2(dy2, dx);
                    int lanceCount = 5;
                    double gapAngle = 0.30;
                    for (int i = 0; i < lanceCount; i++) {
                        double offset = (i - lanceCount / 2) * gapAngle;
                        if (i % 2 == 0) {
                            enemyBullets.add(new KitsuneLanceBullet(cx, cy,
                                    Math.cos(base + offset), Math.sin(base + offset)));
                        }
                    }
                }
            }
            // Tail sweep — light red bullets
            if (frameCount % 140 == 0) {
                for (int tail = 0; tail < 3; tail++) {
                    double sweepBase = Math.toRadians(30 + tail * 60);
                    for (int s = 0; s < 6; s++) {
                        double a = sweepBase + Math.toRadians(s * 8);
                        enemyBullets.add(new KitsuneLanceBullet(cx, cy,
                                Math.cos(a), Math.sin(a)));
                    }
                }
            }
        }
        // Wave 11+: dense symmetric mandala + rotating cross + aimed 5-way spread
        else {
            if (frameCount % 22 == 0) {
                double rot = Math.toRadians(frameCount * 2);
                int cnt = 12;
                for (int i = 0; i < cnt; i++) {
                    double a = 2 * Math.PI * i / cnt + rot;
                    enemyBullets.add(
                            new Bullet(cx, cy, Math.cos(a) * spd, Math.sin(a) * spd, new Color(255, 80, 80), true));
                }
            }
            if (frameCount % 55 == 0) {
                for (int i = 0; i < 6; i++) {
                    double a = Math.PI / 3 * i + Math.toRadians(frameCount * 1.2);
                    enemyBullets.add(new Bullet(cx, cy, Math.cos(a) * spd, Math.sin(a) * spd, Color.CYAN, true));
                }
            }
            if (frameCount % 90 == 0) {
                double dx = player.x - cx, dy2 = player.y - cy, len = Math.sqrt(dx * dx + dy2 * dy2);
                if (len > 0) {
                    double base = Math.atan2(dy2, dx);
                    for (int s = -2; s <= 2; s++)
                        enemyBullets.add(new Bullet(cx, cy, Math.cos(base + s * 0.18) * spd,
                                Math.sin(base + s * 0.18) * spd, Color.ORANGE, true));
                }
            }
        }

        if (boss.isApex) {
            if (frameCount % 80 == 0) {
                int cnt = (int) Math.max(8, Math.min(10 + wave, 18) * 0.7);
                for (int i = 0; i < cnt; i++) {
                    double a = 2 * Math.PI * i / cnt + Math.toRadians(frameCount * 3);
                    double s = 1.6 * dm;
                    enemyBullets
                            .add(new Bullet(cx, cy, Math.cos(a) * s, Math.sin(a) * s, new Color(255, 100, 200), true));
                }
            }
            if (frameCount % 30 == 0) {
                double dx = player.x - cx, dy2 = player.y - cy, len = Math.sqrt(dx * dx + dy2 * dy2);
                if (len > 0) {
                    double s = 2.2 * dm;
                    enemyBullets.add(new Bullet(cx, cy, dx / len * s, dy2 / len * s, new Color(255, 60, 180), true));
                }
            }
        }
    }

    private void applyPowerUp(int type) {
        switch (type) {
            case PU_DOUBLE_SHOT:
                doubleShot = true;
                doubleShotTimer = 600;
                pickupMsg = "DOUBLE SHOT!";
                break;
            case PU_SHIELD:
                hasShield = true;
                shieldTimer = 600;
                pickupMsg = "SHIELD ON!";
                break;
            case PU_SPEED_BOOST:
                speedBoostTimer = 300;
                player.speed = Math.min(player.speed + 3, 10);
                pickupMsg = "SPEED BOOST!";
                break;
            case PU_SCORE_BURST:
                score += 500;
                pickupMsg = "+500 SCORE BURST!";
                break;
            case PU_HEAL:
                int maxLives = new int[] { 5, 3, 2 }[difficulty];
                if (player.lives < maxLives) {
                    player.lives++;
                    pickupMsg = "HEAL +1 LIFE!";
                } else {
                    pickupMsg = "ALREADY FULL HP!";
                }
                break;
        }
        pickupTimer = 120;
    }

    // ── Scenery init & update ─────────────────────────────────────────
    private void initScenery() {
        Random sr = new Random(9999);
        for (int i = 0; i < s1x.length; i++) {
            s1x[i] = sr.nextInt(WIDTH);
            s1y[i] = sr.nextInt(HEIGHT);
            s1b[i] = 80 + sr.nextInt(80);
        }
        for (int i = 0; i < s2x.length; i++) {
            s2x[i] = sr.nextInt(WIDTH);
            s2y[i] = sr.nextInt(HEIGHT);
            s2b[i] = 140 + sr.nextInt(115);
        }
        for (int i = 0; i < s3x.length; i++) {
            s3x[i] = sr.nextInt(WIDTH);
            s3y[i] = sr.nextInt(HEIGHT);
        }
        Color[] nc = { new Color(50, 0, 100, 20), new Color(0, 40, 100, 18), new Color(80, 0, 50, 16),
                new Color(0, 60, 100, 18), new Color(100, 20, 0, 14), new Color(0, 80, 60, 16),
                new Color(60, 0, 120, 20), new Color(100, 40, 0, 14) };
        for (int i = 0; i < nebX.length; i++) {
            nebX[i] = 40 + sr.nextInt(WIDTH - 80);
            nebY[i] = sr.nextInt(HEIGHT);
            nebR[i] = 70 + sr.nextInt(120);
            nebCol[i] = nc[i];
        }
        for (int i = 0; i < astX.length; i++) {
            astX[i] = 10 + sr.nextInt(WIDTH - 40);
            astY[i] = sr.nextInt(HEIGHT);
            astR[i] = 5 + sr.nextInt(18);
            astSpd[i] = 1 + sr.nextInt(3);
        }
        for (int i = 0; i < crtX.length; i++) {
            crtX[i] = sr.nextInt(WIDTH);
            crtY[i] = sr.nextInt(HEIGHT);
            crtR[i] = 15 + sr.nextInt(40);
        }
        for (int i = 0; i < rockX.length; i++) {
            rockX[i] = sr.nextInt(WIDTH);
            rockY[i] = sr.nextInt(HEIGHT);
            rockW[i] = 20 + sr.nextInt(50);
            rockH[i] = 10 + sr.nextInt(25);
        }
        int bx = 0;
        for (int i = 0; i < bldX.length; i++) {
            bldX[i] = bx;
            bldW[i] = 18 + sr.nextInt(30);
            bldH[i] = 60 + sr.nextInt(200);
            bldLit[i] = sr.nextBoolean();
            bx += bldW[i] + sr.nextInt(8);
        }
        for (int i = 0; i < winX.length; i++) {
            winX[i] = sr.nextInt(WIDTH);
            winY[i] = sr.nextInt(HEIGHT);
        }
        for (int i = 0; i < islX.length; i++) {
            islX[i] = sr.nextInt(WIDTH - 80);
            islY[i] = 100 + sr.nextInt(HEIGHT / 2);
            islW[i] = 60 + sr.nextInt(120);
        }
        Color[] oc = { new Color(0, 255, 200, 180), new Color(180, 0, 255, 160), new Color(255, 200, 0, 150),
                new Color(0, 200, 255, 170), new Color(255, 100, 0, 160), new Color(100, 255, 0, 150),
                new Color(200, 0, 255, 170), new Color(0, 255, 120, 160), new Color(255, 0, 180, 150),
                new Color(0, 180, 255, 170) };
        for (int i = 0; i < orbX.length; i++) {
            orbX[i] = sr.nextInt(WIDTH);
            orbY[i] = sr.nextInt(HEIGHT * 2 / 3);
            orbR[i] = 4 + sr.nextInt(12);
            orbCol[i] = oc[i];
        }
        for (int i = 0; i < lbX1.length; i++) {
            lbX1[i] = sr.nextInt(WIDTH);
            lbY1[i] = 0;
            lbX2[i] = lbX1[i] + (sr.nextInt(120) - 60);
            lbY2[i] = 150 + sr.nextInt(300);
        }
        planet1X = 60 + sr.nextInt(120);
        planet1Y = 80 + sr.nextInt(120);
        planet1R = 30 + sr.nextInt(50);
        planet1Col = new Color(180 + sr.nextInt(60), 120 + sr.nextInt(80), sr.nextInt(60));
        planet1HasRing = sr.nextBoolean();
        planet1RingCol = new Color(200, 180, 100, 80);
        planet2X = WIDTH - 80 - sr.nextInt(100);
        planet2Y = 60 + sr.nextInt(100);
        planet2R = 15 + sr.nextInt(30);
        planet2Col = new Color(sr.nextInt(80), 100 + sr.nextInt(100), 180 + sr.nextInt(60));
        currentScene = SCENE_SPACE;
        sceneTransAlpha = 0;
    }

    private void setScene(int scene) {
        currentScene = scene;
        sceneTransAlpha = 255;
    }

    private void updateScenery() {
        if (phantomDecoyT > 0)
            phantomDecoyT--;
        if (phantomAfterT > 0)
            phantomAfterT--;
        if (phantomInvincT > 0) {
            if (--phantomInvincT <= 0)
                phantomInvinc = false;
        }
        if (phantomDashCD > 0)
            phantomDashCD--;
        if (viperFireCD > 0)
            viperFireCD--;
        if (sceneTransAlpha > 0)
            sceneTransAlpha = Math.max(0, sceneTransAlpha - 4);
        if (lbTimer > 0)
            lbTimer--;
        if (frameCount % 90 == 0 && currentScene == SCENE_ALIEN) {
            Color[] oc = { new Color(0, 255, 200, 180), new Color(180, 0, 255, 160), new Color(255, 200, 0, 150),
                    new Color(0, 200, 255, 170), new Color(255, 100, 0, 160), new Color(100, 255, 0, 150),
                    new Color(200, 0, 255, 170), new Color(0, 255, 120, 160), new Color(255, 0, 180, 150),
                    new Color(0, 180, 255, 170) };
            for (int i = 0; i < orbCol.length; i++)
                orbCol[i] = oc[(i + frameCount / 90) % oc.length];
        }
        if (currentScene == SCENE_NEBULA && frameCount % 80 == 0 && rand.nextInt(3) == 0)
            lbTimer = 8;
    }

    // =================================================================
    // RENDERING
    // =================================================================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int shakeX = 0, shakeY = 0;
        if (shakeTimer > 0 && gameState == STATE_PLAYING) {
            shakeX = rand.nextInt(shakeIntensity * 2 + 1) - shakeIntensity;
            shakeY = rand.nextInt(shakeIntensity * 2 + 1) - shakeIntensity;
        }
        g2.translate(shakeX, shakeY);
        g2.setColor(new Color(6, 6, 22));
        g2.fillRect(-10, -10, WIDTH + 20, HEIGHT + 20);
        drawStarfield(g2);
        switch (gameState) {
            case STATE_MENU:
                drawMenu(g2);
                break;
            case STATE_SETTINGS:
                drawSettings(g2);
                break;
            case STATE_DIFF_SEL:
                drawDiffSelect(g2);
                break;
            case STATE_CLASS_SEL:
                drawClassSelect(g2);
                break;
            case STATE_PLAYING:
                drawGame(g2);
                break;
            case STATE_SHOP:
                drawShop(g2);
                break;
            case STATE_PAUSE:
                drawGame(g2);
                drawPause(g2);
                break;
            case STATE_GAME_OVER:
                drawGame(g2);
                drawGameOver(g2);
                break;
            case STATE_WAVE10_CHOICE:
                drawGame(g2);
                drawWave10Choice(g2);
                break;
        }
        g2.translate(-shakeX, -shakeY);
    }

    private void drawStarfield(Graphics2D g2) {
        if (gameState == STATE_PLAYING || gameState == STATE_GAME_OVER) {
            switch (currentScene) {
                case SCENE_SPACE:
                    drawSceneSpace(g2);
                    break;
                case SCENE_MARS:
                    drawSceneMars(g2);
                    break;
                case SCENE_EARTH:
                    drawSceneEarth(g2);
                    break;
                case SCENE_ALIEN:
                    drawSceneAlien(g2);
                    break;
                case SCENE_ASTEROID:
                    drawSceneAsteroid(g2);
                    break;
                case SCENE_NEBULA:
                    drawSceneNebula(g2);
                    break;
                case SCENE_VOID:
                    drawSceneVoid(g2);
                    break;
                case SCENE_STORM:
                    drawSceneStorm(g2);
                    break;
                case SCENE_ICE:
                    drawSceneIce(g2);
                    break;
                case SCENE_JUNGLE:
                    drawSceneJungle(g2);
                    break;
                case SCENE_JAPAN:
                    drawSceneJapan(g2);
                    break;
                case SCENE_KITSUNE:
                    drawSceneKitsune(g2);
                    break;
            }
            if (sceneTransAlpha > 0) {
                g2.setColor(new Color(6, 6, 22, Math.min(255, sceneTransAlpha)));
                g2.fillRect(0, 0, WIDTH, HEIGHT);
            }
        } else {
            for (int i = 0; i < starX.length; i++) {
                float f = (float) (0.6 + 0.4 * Math.sin(frameCount * 0.04 + i));
                int br = (int) (120 + 100 * f);
                g2.setColor(new Color(br, br, Math.min(255, br + 30)));
                g2.fillRect(starX[i], starY[i], starSz[i], starSz[i]);
            }
        }
    }

    private void drawSkyGradient(Graphics2D g2, Color top, Color bottom) {
        for (int y = 0; y < HEIGHT; y++) {
            float t = (float) y / HEIGHT;
            int r = (int) (top.getRed() * (1 - t) + bottom.getRed() * t);
            int g3 = (int) (top.getGreen() * (1 - t) + bottom.getGreen() * t);
            int b2 = (int) (top.getBlue() * (1 - t) + bottom.getBlue() * t);
            g2.setColor(new Color(Math.min(255, r), Math.min(255, g3), Math.min(255, b2)));
            g2.fillRect(0, y, WIDTH, 1);
        }
    }

    private void drawStars(Graphics2D g2, int rTint, int gTint, int bTint, boolean fast) {
        for (int i = 0; i < s1x.length; i++) {
            int sy = (s1y[i] + frameCount / 4) % HEIGHT;
            int b = s1b[i];
            g2.setColor(new Color(Math.min(255, b + rTint), Math.min(255, b + gTint), Math.min(255, b + bTint)));
            g2.fillRect(s1x[i], sy, 1, 1);
        }
        for (int i = 0; i < s2x.length; i++) {
            int sy = (s2y[i] + frameCount / 2) % HEIGHT;
            int b = s2b[i];
            g2.setColor(
                    new Color(Math.min(255, b + rTint / 2), Math.min(255, b + gTint / 2), Math.min(255, b + bTint)));
            int sz = i % 4 == 0 ? 2 : 1;
            g2.fillRect(s2x[i], sy, sz, sz);
        }
        if (fast)
            for (int i = 0; i < s3x.length; i++) {
                int sy = (s3y[i] + frameCount) % HEIGHT;
                float twinkle = (float) (0.6 + 0.4 * Math.sin(frameCount * 0.12 + i * 0.8));
                int br2 = (int) (180 * twinkle) + 60;
                g2.setColor(
                        new Color(Math.min(255, br2 + rTint), Math.min(255, br2 + gTint), Math.min(255, br2 + bTint)));
                g2.fillRect(s3x[i], sy, 2, 2);
            }
    }

    private void drawPlanet(Graphics2D g2, int px, int py, int pr, Color base,
            boolean hasRing, Color ringCol, Color atmosphereCol) {
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillOval(px - pr + 6, py - pr + 6, pr * 2, pr * 2);
        g2.setColor(base);
        g2.fillOval(px - pr, py - pr, pr * 2, pr * 2);
        int bands = 4 + pr / 15;
        for (int b2 = 0; b2 < bands; b2++) {
            float bt = (float) b2 / bands;
            int by2 = (int) (py - pr + bt * pr * 2);
            int bh = Math.max(2, pr * 2 / bands);
            int br3 = Math.max(0, base.getRed() - 20 + ((b2 % 2) * 30));
            int bg = Math.max(0, base.getGreen() - 15 + ((b2 % 2) * 20));
            int bb = Math.max(0, base.getBlue() - 10 + ((b2 % 2) * 15));
            g2.setClip(new java.awt.geom.Ellipse2D.Float(px - pr, py - pr, pr * 2, pr * 2));
            g2.setColor(new Color(Math.min(255, br3), Math.min(255, bg), Math.min(255, bb), 60));
            g2.fillRect(px - pr, by2, pr * 2, bh);
            g2.setClip(null);
        }
        g2.setClip(new java.awt.geom.Ellipse2D.Float(px - pr, py - pr, pr * 2, pr * 2));
        g2.setColor(new Color(255, 255, 255, 20));
        g2.fillOval(px - pr / 2, py - pr, pr, pr / 3);
        g2.setClip(null);
        for (int rim = 1; rim <= 4; rim++) {
            int alpha = Math.max(0, 55 - rim * 12);
            g2.setColor(new Color(atmosphereCol.getRed(), atmosphereCol.getGreen(), atmosphereCol.getBlue(), alpha));
            g2.setStroke(new BasicStroke(rim * 2.5f));
            g2.drawOval(px - pr - rim, py - pr - rim, pr * 2 + rim * 2, pr * 2 + rim * 2);
        }
        g2.setStroke(new BasicStroke(1));
        g2.setColor(new Color(0, 0, 0, 100));
        g2.setClip(new java.awt.geom.Ellipse2D.Float(px - pr, py - pr, pr * 2, pr * 2));
        g2.fillOval(px, py - pr, pr, pr * 2);
        g2.setClip(null);
        g2.setColor(new Color(255, 255, 255, 35));
        g2.fillOval(px - pr / 2, py - pr + pr / 6, pr / 3, pr / 5);
        if (hasRing) {
            g2.setClip(null);
            for (int rl = 0; rl < 5; rl++) {
                int rw = (int) (pr * (2.0 + rl * 0.4)), rh = (int) (pr * 0.35);
                int alpha = Math.max(0, 90 - rl * 18);
                g2.setColor(new Color(ringCol.getRed(), ringCol.getGreen(), ringCol.getBlue(), alpha));
                g2.setStroke(new BasicStroke(3.5f - rl * 0.5f));
                g2.drawOval(px - rw / 2, py - rh / 2, rw, rh);
            }
            g2.setColor(base);
            g2.setClip(new java.awt.geom.Ellipse2D.Float(px - pr, py, pr * 2, pr));
            g2.fillOval(px - pr, py, pr * 2, pr);
            g2.setClip(null);
            g2.setStroke(new BasicStroke(1));
        }
    }

    private void drawSceneSpace(Graphics2D g2) {
        drawSkyGradient(g2, new Color(2, 2, 12), new Color(4, 4, 18));
        g2.setColor(new Color(200, 180, 255, 8));
        g2.fillOval(-60, HEIGHT / 3 - 40, 320, 80);
        g2.setColor(new Color(220, 200, 255, 5));
        g2.fillOval(-40, HEIGHT / 3 - 20, 280, 40);
        for (int i = 0; i < nebX.length; i++) {
            int ry = (nebY[i] + frameCount / 5) % (HEIGHT + 280) - 140;
            Color nc2 = nebCol[i];
            g2.setColor(new Color(nc2.getRed(), nc2.getGreen(), nc2.getBlue(), 8));
            g2.fillOval(nebX[i] - nebR[i] - 30, ry - nebR[i] / 2 - 15, (nebR[i] + 30) * 2, (int) (nebR[i] * 1.2));
            g2.setColor(new Color(nc2.getRed(), nc2.getGreen(), nc2.getBlue(), 14));
            g2.fillOval(nebX[i] - nebR[i], ry - nebR[i] / 2, nebR[i] * 2, nebR[i]);
            g2.setColor(new Color(Math.min(255, nc2.getRed() + 30), Math.min(255, nc2.getGreen() + 20),
                    Math.min(255, nc2.getBlue() + 30), 20));
            g2.fillOval(nebX[i] - nebR[i] / 2, ry - nebR[i] / 4, nebR[i], nebR[i] / 2);
        }
        drawStars(g2, 0, 0, 30, true);
        rand.setSeed(77777);
        for (int i = 0; i < 50; i++) {
            int sx2 = 80 + rand.nextInt(160), sy2 = (50 + rand.nextInt(200) + frameCount / 3) % HEIGHT;
            int br2 = 160 + rand.nextInt(95);
            g2.setColor(new Color(br2, br2, Math.min(255, br2 + 40), br2));
            g2.fillRect(sx2, sy2, 1, 1);
        }
        int ssPhase = frameCount % 300;
        if (ssPhase < 40) {
            float ssp = (float) ssPhase / 40;
            int ssx = (int) (ssp * 700 - 50), ssy = (int) (ssp * 300 + 20);
            g2.setColor(new Color(255, 255, 255, (int) (200 * (1 - ssp))));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawLine(ssx, ssy, ssx - (int) (40 * (1 - ssp)), ssy - (int) (15 * (1 - ssp)));
            g2.setStroke(new BasicStroke(1));
        }
        drawPlanet(g2, planet1X, planet1Y, planet1R, planet1Col, planet1HasRing, planet1RingCol,
                new Color(180, 200, 255));
        drawPlanet(g2, planet2X, planet2Y, planet2R, planet2Col, false, Color.WHITE, new Color(200, 220, 255));
        if (planet1R > 35) {
            int mx = planet1X + planet1R + 18, my = planet1Y - 8;
            g2.setColor(new Color(160, 155, 140, 200));
            g2.fillOval(mx, my, 10, 10);
            g2.setColor(new Color(0, 0, 0, 80));
            g2.fillOval(mx + 4, my, 6, 10);
        }
    }

    private void drawSceneMars(Graphics2D g2) {
        drawSkyGradient(g2, new Color(30, 18, 22), new Color(120, 75, 45));
        g2.setColor(new Color(255, 245, 200, 180));
        g2.fillOval(WIDTH - 70, 18, 28, 28);
        g2.setColor(new Color(255, 240, 180, 60));
        g2.fillOval(WIDTH - 80, 10, 48, 48);
        g2.setColor(new Color(255, 235, 160, 25));
        g2.fillOval(WIDTH - 95, 2, 78, 78);
        drawStars(g2, 10, -10, -30, false);
        g2.setColor(new Color(60, 35, 28, 120));
        int[] farMx = { -20, 40, 90, 140, 200, 270, 330, 390, 460, 520, 580, WIDTH + 20 };
        int[] farMy = { HEIGHT, HEIGHT - 55, HEIGHT - 90, HEIGHT - 70, HEIGHT - 120, HEIGHT - 85, HEIGHT - 100,
                HEIGHT - 65, HEIGHT - 110, HEIGHT - 80, HEIGHT - 60, HEIGHT };
        g2.fillPolygon(farMx, farMy, farMx.length);
        g2.setColor(new Color(42, 18, 12, 210));
        int[] volX = { -30, 30, 80, 130, 160, 200, 240, 270, 310, WIDTH + 30 };
        int[] volY = { HEIGHT, HEIGHT - 70, HEIGHT - 140, HEIGHT - 190, HEIGHT - 210, HEIGHT - 195, HEIGHT - 170,
                HEIGHT - 120, HEIGHT - 70, HEIGHT };
        g2.fillPolygon(volX, volY, volX.length);
        g2.setColor(new Color(20, 8, 5, 200));
        g2.fillOval(150, HEIGHT - 225, 50, 15);
        for (int i = 0; i < 8; i++) {
            float speed = 0.2f + i * 0.05f;
            int wx = (int) ((i * 180 + frameCount * speed) % (WIDTH + 400)) - 200, wy = 60 + i * 55, wa = 8 + i * 2;
            g2.setColor(new Color(180, 110, 60, wa));
            g2.fillOval(wx, wy, 350, 30);
            g2.setColor(new Color(200, 130, 70, wa / 2));
            g2.fillOval(wx + 50, wy - 8, 200, 18);
        }
        for (int i = 0; i < crtX.length; i++) {
            int cy2 = (crtY[i] + frameCount / 5) % (HEIGHT + 120) - 60, cr = crtR[i];
            g2.setColor(new Color(140, 65, 35, 100));
            g2.fillOval(crtX[i] - cr - 2, cy2 - cr / 3 - 1, (cr + 2) * 2, (cr / 3 + 1) * 2);
            g2.setColor(new Color(55, 22, 12, 160));
            g2.fillOval(crtX[i] - cr + 4, cy2 - cr / 3 + 2, (cr - 4) * 2, (int) ((cr / 3 - 2) * 1.5));
        }
        for (int i = 0; i < rockX.length; i++) {
            int ry = (rockY[i] + frameCount / 4) % (HEIGHT + 80) - 40;
            g2.setColor(new Color(30, 10, 5, 100));
            g2.fillOval(rockX[i] + 4, ry + rockH[i] - 4, rockW[i], rockH[i] / 3);
            g2.setColor(new Color(130, 58, 28, 210));
            g2.fillRoundRect(rockX[i], ry, rockW[i], rockH[i], 8, 8);
            g2.setColor(new Color(170, 85, 40, 180));
            g2.fillRoundRect(rockX[i] + 2, ry + 2, rockW[i] - 4, rockH[i] / 3, 4, 4);
        }
        int phx = (int) ((frameCount * 0.18) % (WIDTH + 80)) - 40;
        g2.setColor(new Color(100, 70, 52, 220));
        g2.fillOval(phx, 35, 30, 20);
        g2.setColor(new Color(150, 75, 35, 200));
        g2.fillRect(0, HEIGHT - 35, WIDTH, 35);
        g2.setColor(new Color(110, 52, 24, 255));
        g2.fillRect(0, HEIGHT - 20, WIDTH, 20);
    }

    private void drawSceneEarth(Graphics2D g2) {
        drawSkyGradient(g2, new Color(2, 4, 18), new Color(20, 18, 40));
        g2.setColor(new Color(255, 120, 30, 25));
        g2.fillOval(-100, HEIGHT - 200, WIDTH + 200, 300);
        int mx2 = WIDTH - 120, my2 = 25, mr = 38;
        g2.setColor(new Color(255, 250, 220, 8));
        g2.fillOval(mx2 - mr - 30, my2 - mr - 30, (mr + 30) * 2, (mr + 30) * 2);
        g2.setColor(new Color(235, 232, 210, 230));
        g2.fillOval(mx2 - mr, my2 - mr, mr * 2, mr * 2);
        g2.setColor(new Color(160, 155, 138, 180));
        g2.fillOval(mx2 - 10, my2 - 15, 22, 18);
        g2.fillOval(mx2 + 5, my2 + 8, 15, 12);
        drawStars(g2, 15, 10, -20, false);
        for (int i = 0; i < 6; i++) {
            float speed = 0.25f + i * 0.08f;
            int cx2 = (int) ((i * 220 + frameCount * speed) % (WIDTH + 500)) - 250, cy2 = 30 + i * 35;
            g2.setColor(new Color(35, 38, 65, 70));
            g2.fillOval(cx2, cy2 + 10, 200, 50);
            g2.setColor(new Color(40, 44, 72, 80));
            g2.fillOval(cx2 + 30, cy2 - 8, 160, 55);
        }
        int[][] nearBlds = { { -15, 80, 310 }, { 50, 55, 280 }, { 115, 65, 340 }, { 190, 45, 260 }, { 245, 70, 380 },
                { 325, 50, 220 }, { 385, 75, 350 }, { 465, 60, 300 }, { 530, 70, 260 }, { 568, 85, 200 } };
        for (int[] b3 : nearBlds) {
            int bx2 = b3[0], bw2 = b3[1], bh2 = b3[2], by3 = HEIGHT - bh2;
            g2.setColor(new Color(10, 12, 28, 245));
            g2.fillRect(bx2, by3, bw2, bh2);
            for (int wy2 = by3 + 8; wy2 < HEIGHT - 12; wy2 += 11) {
                for (int wx2 = bx2 + 5; wx2 < bx2 + bw2 - 8; wx2 += 9) {
                    long seed2 = (long) wx2 * 31 + wy2 * 17 + wave * 7;
                    if (seed2 % 5 != 0 && seed2 % 11 != 3) {
                        g2.setColor(seed2 % 3 == 0 ? new Color(180, 210, 255, 140) : new Color(255, 235, 140, 130));
                        g2.fillRect(wx2, wy2, 5, 6);
                    }
                }
            }
        }
        g2.setColor(new Color(18, 18, 22, 255));
        g2.fillRect(0, HEIGHT - 42, WIDTH, 42);
        drawPlanet(g2, 70, 100, 55, new Color(50, 110, 210), false, Color.WHITE, new Color(100, 180, 255));
    }

    private void drawSceneAlien(Graphics2D g2) {
        drawSkyGradient(g2, new Color(4, 8, 22), new Color(18, 28, 12));
        drawPlanet(g2, 80, 60, 42, new Color(180, 210, 160), false, Color.WHITE, new Color(150, 255, 180));
        drawPlanet(g2, WIDTH - 90, 80, 22, new Color(220, 160, 200), true, new Color(255, 200, 220, 80),
                new Color(255, 180, 220));
        drawStars(g2, -30, 40, 40, false);
        for (int i = 0; i < orbX.length; i++) {
            int oy = (orbY[i] + frameCount / 3 + (int) (Math.sin(frameCount * 0.035 + i * 0.9) * 15)) % HEIGHT;
            Color oc3 = orbCol[i];
            float pulse2 = (float) (0.5 + 0.5 * Math.sin(frameCount * 0.07 + i * 1.3));
            g2.setColor(new Color(oc3.getRed(), oc3.getGreen(), oc3.getBlue(), (int) (25 * pulse2)));
            g2.fillOval(orbX[i] - orbR[i] * 3, oy - orbR[i] * 3, orbR[i] * 6, orbR[i] * 6);
            g2.setColor(new Color(oc3.getRed(), oc3.getGreen(), oc3.getBlue(), (int) (180 * pulse2)));
            g2.fillOval(orbX[i] - orbR[i], oy - orbR[i], orbR[i] * 2, orbR[i] * 2);
        }
        g2.setColor(new Color(8, 25, 15, 255));
        g2.fillRect(0, HEIGHT - 55, WIDTH, 55);
    }

    private void drawSceneAsteroid(Graphics2D g2) {
        drawSkyGradient(g2, new Color(1, 1, 8), new Color(3, 3, 14));
        drawStars(g2, 15, 8, 0, true);
        for (int i = 0; i < astX.length; i++) {
            int ay2 = (astY[i] + frameCount * astSpd[i] / 2) % (HEIGHT + 140) - 70, ar = astR[i] + 4;
            g2.setColor(new Color(65, 60, 52, 220));
            int[] apx = new int[6], apy = new int[6];
            for (int s = 0; s < 6; s++) {
                double a = 2 * Math.PI * s / 6;
                double r2 = ar * (0.72 + 0.28 * ((s * 7 + i * 13) % 10) / 10.0);
                apx[s] = (int) (astX[i] + Math.cos(a) * r2);
                apy[s] = (int) (ay2 + Math.sin(a) * r2 * 0.85);
            }
            g2.fillPolygon(apx, apy, 6);
        }
        drawPlanet(g2, planet2X, planet2Y + 30, planet2R + 10, new Color(210, 190, 140), true,
                new Color(200, 180, 120, 70), new Color(220, 200, 160));
    }

    private void drawSceneNebula(Graphics2D g2) {
        drawSkyGradient(g2, new Color(2, 1, 8), new Color(5, 2, 15));
        for (int i = 0; i < nebX.length; i++) {
            int ry = (nebY[i] + frameCount / 6) % (HEIGHT + 300) - 150;
            Color nc2 = nebCol[i];
            g2.setColor(new Color(nc2.getRed(), nc2.getGreen(), nc2.getBlue(), 12));
            g2.fillOval(nebX[i] - nebR[i] - 20, ry - nebR[i] / 2 - 10, (nebR[i] + 20) * 2, (int) (nebR[i] * 1.2));
            g2.setColor(new Color(nc2.getRed(), nc2.getGreen(), nc2.getBlue(), 20));
            g2.fillOval(nebX[i] - nebR[i], ry - nebR[i] / 2, nebR[i] * 2, nebR[i]);
        }
        drawStars(g2, 60, 30, 80, false);
        if (lbTimer > 0) {
            float lt = (float) lbTimer / 8;
            for (int i = 0; i < lbX1.length; i++) {
                int steps = 8;
                int[] bpx = new int[steps], bpy2 = new int[steps];
                bpx[0] = lbX1[i];
                bpy2[0] = lbY1[i];
                bpx[steps - 1] = lbX2[i];
                bpy2[steps - 1] = lbY2[i];
                for (int s = 1; s < steps - 1; s++) {
                    float sf = (float) s / (steps - 1);
                    bpx[s] = (int) (lbX1[i] + (lbX2[i] - lbX1[i]) * sf + (rand.nextInt(60) - 30));
                    bpy2[s] = (int) (lbY1[i] + (lbY2[i] - lbY1[i]) * sf);
                }
                g2.setColor(new Color(180, 150, 255, (int) (30 * lt)));
                g2.setStroke(new BasicStroke(8f));
                g2.drawPolyline(bpx, bpy2, steps);
                g2.setColor(new Color(240, 230, 255, (int) (200 * lt)));
                g2.setStroke(new BasicStroke(1f));
                g2.drawPolyline(bpx, bpy2, steps);
                g2.setStroke(new BasicStroke(1));
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // ★ SHOP SCREEN
    // ═══════════════════════════════════════════════════════════════════
    // Accent colour for each pool item (index 0-9)
    private static final Color[] SHOP_ACCENT = {
            new Color(60, 220, 80), // 0 BLOOD PACT – green
            new Color(255, 200, 0), // 1 AETHER STRIDE – yellow
            new Color(255, 130, 30), // 2 FRENZY CORE – orange
            new Color(0, 200, 180), // 3 CLOCK FRACTURE – teal
            new Color(255, 220, 80), // 4 GOLD RUSH – gold
            new Color(160, 0, 255), // 5 GHOST WALK – violet
            new Color(255, 60, 60), // 6 SINGULARITY – red
            new Color(200, 80, 255), // 7 NANO MEND – purple
            new Color(0, 180, 255), // 8 VOID MAGNET – cyan
            new Color(255, 100, 180), // 9 ECHO SHOT – pink
            new Color(255, 30, 30), // 10 DEATH MARK – crimson
    };

    private void drawShop(Graphics2D g2) {
        // ── Dark armory background ────────────────────────────────────
        GradientPaint bg = new GradientPaint(0, 0, new Color(4, 4, 14), 0, HEIGHT, new Color(10, 6, 28));
        g2.setPaint(bg);
        g2.fillRect(0, 0, WIDTH, HEIGHT);
        // Scanlines
        g2.setColor(new Color(0, 0, 0, 28));
        for (int y = 0; y < HEIGHT; y += 3)
            g2.fillRect(0, y, WIDTH, 1);
        // Stars
        for (int i = 0; i < starX.length; i++) {
            float f = (float) (0.3 + 0.2 * Math.sin(frameCount * 0.02 + i));
            int br = (int) (50 + 55 * f);
            g2.setColor(new Color(br, br, Math.min(255, br + 30)));
            g2.fillRect(starX[i], starY[i], starSz[i], starSz[i]);
        }

        // ── Header ────────────────────────────────────────────────────
        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillRect(0, 0, WIDTH, 152);
        // Glow divider
        g2.setPaint(new GradientPaint(0, 152, new Color(120, 60, 255, 0),
                WIDTH / 2, 152, new Color(180, 100, 255, 120)));
        g2.fillRect(0, 150, WIDTH, 2);

        g2.setFont(new Font("Arial", Font.BOLD, 11));
        g2.setColor(new Color(160, 100, 255, 200));
        String sub = "— BLACK MARKET —";
        g2.drawString(sub, WIDTH / 2 - g2.getFontMetrics().stringWidth(sub) / 2, 26);

        // Title shadow + text
        g2.setFont(new Font("Arial", Font.BOLD, 42));
        String title = "WAVE " + (wave - 1) + " CLEAR";
        int tw = g2.getFontMetrics().stringWidth(title);
        g2.setColor(new Color(100, 0, 200, 80));
        g2.drawString(title, WIDTH / 2 - tw / 2 + 2, 72);
        g2.setColor(new Color(230, 200, 255));
        g2.drawString(title, WIDTH / 2 - tw / 2, 70);

        // Score pill
        g2.setFont(new Font("Courier New", Font.BOLD, 19));
        String sc = "⬡ " + score + " pts";
        int sw = g2.getFontMetrics().stringWidth(sc);
        g2.setColor(new Color(50, 50, 0, 160));
        g2.fillRoundRect(WIDTH / 2 - sw / 2 - 12, 93, sw + 24, 26, 10, 10);
        g2.setColor(new Color(255, 220, 60));
        g2.drawString(sc, WIDTH / 2 - sw / 2, 112);

        // Hint
        g2.setFont(new Font("Arial", Font.PLAIN, 11));
        g2.setColor(new Color(90, 80, 130));
        String hint = "Most upgrades are PERMANENT — ⏱ marked ones are timed";
        g2.drawString(hint, WIDTH / 2 - g2.getFontMetrics().stringWidth(hint) / 2, 138);

        // ── 3 Item Cards ──────────────────────────────────────────────
        int cardW = 170, cardH = 228, gap = 18;
        int cx0 = WIDTH / 2 - (cardW * 3 + gap * 2) / 2, cardY = 162;

        for (int slot = 0; slot < SHOP_OFFERED; slot++) {
            int idx = shopOfferedItems[slot];
            int rx = cx0 + slot * (cardW + gap);
            boolean bought = shopBought[slot];
            boolean canAfford = score >= SHOP_COSTS[idx];
            int rar = SHOP_RARITY[idx];
            Color rarC = RARITY_COLOR[rar];
            Color ac = SHOP_ACCENT[idx];

            // Outer glow
            if (!bought && canAfford) {
                float pulse = (float) (0.3 + 0.2 * Math.sin(frameCount * 0.08 + slot * 1.1));
                g2.setColor(new Color(rarC.getRed(), rarC.getGreen(), rarC.getBlue(), (int) (38 * pulse)));
                g2.fillRoundRect(rx - 6, cardY - 6, cardW + 12, cardH + 12, 20, 20);
            }
            // Card body
            GradientPaint cBg = bought
                    ? new GradientPaint(rx, cardY, new Color(8, 28, 8), rx, cardY + cardH, new Color(4, 14, 4))
                    : new GradientPaint(rx, cardY, new Color(14, 10, 32), rx, cardY + cardH, new Color(6, 4, 18));
            g2.setPaint(cBg);
            g2.fillRoundRect(rx, cardY, cardW, cardH, 14, 14);
            // Border
            g2.setColor(bought ? new Color(40, 160, 40) : canAfford ? rarC : new Color(40, 38, 58));
            g2.setStroke(new BasicStroke(bought ? 2.5f : canAfford ? 2f : 1f));
            g2.drawRoundRect(rx, cardY, cardW, cardH, 14, 14);
            g2.setStroke(new BasicStroke(1));

            // Rarity badge
            g2.setColor(new Color(rarC.getRed(), rarC.getGreen(), rarC.getBlue(), bought ? 70 : 155));
            g2.fillRoundRect(rx + cardW / 2 - 34, cardY + 8, 68, 16, 6, 6);
            g2.setFont(new Font("Arial", Font.BOLD, 9));
            g2.setColor(new Color(6, 4, 18));
            String rLbl = RARITY_LABEL[rar];
            g2.drawString(rLbl, rx + cardW / 2 - g2.getFontMetrics().stringWidth(rLbl) / 2, cardY + 19);

            // Icon
            drawShopIcon(g2, idx, rx + cardW / 2, cardY + 66, bought ? 0.35f : 1f, frameCount);

            // PERMANENT badge
            g2.setFont(new Font("Arial", Font.BOLD, 8));
            g2.setColor(new Color(ac.getRed(), ac.getGreen(), ac.getBlue(), bought ? 60 : 130));
            g2.fillRoundRect(rx + cardW / 2 - 30, cardY + 96, 60, 13, 5, 5);
            g2.setColor(new Color(6, 4, 18));
            String permLabel = (idx == SHOP_BULLET_TIME || idx == SHOP_PHASE_SHIFT) ? "⏱ TEMPORARY" : "★ PERMANENT ★";
            g2.drawString(permLabel, rx + cardW / 2 - g2.getFontMetrics().stringWidth(permLabel) / 2,
                    cardY + 106);

            // Item name
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            g2.setColor(bought ? new Color(70, 160, 70) : canAfford ? Color.WHITE : new Color(80, 78, 100));
            FontMetrics nfm = g2.getFontMetrics();
            g2.drawString(SHOP_NAMES[idx], rx + cardW / 2 - nfm.stringWidth(SHOP_NAMES[idx]) / 2, cardY + 124);

            // Stat line
            g2.setFont(new Font("Arial", Font.BOLD, 10));
            g2.setColor(bought ? new Color(50, 130, 50)
                    : canAfford ? new Color(ac.getRed(), ac.getGreen(), ac.getBlue(), 220) : new Color(65, 63, 85));
            FontMetrics dfm = g2.getFontMetrics();
            int descW = dfm.stringWidth(SHOP_DESCS[idx]);
            g2.drawString(SHOP_DESCS[idx], rx + (cardW - descW) / 2, cardY + 140);

            // Flavor text (word-wrapped)
            g2.setFont(new Font("Arial", Font.ITALIC, 9));
            g2.setColor(bought ? new Color(40, 100, 40, 170) : new Color(110, 100, 150, 200));
            String flav = SHOP_FLAVORS[idx];
            FontMetrics ffm = g2.getFontMetrics();
            if (ffm.stringWidth(flav) > cardW - 16) {
                int sp = flav.lastIndexOf(' ', flav.length() / 2);
                if (sp < 0)
                    sp = flav.length() / 2;
                g2.drawString(flav.substring(0, sp), rx + cardW / 2 - ffm.stringWidth(flav.substring(0, sp)) / 2,
                        cardY + 158);
                g2.drawString(flav.substring(sp + 1), rx + cardW / 2 - ffm.stringWidth(flav.substring(sp + 1)) / 2,
                        cardY + 169);
            } else {
                g2.drawString(flav, rx + cardW / 2 - ffm.stringWidth(flav) / 2, cardY + 163);
            }

            // Price / ACQUIRED
            if (bought) {
                g2.setColor(new Color(40, 160, 40, 200));
                g2.fillRoundRect(rx + cardW / 2 - 36, cardY + cardH - 42, 72, 24, 8, 8);
                g2.setFont(new Font("Arial", Font.BOLD, 12));
                g2.setColor(new Color(6, 4, 18));
                g2.drawString("ACQUIRED", rx + cardW / 2 - g2.getFontMetrics().stringWidth("ACQUIRED") / 2,
                        cardY + cardH - 25);
            } else {
                Color prC = canAfford ? new Color(255, 210, 50) : new Color(200, 60, 60);
                g2.setColor(new Color(prC.getRed(), prC.getGreen(), prC.getBlue(), 40));
                g2.fillRoundRect(rx + cardW / 2 - 36, cardY + cardH - 44, 72, 26, 8, 8);
                g2.setColor(prC);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(rx + cardW / 2 - 36, cardY + cardH - 44, 72, 26, 8, 8);
                g2.setStroke(new BasicStroke(1));
                g2.setFont(new Font("Courier New", Font.BOLD, 15));
                String price = "$" + SHOP_COSTS[idx];
                g2.setColor(prC);
                g2.drawString(price, rx + cardW / 2 - g2.getFontMetrics().stringWidth(price) / 2, cardY + cardH - 27);
                if (!canAfford) {
                    g2.setFont(new Font("Arial", Font.PLAIN, 8));
                    g2.setColor(new Color(180, 60, 60, 180));
                    String nd = "need $" + (SHOP_COSTS[idx] - score) + " more";
                    g2.drawString(nd, rx + cardW / 2 - g2.getFontMetrics().stringWidth(nd) / 2, cardY + cardH - 12);
                }
            }
        }

        // ── Active upgrades row ───────────────────────────────────────
        int rowY = cardY + cardH + 20;
        g2.setFont(new Font("Arial", Font.BOLD, 11));
        g2.setColor(new Color(100, 90, 140));
        g2.drawString("ACTIVE UPGRADES:", cx0, rowY);
        int ex = cx0 + 132;
        String[] aNames = { "AETHER STRIDE", "FRENZY CORE", "CLOCK FRACTURE", "GOLD RUSH", "GHOST WALK" };
        boolean[] aFlags = { shopSpeedBoost, shopRapidFire, shopBulletTime, shopScoreRush, shopPhaseShift };
        int[] aIdx = { 1, 2, 3, 4, 5 };
        boolean any = false;
        for (int i = 0; i < aFlags.length; i++) {
            if (!aFlags[i])
                continue;
            any = true;
            Color ac2 = SHOP_ACCENT[aIdx[i]];
            g2.setFont(new Font("Arial", Font.BOLD, 10));
            int lw = g2.getFontMetrics().stringWidth(aNames[i]);
            g2.setColor(new Color(ac2.getRed(), ac2.getGreen(), ac2.getBlue(), 160));
            g2.fillRoundRect(ex - 2, rowY - 11, lw + 10, 14, 5, 5);
            g2.setColor(new Color(6, 4, 18));
            g2.drawString(aNames[i], ex + 3, rowY);
            ex += lw + 16;
        }
        if (!any) {
            g2.setFont(new Font("Arial", Font.ITALIC, 10));
            g2.setColor(new Color(60, 58, 80));
            g2.drawString("none yet", ex, rowY);
        }

        // HP pips
        g2.setFont(new Font("Arial", Font.BOLD, 11));
        g2.setColor(new Color(120, 180, 255));
        g2.drawString("HP:", WIDTH - 110, rowY);
        for (int i = 0; i < player.lives; i++) {
            int hx = WIDTH - 88 + i * 20;
            g2.setColor(i == 0 ? new Color(255, 80, 80) : Color.CYAN);
            g2.fillPolygon(new int[] { hx + 7, hx, hx + 14 }, new int[] { rowY - 13, rowY + 1, rowY + 1 }, 3);
        }

        // ── Continue button ───────────────────────────────────────────
        int btnY = rowY + 24;
        Rectangle cont = new Rectangle(WIDTH / 2 - 115, btnY, 230, 50);
        g2.setPaint(new GradientPaint(cont.x, cont.y, new Color(28, 10, 65),
                cont.x, cont.y + cont.height, new Color(14, 5, 38)));
        g2.fillRoundRect(cont.x, cont.y, cont.width, cont.height, 12, 12);
        g2.setColor(new Color(155, 90, 255));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(cont.x, cont.y, cont.width, cont.height, 12, 12);
        g2.setStroke(new BasicStroke(1));
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        g2.setColor(Color.WHITE);
        String contLbl = "ENTER THE FRAY  →";
        g2.drawString(contLbl, cont.x + cont.width / 2 - g2.getFontMetrics().stringWidth(contLbl) / 2,
                cont.y + cont.height / 2 + 7);
        for (int i = 0; i < SHOP_OFFERED; i++) {
            int rx = cx0 + i * (cardW + gap);
            btnShopItems[i].setBounds(rx, cardY, cardW, cardH);
        }
        btnShopContinue.setBounds(cont.x, cont.y, cont.width, cont.height);

        // Pickup flash
        if (pickupTimer > 0) {
            float alpha = Math.min(1f, pickupTimer / 30f);
            g2.setFont(new Font("Arial", Font.BOLD, 18));
            FontMetrics pm = g2.getFontMetrics();
            int msgX = WIDTH / 2 - pm.stringWidth(pickupMsg) / 2;
            int msgY = cont.y + cont.height + 34;
            g2.setColor(new Color(0, 0, 0, (int) (alpha * 140)));
            g2.fillRoundRect(msgX - 10, msgY - 22, pm.stringWidth(pickupMsg) + 20, 28, 8, 8);
            g2.setColor(new Color(120, 255, 160, (int) (alpha * 230)));
            g2.drawString(pickupMsg, msgX, msgY);
        }
    }

    /** Unique animated icon for each shop item, centered at (cx,cy). */
    private void drawShopIcon(Graphics2D g2, int idx, int cx, int cy, float dim, int fc) {
        float pulse = (float) (0.75 + 0.25 * Math.sin(fc * 0.10 + idx));
        int a = (int) (215 * pulse * dim);
        Color ac = SHOP_ACCENT[idx];
        Color c = new Color(ac.getRed(), ac.getGreen(), ac.getBlue(), a);
        Color cw = new Color(255, 255, 255, a);
        g2.setStroke(new BasicStroke(2f));
        switch (idx) {
            case SHOP_EXTRA_LIFE: { // heart
                int[] hx = { cx, cx - 10, cx - 12, cx - 6, cx, cx + 6, cx + 12, cx + 10 };
                int[] hy = { cy + 10, cy - 2, cy - 8, cy - 12, cy - 8, cy - 12, cy - 8, cy - 2 };
                g2.setColor(c);
                g2.fillPolygon(hx, hy, 8);
                g2.setColor(cw);
                g2.drawPolygon(hx, hy, 8);
                break;
            }
            case SHOP_SPEED_BOOST: { // lightning bolt
                int[] lx = { cx - 3, cx + 5, cx - 1, cx + 7, cx - 5, cx + 3, cx - 1 };
                int[] ly = { cy - 14, cy - 2, cy - 2, cy + 14, cy + 2, cy + 2, cy - 14 };
                g2.setColor(c);
                g2.fillPolygon(lx, ly, 7);
                g2.setColor(cw);
                g2.drawPolygon(lx, ly, 7);
                break;
            }
            case SHOP_RAPID_FIRE: { // 3 stacked bullets
                for (int i = 0; i < 3; i++) {
                    int bx = cx - 16 + i * 16, by = cy - 6;
                    g2.setColor(c);
                    g2.fillRoundRect(bx, by, 8, 14, 4, 4);
                    g2.setColor(cw);
                    g2.drawRoundRect(bx, by, 8, 14, 4, 4);
                }
                break;
            }
            case SHOP_BULLET_TIME: { // clock face
                g2.setColor(c);
                g2.fillOval(cx - 13, cy - 13, 26, 26);
                g2.setColor(cw);
                g2.drawOval(cx - 13, cy - 13, 26, 26);
                g2.setColor(new Color(6, 4, 18, a));
                double ha = Math.toRadians(-90 + fc * 3);
                g2.drawLine(cx, cy, cx + (int) (8 * Math.cos(ha)), cy + (int) (8 * Math.sin(ha)));
                g2.drawLine(cx, cy, cx + (int) (11 * Math.cos(Math.toRadians(-90 + fc * 8))),
                        cy + (int) (11 * Math.sin(Math.toRadians(-90 + fc * 8))));
                break;
            }
            case SHOP_SCORE_RUSH: { // coin stack
                for (int i = 2; i >= 0; i--) {
                    int cy2 = cy + 4 - i * 5;
                    g2.setColor(i == 0 ? c : new Color(ac.getRed() / 2, ac.getGreen() / 2, 0, a));
                    g2.fillOval(cx - 11, cy2 - 4, 22, 9);
                    g2.setColor(cw);
                    g2.drawOval(cx - 11, cy2 - 4, 22, 9);
                }
                break;
            }
            case SHOP_PHASE_SHIFT: { // ghost
                int ga = (int) (185 * pulse * dim);
                Color gc = new Color(ac.getRed(), ac.getGreen(), ac.getBlue(), ga);
                int[] gx = { cx - 10, cx - 10, cx - 7, cx - 4, cx, cx + 4, cx + 7, cx + 10, cx + 10 };
                int[] gy = { cy + 12, cy - 6, cy - 12, cy - 14, cy - 12, cy - 14, cy - 12, cy - 6, cy + 12 };
                g2.setColor(gc);
                g2.fillPolygon(gx, gy, 9);
                g2.setColor(new Color(255, 255, 255, ga));
                g2.drawPolygon(gx, gy, 9);
                g2.setColor(new Color(6, 4, 18, ga));
                g2.fillOval(cx - 5, cy - 6, 4, 5);
                g2.fillOval(cx + 1, cy - 6, 4, 5);
                break;
            }
            case SHOP_NUKE: { // starburst
                for (int i = 0; i < 8; i++) {
                    double ang = i * Math.PI / 4 + Math.toRadians(fc * 2);
                    g2.setColor(c);
                    g2.setStroke(new BasicStroke(3f));
                    g2.drawLine(cx + (int) (5 * Math.cos(ang)), cy + (int) (5 * Math.sin(ang)),
                            cx + (int) (14 * Math.cos(ang)), cy + (int) (14 * Math.sin(ang)));
                }
                g2.setColor(c);
                g2.fillOval(cx - 6, cy - 6, 12, 12);
                g2.setColor(cw);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(cx - 6, cy - 6, 12, 12);
                break;
            }
            case SHOP_REPAIR: { // cross / plus
                g2.setColor(c);
                g2.fillRect(cx - 3, cy - 12, 6, 24);
                g2.fillRect(cx - 12, cy - 3, 24, 6);
                g2.setColor(cw);
                g2.drawRect(cx - 3, cy - 12, 6, 24);
                g2.drawRect(cx - 12, cy - 3, 24, 6);
                break;
            }
            case SHOP_VOID_MAGNET: { // magnet shape
                g2.setColor(c);
                g2.setStroke(new BasicStroke(4f));
                g2.drawArc(cx - 11, cy - 12, 22, 22, 0, 180);
                g2.setStroke(new BasicStroke(4f));
                g2.drawLine(cx - 11, cy + 2, cx - 11, cy + 10);
                g2.drawLine(cx + 11, cy + 2, cx + 11, cy + 10);
                g2.setColor(new Color(255, 80, 80, a));
                g2.setStroke(new BasicStroke(4f));
                g2.drawLine(cx - 11, cy + 7, cx - 11, cy + 11);
                g2.setColor(new Color(80, 80, 255, a));
                g2.drawLine(cx + 11, cy + 7, cx + 11, cy + 11);
                break;
            }
            case SHOP_ECHO_SHOT: { // two offset bullets
                for (int i = 0; i < 2; i++) {
                    int ox = i == 0 ? -6 : 6, oy = i == 0 ? -4 : 4;
                    int ba = i == 0 ? a : a / 2;
                    g2.setColor(new Color(ac.getRed(), ac.getGreen(), ac.getBlue(), ba));
                    g2.fillRoundRect(cx + ox - 3, cy + oy - 10, 6, 14, 3, 3);
                    g2.setColor(new Color(255, 255, 255, ba));
                    g2.drawRoundRect(cx + ox - 3, cy + oy - 10, 6, 14, 3, 3);
                }
                break;
            }
            case SHOP_DEATH_MARK: { // skull-like X mark
                g2.setColor(c);
                g2.setStroke(new BasicStroke(3.5f));
                g2.drawLine(cx - 10, cy - 10, cx + 10, cy + 10);
                g2.drawLine(cx + 10, cy - 10, cx - 10, cy + 10);
                g2.setColor(cw);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(cx - 13, cy - 13, 26, 26);
                break;
            }
        }
        g2.setStroke(new BasicStroke(1));
    }

    // ═══════════════════════════════════════════════════════════════════
    // ★ 4 NEW SCENES
    // ═══════════════════════════════════════════════════════════════════

    // SCENE 6: VOID — deep wormhole / interdimensional rift
    private void drawSceneVoid(Graphics2D g2) {
        drawSkyGradient(g2, new Color(0, 0, 2), new Color(4, 0, 12));
        // Wormhole spiral rings
        int cx = WIDTH / 2, cy = HEIGHT / 2;
        for (int r = 200; r > 10; r -= 18) {
            float t = (float) (frameCount * 0.012 + r * 0.04);
            int alpha = Math.max(0, Math.min(80, (200 - r) / 2));
            float hue = (r + frameCount * 0.5f) / 300f % 1f;
            int rc = (int) (Math.sin(hue * Math.PI * 2) * 80 + 80);
            int gc = (int) (Math.sin(hue * Math.PI * 2 + 2.1) * 80 + 80);
            int bc = (int) (Math.sin(hue * Math.PI * 2 + 4.2) * 80 + 80);
            g2.setColor(new Color(rc, gc, bc, alpha));
            g2.setStroke(new BasicStroke(2.2f));
            int ox = (int) (Math.cos(t) * r * 0.03), oy = (int) (Math.sin(t) * r * 0.03);
            g2.drawOval(cx - r + ox, cy - r + oy, r * 2, r * 2);
        }
        g2.setStroke(new BasicStroke(1));
        // Central void eye
        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillOval(cx - 12, cy - 12, 24, 24);
        g2.setColor(new Color(120, 0, 200, 180));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(cx - 12, cy - 12, 24, 24);
        g2.setStroke(new BasicStroke(1));
        // Stars being distorted toward centre
        rand.setSeed(12345);
        for (int i = 0; i < 80; i++) {
            int sx2 = rand.nextInt(WIDTH), sy2 = rand.nextInt(HEIGHT);
            double dx = cx - sx2, dy2 = cy - sy2;
            double dist = Math.sqrt(dx * dx + dy2 * dy2);
            double pull = Math.max(0, 1 - dist / 300) * Math.sin(frameCount * 0.04 + i) * 3;
            int fx = (int) (sx2 + dx / dist * pull), fy = (int) (sy2 + dy2 / dist * pull);
            int br2 = 60 + rand.nextInt(80);
            g2.setColor(new Color(br2, br2, Math.min(255, br2 + 40), br2));
            g2.fillRect(fx, fy, 1, 1);
        }
        // Streaks radiating outward (like motion blur of falling in)
        rand.setSeed(frameCount / 4L * 4 + 99);
        for (int i = 0; i < 20; i++) {
            double ang = rand.nextDouble() * Math.PI * 2;
            double len = 40 + rand.nextDouble() * 120;
            int r2 = 80 + rand.nextInt(150);
            int x1 = (int) (cx + Math.cos(ang) * r2), y1 = (int) (cy + Math.sin(ang) * r2);
            int x2 = (int) (cx + Math.cos(ang) * (r2 + len)), y2 = (int) (cy + Math.sin(ang) * (r2 + len));
            int alpha2 = rand.nextInt(40) + 10;
            float hue2 = (float) (ang / (Math.PI * 2));
            g2.setColor(new Color((int) (Math.abs(Math.sin(hue2 * 3)) * 120 + 80),
                    (int) (Math.abs(Math.sin(hue2 * 3 + 2)) * 80 + 30),
                    (int) (Math.abs(Math.sin(hue2 * 3 + 4)) * 120 + 80), alpha2));
            g2.setStroke(new BasicStroke(0.8f));
            g2.drawLine(x1, y1, x2, y2);
            g2.setStroke(new BasicStroke(1));
        }
        // Purple/violet nebula wisps
        for (int i = 0; i < 5; i++) {
            int nx = (int) ((i * 160 + frameCount * 0.2) % (WIDTH + 300)) - 150;
            int ny = 80 + i * 120;
            g2.setColor(new Color(80, 0, 140, 10));
            g2.fillOval(nx, ny, 300, 80);
        }
    }

    // SCENE 7: SOLAR STORM — inside the sun's corona, plasma eruptions
    private void drawSceneStorm(Graphics2D g2) {
        drawSkyGradient(g2, new Color(20, 4, 0), new Color(60, 18, 0));
        // Plasma wave sweeps
        for (int i = 0; i < 6; i++) {
            double t = (double) (frameCount * 0.008 + i * 0.9);
            int wx = (int) ((Math.sin(t) * 0.5 + 0.5) * (WIDTH + 200)) - 100;
            int wy = (int) (i * 130 + Math.cos(t * 1.3) * 40);
            int alpha = 12 + i * 3;
            g2.setColor(new Color(255, 100 + i * 15, 0, alpha));
            g2.fillOval(wx - 150, wy - 40, 400, 100);
        }
        // Solar filaments (dark arching loops)
        g2.setColor(new Color(150, 40, 0, 120));
        g2.setStroke(new BasicStroke(3f));
        for (int i = 0; i < 4; i++) {
            int bx = (int) ((i * 180 + frameCount * 0.3) % (WIDTH + 200)) - 100;
            g2.drawArc(bx, HEIGHT / 2 - 80 + i * 30, 160, 120, 0, 180);
        }
        g2.setStroke(new BasicStroke(1));
        // Bright plasma dots / flares
        rand.setSeed(frameCount / 3L * 3);
        for (int i = 0; i < 30; i++) {
            int fx = rand.nextInt(WIDTH), fy = rand.nextInt(HEIGHT);
            int fs = 1 + rand.nextInt(4);
            int falpha = 40 + rand.nextInt(80);
            g2.setColor(new Color(255, 200 + rand.nextInt(55), 50, falpha));
            g2.fillOval(fx, fy, fs, fs);
        }
        // Solar prominence eruptions
        for (int i = 0; i < 3; i++) {
            int phase = (frameCount + i * 120) % 360;
            if (phase < 120) {
                float pf = (float) phase / 120;
                int px2 = (int) ((i + 1) * WIDTH / 4);
                int ph = (int) (pf * 200);
                // Eruption column
                g2.setColor(new Color(255, (int) (120 + 60 * pf), 0, (int) (80 * (1 - pf))));
                g2.fillOval(px2 - 20, HEIGHT - ph - 40, 40, (int) (ph * 0.6));
                // Tip glow
                g2.setColor(new Color(255, 240, 100, (int) (120 * (1 - pf))));
                g2.fillOval(px2 - 12, HEIGHT - ph - 52, 24, 24);
            }
        }
        // Ground: molten surface
        g2.setColor(new Color(80, 20, 0, 255));
        g2.fillRect(0, HEIGHT - 40, WIDTH, 40);
        // Lava ripples
        for (int i = 0; i < WIDTH; i += 20) {
            int lh = (int) (6 + 4 * Math.sin((i + frameCount * 2) * 0.08));
            g2.setColor(new Color(255, 80, 0, 180));
            g2.fillOval(i, HEIGHT - 40, 18, lh * 2);
        }
        // Scattered dim stars barely visible
        drawStars(g2, 80, -20, -60, false);
    }

    // SCENE 8: ICE PLANET — frozen tundra, aurora, frozen structures
    private void drawSceneIce(Graphics2D g2) {
        drawSkyGradient(g2, new Color(0, 4, 18), new Color(5, 18, 35));
        // Aurora borealis bands
        int[][] auroraCols = { { 0, 200, 120 }, { 0, 150, 255 }, { 80, 0, 200 }, { 0, 220, 180 } };
        for (int band = 0; band < 4; band++) {
            int[] ac = auroraCols[band];
            for (int x2 = 0; x2 < WIDTH; x2 += 2) {
                double t = (double) (frameCount * 0.02 + x2 * 0.015 + band * 1.2);
                int ay = (int) (80 + band * 55 + Math.sin(t) * 30 + Math.cos(t * 0.7) * 15);
                int ah = (int) (20 + Math.sin(t * 0.5 + 1) * 15);
                int alpha = (int) (20 + 12 * Math.sin(frameCount * 0.04 + x2 * 0.02 + band));
                g2.setColor(new Color(ac[0], ac[1], ac[2], Math.max(0, alpha)));
                g2.fillRect(x2, ay, 2, ah);
            }
        }
        // Stars — blue-white tinted cold stars
        drawStars(g2, -20, 0, 60, true);
        // Distant frozen mountains
        g2.setColor(new Color(30, 45, 80, 200));
        int[] iceMx = { -10, 30, 80, 130, 180, 220, 270, 310, 360, 400, 450, 500, 550, WIDTH + 10 };
        int[] iceMy = { HEIGHT, HEIGHT - 70, HEIGHT - 120, HEIGHT - 80, HEIGHT - 150, HEIGHT - 90, HEIGHT - 130,
                HEIGHT - 70, HEIGHT - 110, HEIGHT - 85, HEIGHT - 140, HEIGHT - 75, HEIGHT - 95, HEIGHT };
        g2.fillPolygon(iceMx, iceMy, iceMx.length);
        // Snow highlights on peaks
        g2.setColor(new Color(200, 220, 255, 180));
        for (int i = 1; i < iceMx.length - 2; i += 2) {
            if (iceMy[i] < HEIGHT - 100) {
                g2.fillPolygon(
                        new int[] { iceMx[i] - 12, iceMx[i], iceMx[i] + 12 },
                        new int[] { iceMy[i] + 20, iceMy[i], iceMy[i] + 20 }, 3);
            }
        }
        // Ice crystal spires in foreground
        for (int i = 0; i < 10; i++) {
            int sx2 = i * 62 + 10, sh = 40 + i % 3 * 30;
            g2.setColor(new Color(140, 200, 255, 180));
            g2.fillPolygon(new int[] { sx2, sx2 + 10, sx2 + 20 }, new int[] { HEIGHT - sh, HEIGHT, HEIGHT }, 3);
            // Refraction shimmer
            float shimmer = (float) (0.5 + 0.5 * Math.sin(frameCount * 0.07 + i * 0.8));
            g2.setColor(new Color(200, 240, 255, (int) (60 * shimmer)));
            g2.fillPolygon(new int[] { sx2 + 2, sx2 + 8, sx2 + 14 }, new int[] { HEIGHT - sh + 4, HEIGHT, HEIGHT }, 3);
        }
        // Frozen ground
        g2.setColor(new Color(120, 170, 220, 255));
        g2.fillRect(0, HEIGHT - 28, WIDTH, 28);
        // Ice cracks
        g2.setColor(new Color(180, 220, 255, 160));
        g2.setStroke(new BasicStroke(0.8f));
        rand.setSeed(33333);
        for (int i = 0; i < 15; i++) {
            int cx2 = rand.nextInt(WIDTH), cy2 = HEIGHT - rand.nextInt(20);
            int cx3 = cx2 + rand.nextInt(50) - 25, cy3 = cy2 + rand.nextInt(12);
            g2.drawLine(cx2, cy2, cx3, cy3);
        }
        g2.setStroke(new BasicStroke(1));
        // Snowflakes drifting
        rand.setSeed(frameCount / 6L * 6 + 11);
        for (int i = 0; i < 35; i++) {
            int sx2 = rand.nextInt(WIDTH);
            int sy2 = (rand.nextInt(HEIGHT) + frameCount * (1 + rand.nextInt(2)) / 2) % HEIGHT;
            g2.setColor(new Color(200, 230, 255, 100 + rand.nextInt(80)));
            g2.fillOval(sx2, sy2, 2 + rand.nextInt(3), 2 + rand.nextInt(3));
        }
    }

    // SCENE 9: ALIEN JUNGLE PLANET — dense bioluminescent overgrowth
    private void drawSceneJungle(Graphics2D g2) {
        drawSkyGradient(g2, new Color(2, 10, 8), new Color(8, 25, 14));
        // Alien moon — large reddish
        g2.setColor(new Color(200, 100, 60, 200));
        g2.fillOval(WIDTH - 130, 15, 80, 80);
        g2.setColor(new Color(160, 70, 40, 100));
        g2.fillOval(WIDTH - 120, 25, 30, 25);
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillOval(WIDTH - 115, 15, 70, 80);
        // Dim stars through thick atmosphere
        drawStars(g2, -40, 20, 0, false);
        // Far jungle canopy silhouette (deep layer)
        g2.setColor(new Color(5, 22, 10, 220));
        int[] farJx = new int[WIDTH / 6 + 2], farJy = new int[WIDTH / 6 + 2];
        farJx[0] = 0;
        farJy[0] = HEIGHT;
        for (int i = 1; i < WIDTH / 6 + 1; i++) {
            farJx[i] = i * 6;
            farJy[i] = HEIGHT - 80 - (int) (35 * Math.sin(i * 0.4 + wave)) - ((i * 17) % 40);
        }
        farJx[WIDTH / 6 + 1] = WIDTH;
        farJy[WIDTH / 6 + 1] = HEIGHT;
        g2.fillPolygon(farJx, farJy, farJx.length);
        // Mid jungle canopy — alien colours
        g2.setColor(new Color(10, 40, 18, 230));
        int[] midJx = new int[WIDTH / 4 + 2], midJy = new int[WIDTH / 4 + 2];
        midJx[0] = 0;
        midJy[0] = HEIGHT;
        for (int i = 1; i < WIDTH / 4 + 1; i++) {
            midJx[i] = i * 4;
            midJy[i] = HEIGHT - 120 - (int) (50 * Math.sin(i * 0.3 + wave * 0.5)) - ((i * 23) % 60);
        }
        midJx[WIDTH / 4 + 1] = WIDTH;
        midJy[WIDTH / 4 + 1] = HEIGHT;
        g2.fillPolygon(midJx, midJy, midJx.length);
        // Bioluminescent tree trunks
        for (int i = 0; i < 8; i++) {
            int tx2 = i * 80 + 20;
            int th = 200 + ((i * 37) % 100);
            int tw = 12 + i % 3 * 4;
            // Dark trunk
            g2.setColor(new Color(5, 25, 12, 220));
            g2.fillRect(tx2, HEIGHT - th, tw, th);
            // Glowing bark veins
            float vg = Math.max(0f, Math.min(1f, (float) (0.4 + 0.6 * Math.sin(frameCount * 0.05 + i * 0.9))));
            g2.setColor(new Color(0, 180, 80, (int) (60 * vg)));
            g2.fillRect(tx2 + tw / 3, HEIGHT - th + 20, 2, th - 20);
            // Canopy blob
            g2.setColor(new Color(8, 50, 20, 220));
            g2.fillOval(tx2 - 30, HEIGHT - th - 30, tw + 60, 50);
            g2.setColor(new Color(15, 70, 30, 180));
            g2.fillOval(tx2 - 20, HEIGHT - th - 50, tw + 40, 40);
            // Glowing canopy tips
            g2.setColor(new Color(0, 255, 100, (int) (100 * vg)));
            g2.fillOval(tx2 - 10, HEIGHT - th - 60, tw + 20, 25);
            g2.setColor(new Color(0, 255, 100, (int) (25 * vg)));
            g2.fillOval(tx2 - 20, HEIGHT - th - 65, tw + 40, 35);
        }
        // Floating spores drifting upward
        rand.setSeed(frameCount / 5L * 5 + 44);
        for (int i = 0; i < 25; i++) {
            int sx2 = rand.nextInt(WIDTH);
            int sy2 = (HEIGHT + 100 - ((rand.nextInt(HEIGHT) + frameCount * (1 + rand.nextInt(2))) % (HEIGHT + 100)));
            float sf = Math.max(0f, Math.min(1f, (float) (0.5 + 0.5 * Math.sin(frameCount * 0.06 + i))));
            g2.setColor(new Color(0, 255, 120, Math.max(0, Math.min(255, (int) (60 * sf)))));
            g2.fillOval(sx2, sy2, 3, 3);
        }
        // Foreground thick roots/vines
        g2.setColor(new Color(8, 30, 15, 240));
        g2.fillRect(0, HEIGHT - 55, WIDTH, 55);
        // Glowing ground mushrooms
        for (int i = 0; i < 12; i++) {
            int mx = i * 50 + 10;
            float mg = Math.max(0f, Math.min(1f, (float) (0.4 + 0.6 * Math.sin(frameCount * 0.06 + i * 1.2))));
            // Cap
            g2.setColor(new Color(0, 200, 80, (int) (120 * mg)));
            g2.fillOval(mx - 10, HEIGHT - 62, 22, 14);
            // Stalk
            g2.setColor(new Color(0, 140, 50, 180));
            g2.fillRect(mx - 2, HEIGHT - 52, 5, 12);
            // Glow
            g2.setColor(new Color(0, 255, 100, (int) (20 * mg)));
            g2.fillOval(mx - 18, HEIGHT - 68, 38, 28);
        }
    }

    private void drawSceneJapan(Graphics2D g2) {
        // Calm day/dusk Japan — soft pink sky
        drawSkyGradient(g2, new Color(100, 160, 210), new Color(160, 200, 230));

        // Soft sun
        g2.setColor(new Color(255, 220, 180, 60));
        g2.fillOval(WIDTH - 160, 20, 120, 120);
        g2.setColor(new Color(255, 200, 150, 100));
        g2.fillOval(WIDTH - 140, 40, 80, 80);
        g2.setColor(new Color(255, 230, 200, 200));
        g2.fillOval(WIDTH - 125, 55, 50, 50);

        // Distant mountains
        g2.setColor(new Color(140, 100, 130, 120));
        int[] dmx = { -10, 60, 130, 200, 270, 340, 410, 480, 550, WIDTH + 10 };
        int[] dmy = { HEIGHT, HEIGHT - 100, HEIGHT - 160, HEIGHT - 120, HEIGHT - 180,
                HEIGHT - 140, HEIGHT - 170, HEIGHT - 110, HEIGHT - 130, HEIGHT };
        g2.fillPolygon(dmx, dmy, dmx.length);
        // Snow caps
        g2.setColor(new Color(255, 255, 255, 160));
        g2.fillPolygon(new int[] { 130, 160, 190 }, new int[] { HEIGHT - 160, HEIGHT - 190, HEIGHT - 160 }, 3);
        g2.fillPolygon(new int[] { 270, 300, 330 }, new int[] { HEIGHT - 180, HEIGHT - 210, HEIGHT - 180 }, 3);

        // Grass ground base
        g2.setColor(new Color(80, 140, 70, 255));
        g2.fillRect(0, HEIGHT - 60, WIDTH, 60);
        g2.setColor(new Color(100, 160, 80, 255));
        g2.fillRect(0, HEIGHT - 45, WIDTH, 45);

        // Torii gate — vermillion
        g2.setColor(new Color(180, 40, 20, 220));
        g2.fillRect(180, HEIGHT - 260, 16, 220);
        g2.fillRect(280, HEIGHT - 260, 16, 220);
        g2.fillRect(150, HEIGHT - 260, 76, 14);
        g2.fillRect(138, HEIGHT - 248, 14, 9);
        g2.fillRect(288, HEIGHT - 248, 14, 9);
        g2.fillRect(160, HEIGHT - 234, 56, 10);

        // Stone path
        g2.setColor(new Color(160, 150, 140, 180));
        g2.fillRect(WIDTH / 2 - 30, HEIGHT - 60, 60, 60);
        for (int i = 0; i < 5; i++) {
            g2.setColor(new Color(140, 130, 120, 160));
            g2.fillRect(WIDTH / 2 - 26, HEIGHT - 58 + i * 12, 52, 10);
        }

        // Sakura trees
        int[] treeX = { 60, 420, 520 };
        for (int i = 0; i < treeX.length; i++) {
            int tx = treeX[i];
            int th = 160 + i * 20;
            // Trunk
            g2.setColor(new Color(80, 50, 30, 220));
            g2.fillRect(tx - 5, HEIGHT - th, 10, th - 40);
            // Branches
            g2.setColor(new Color(90, 55, 35, 200));
            g2.setStroke(new BasicStroke(3f));
            g2.drawLine(tx, HEIGHT - th + 20, tx - 30, HEIGHT - th - 20);
            g2.drawLine(tx, HEIGHT - th + 20, tx + 28, HEIGHT - th - 15);
            g2.setStroke(new BasicStroke(1));
            // Blossom clusters
            int[] bx = { tx - 35, tx - 15, tx + 5, tx + 25, tx - 25, tx + 15, tx };
            int[] by2 = { HEIGHT - th - 18, HEIGHT - th - 35, HEIGHT - th - 40, HEIGHT - th - 25,
                    HEIGHT - th - 50, HEIGHT - th - 48, HEIGHT - th - 60 };
            for (int b = 0; b < bx.length; b++) {
                float bg = (float) (0.6 + 0.4 * Math.sin(frameCount * 0.04 + b + i));
                g2.setColor(new Color(240, 160, 180, Math.max(0, Math.min(255, (int) (180 * bg)))));
                g2.fillOval(bx[b] - 18, by2[b] - 18, 36, 36);
                g2.setColor(new Color(255, 190, 210, Math.max(0, Math.min(255, (int) (220 * bg)))));
                g2.fillOval(bx[b] - 12, by2[b] - 12, 24, 24);
            }
        }

        // Stone lanterns
        int[] stx = { 340, 390 };
        for (int i = 0; i < stx.length; i++) {
            int sx = stx[i];
            int sy = HEIGHT - 80;
            g2.setColor(new Color(120, 110, 100, 230));
            g2.fillRect(sx, sy, 14, 50);
            g2.fillRect(sx - 6, sy - 20, 26, 22);
            g2.fillRect(sx - 3, sy - 28, 20, 10);
            g2.fillRect(sx - 1, sy - 36, 16, 10);
            float sg = (float) (0.5 + 0.5 * Math.sin(frameCount * 0.05 + i));
            g2.setColor(new Color(255, 200, 80, Math.max(0, Math.min(255, (int) (80 * sg)))));
            g2.fillRect(sx - 4, sy - 18, 22, 18);
        }

        // Falling sakura petals
        rand.setSeed(frameCount / 4L * 4 + 55);
        for (int i = 0; i < 40; i++) {
            int px2 = rand.nextInt(WIDTH);
            int py2 = (rand.nextInt(HEIGHT) + frameCount * (1 + rand.nextInt(2))) % HEIGHT;
            float ps = (float) (0.5 + 0.5 * Math.sin(frameCount * 0.05 + i));
            g2.setColor(new Color(255, 180, 200, Math.max(0, Math.min(255, (int) (160 * ps)))));
            g2.fillOval(px2, py2, 5 + rand.nextInt(4), 4 + rand.nextInt(3));
        }

        // Soft mist at base
        for (int i = 0; i < 4; i++) {
            int mstx = (int) ((i * 200 + frameCount * 0.3) % (WIDTH + 400)) - 200;
            g2.setColor(new Color(255, 220, 230, 18));
            g2.fillOval(mstx, HEIGHT - 75, 320, 50);
        }
    }

    private void drawSceneKitsune(Graphics2D g2) {
        drawSkyGradient(g2, new Color(8, 2, 20), new Color(40, 4, 30));

        // Fox fire orbs
        rand.setSeed(frameCount / 3L * 3 + 77);
        for (int i = 0; i < 18; i++) {
            int fox = (int) ((i * 80 + frameCount * (0.3 + i * 0.05)) % (WIDTH + 100)) - 50;
            int foy = (int) (120 + i * 35 + Math.sin(frameCount * 0.04 + i * 0.9) * 25);
            double fopRaw = 0.4 + 0.6 * Math.sin(frameCount * 0.06 + i * 1.3);
            float fop = (float) Math.max(0, Math.min(1, fopRaw));
            g2.setColor(new Color(100, 255, 180, Math.max(0, Math.min(255, (int) (18 * fop)))));
            g2.fillOval(fox - 22, foy - 22, 44, 44);
            g2.setColor(new Color(120, 255, 160, Math.max(0, Math.min(255, (int) (60 * fop)))));
            g2.fillOval(fox - 10, foy - 10, 20, 20);
            g2.setColor(new Color(200, 255, 220, Math.max(0, Math.min(255, (int) (180 * fop)))));
            g2.fillOval(fox - 4, foy - 4, 8, 8);
        }

        drawStars(g2, 40, -20, 60, true);

        // Spectral moon
        int kmx = WIDTH / 2 - 20;
        int kmy = 45;
        int kmr = 55;
        g2.setColor(new Color(120, 60, 180, 25));
        g2.fillOval(kmx - kmr - 35, kmy - kmr - 35, (kmr + 35) * 2, (kmr + 35) * 2);
        g2.setColor(new Color(160, 100, 220, 60));
        g2.fillOval(kmx - kmr - 15, kmy - kmr - 15, (kmr + 15) * 2, (kmr + 15) * 2);
        g2.setColor(new Color(220, 200, 255));
        g2.fillOval(kmx - kmr, kmy - kmr, kmr * 2, kmr * 2);
        g2.setColor(new Color(180, 150, 220, 120));
        g2.fillOval(kmx - 20, kmy - 18, 28, 20);
        g2.fillOval(kmx + 10, kmy + 10, 18, 14);
        for (int r = 1; r <= 5; r++) {
            g2.setColor(new Color(140, 80, 220, Math.max(0, 40 - r * 8)));
            g2.setStroke(new BasicStroke(r * 2.5f));
            g2.drawOval(kmx - kmr - r * 3, kmy - kmr - r * 3,
                    (kmr + r * 3) * 2, (kmr + r * 3) * 2);
        }
        g2.setStroke(new BasicStroke(1));

        // Torii gate violet
        g2.setColor(new Color(60, 10, 80, 200));
        g2.fillRect(85, HEIGHT - 310, 18, 270);
        g2.fillRect(195, HEIGHT - 310, 18, 270);
        g2.fillRect(55, HEIGHT - 310, 82, 16);
        g2.fillRect(42, HEIGHT - 298, 16, 10);
        g2.fillRect(258, HEIGHT - 298, 16, 10);
        g2.fillRect(65, HEIGHT - 282, 62, 11);
        float ktg = (float) (0.5 + 0.5 * Math.sin(frameCount * 0.04));
        g2.setColor(new Color(180, 80, 255, Math.max(0, Math.min(255, (int) (30 * ktg)))));
        g2.fillRect(50, HEIGHT - 315, 120, 280);

        // Spirit lanterns
        for (int i = 0; i < 5; i++) {
            int klx = 95 + i * 22;
            int kly = HEIGHT - 290;
            double klgRaw = 0.5 + 0.5 * Math.sin(frameCount * 0.08 + i * 1.1);
            float klg = (float) Math.max(0, Math.min(1, klgRaw));
            g2.setColor(new Color(80, 255, 160, Math.max(0, Math.min(255, (int) (50 * klg)))));
            g2.fillOval(klx - 6, kly - 4, 22, 26);
            g2.setColor(new Color(100, 255, 180, Math.max(0, Math.min(255, (int) (180 * klg)))));
            g2.fillRoundRect(klx, kly, 10, 18, 4, 4);
            g2.setColor(new Color(80, 40, 100, 140));
            g2.drawLine(klx + 5, kly, klx + 5, kly - 10);
        }

        // Spirit wisps
        for (int i = 0; i < 10; i++) {
            int kwx = (int) ((i * 65 + frameCount * 0.5) % WIDTH);
            int kwy = HEIGHT - 60 - (int) ((frameCount * 0.8 + i * 40) % 200);
            double kwaRaw = 0.3 + 0.3 * Math.sin(frameCount * 0.05 + i);
            float kwa = (float) Math.max(0, Math.min(1, kwaRaw));
            g2.setColor(new Color(100, 255, 160, Math.max(0, Math.min(255, (int) (40 * kwa)))));
            g2.fillOval(kwx - 8, kwy - 8, 16, 30);
            g2.setColor(new Color(180, 255, 220, Math.max(0, Math.min(255, (int) (60 * kwa)))));
            g2.fillOval(kwx - 3, kwy - 3, 6, 10);
        }

        // Petals
        rand.setSeed(frameCount / 5L * 5 + 33);
        for (int i = 0; i < 40; i++) {
            int kpx = rand.nextInt(WIDTH);
            int kpy = (rand.nextInt(HEIGHT) + frameCount * (1 + rand.nextInt(2))) % HEIGHT;
            double kpsRaw = 0.4 + 0.6 * Math.sin(frameCount * 0.04 + i);
            float kps = (float) Math.max(0, Math.min(1, kpsRaw));
            g2.setColor(new Color(220, 180, 255, Math.max(0, Math.min(255, (int) (100 * kps)))));
            g2.fillOval(kpx, kpy, 4 + rand.nextInt(4), 3 + rand.nextInt(3));
        }

        // Mist
        for (int i = 0; i < 7; i++) {
            int kgx = (int) ((i * 160 + frameCount * 0.3) % (WIDTH + 400)) - 200;
            g2.setColor(new Color(100, 40, 140, 14));
            g2.fillOval(kgx, HEIGHT - 80, 320, 70);
        }

        // Ground
        g2.setColor(new Color(10, 4, 22, 255));
        g2.fillRect(0, HEIGHT - 38, WIDTH, 38);
        g2.setColor(new Color(60, 20, 90, 180));
        g2.fillRect(0, HEIGHT - 42, WIDTH, 5);
    }

    // ── Menu ──────────────────────────────────────────────────────────
    private void drawMenu(Graphics2D g2) {
        g2.setColor(new Color(255, 60, 60, 160));
        for (int i = 0; i < 6; i++) {
            int bx = (int) ((i * 110 + frameCount * 1.8) % (WIDTH + 40)) - 20;
            int by = 262 + (int) (Math.sin(frameCount * 0.06 + i * 1.1) * 28);
            g2.fillOval(bx - 5, by - 5, 10, 10);
        }
        g2.setFont(new Font("Arial", Font.BOLD, 58));
        FontMetrics fm = g2.getFontMetrics();
        String title = "BULLET HELL";
        int tx = WIDTH / 2 - fm.stringWidth(title) / 2;
        g2.setColor(new Color(0, 180, 255, 55));
        for (int off = 4; off >= 1; off--)
            g2.drawString(title, tx - off, 197 + off);
        g2.setPaint(new GradientPaint(0, 150, new Color(0, 220, 255), WIDTH, 220, new Color(140, 0, 255)));
        g2.drawString(title, tx, 197);
        g2.setFont(new Font("Arial", Font.ITALIC, 18));
        g2.setColor(new Color(160, 160, 230));
        String sub = "SURVIVE  THE  ONSLAUGHT";
        g2.drawString(sub, WIDTH / 2 - g2.getFontMetrics().stringWidth(sub) / 2, 230);
        drawBtn(g2, btnStart, "START", true);
        drawBtn(g2, btnSettings, "SETTINGS", true);
        drawBtn(g2, btnQuit, "QUIT", true);
        // Drawn icons (no Unicode)
        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        g2.setColor(new Color(80, 80, 120));
        g2.drawString("v4.4", WIDTH - 40, HEIGHT - 10);
    }

    // ── Settings ──────────────────────────────────────────────────────
    private void drawSettings(Graphics2D g2) {
        centeredTitle(g2, "SETTINGS", 80);
        divider(g2, 90);
        sectionLabel(g2, "FIRING MODE", 148);
        drawBtn(g2, btnFireMouse, "MOUSE CLICK", fireMode == FIRE_MOUSE);
        drawBtn(g2, btnFireSpace, "SPACEBAR", fireMode == FIRE_SPACE);
        divider(g2, 310);
        sectionLabel(g2, "AUDIO", 340);
        drawBtn(g2, btnMusicToggle, musicEnabled ? "MUSIC :  ON" : "MUSIC :  OFF", musicEnabled);
        g2.setFont(new Font("Arial", Font.BOLD, 13));
        g2.setColor(new Color(100, 180, 255));
        g2.drawString("VOLUME", WIDTH / 2 - 110, 432);
        g2.setFont(new Font("Arial", Font.PLAIN, 13));
        g2.setColor(new Color(200, 200, 255));
        g2.drawString(musicVolPct + "%", WIDTH / 2 + 85, 432);
        drawVolumeSlider(g2);
        divider(g2, 480);
        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        g2.setColor(new Color(80, 80, 130));
        String ctrl = "Move: WASD / Arrows   Fire: " + (fireMode == FIRE_MOUSE ? "Left Click" : "Spacebar")
                + "   Aim: Mouse";
        g2.drawString(ctrl, WIDTH / 2 - g2.getFontMetrics().stringWidth(ctrl) / 2, 510);
        drawBtn(g2, btnSettBack, "BACK", true);
        g2.setColor(new Color(0, 210, 255, 200));
        g2.fillPolygon(
                new int[] { btnSettBack.x + 18, btnSettBack.x + 28, btnSettBack.x + 28 },
                new int[] { btnSettBack.y + 25, btnSettBack.y + 18, btnSettBack.y + 32 }, 3);
        ;
    }

    private void drawVolumeSlider(Graphics2D g2) {
        Rectangle r = sliderTrack;
        g2.setColor(new Color(20, 20, 60));
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        int fillW = (int) (r.width * musicVolPct / 100.0);
        if (fillW > 0) {
            g2.setColor(new Color(0, 160, 255));
            g2.fillRoundRect(r.x, r.y, fillW, r.height, 10, 10);
        }
        g2.setColor(new Color(0, 120, 200));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        g2.setStroke(new BasicStroke(1));
        int tx2 = r.x + fillW;
        g2.setColor(Color.WHITE);
        g2.fillOval(tx2 - 9, r.y + r.height / 2 - 9, 18, 18);
        g2.setColor(new Color(0, 160, 255));
        g2.setStroke(new BasicStroke(2));
        g2.drawOval(tx2 - 9, r.y + r.height / 2 - 9, 18, 18);
        g2.setStroke(new BasicStroke(1));
    }

    // ── Diff select ───────────────────────────────────────────────────
    private void drawDiffSelect(Graphics2D g2) {
        centeredTitle(g2, "SELECT DIFFICULTY", 100);
        divider(g2, 112);
        g2.setFont(new Font("Arial", Font.ITALIC, 14));
        g2.setColor(new Color(160, 160, 230));
        String sub = "Choose your challenge";
        g2.drawString(sub, WIDTH / 2 - g2.getFontMetrics().stringWidth(sub) / 2, 150);
        drawDiffBtn(g2, btnDiffEasy, "EASY", new Color(60, 200, 80), "Slower bullets", "5 lives", difficulty == 0);
        drawDiffBtn(g2, btnDiffNormal, "NORMAL", new Color(0, 180, 255), "Standard pace", "3 lives", difficulty == 1);
        drawDiffBtn(g2, btnDiffHard, "HARD", new Color(255, 80, 60), "Faster bullets", "2 lives", difficulty == 2);
        drawBtn(g2, btnDiffBack, "BACK", true);
        g2.setColor(new Color(0, 210, 255, 200));
        g2.fillPolygon(
                new int[] { btnDiffBack.x + 18, btnDiffBack.x + 28, btnDiffBack.x + 28 },
                new int[] { btnDiffBack.y + 25, btnDiffBack.y + 18, btnDiffBack.y + 32 }, 3);
    }

    private void drawDiffBtn(Graphics2D g2, Rectangle r, String title, Color accent, String l1, String l2,
            boolean sel) {
        // Outer glow ONLY when selected
        if (sel) {
            for (int glow = 4; glow >= 1; glow--) {
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 9 * glow));
                g2.fillRoundRect(r.x - glow * 3, r.y - glow * 3, r.width + glow * 6, r.height + glow * 6, 18, 18);
            }
        }
        // Body — nearly black when unselected, dark navy when selected
        g2.setColor(sel ? new Color(10, 14, 40) : new Color(30, 30, 55, 255));
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 12, 12);
        // Top shimmer only when selected
        if (sel) {
            g2.setPaint(new GradientPaint(r.x, r.y, new Color(255, 255, 255, 18), r.x, r.y + r.height,
                    new Color(255, 255, 255, 0)));
            g2.fillRoundRect(r.x, r.y, r.width, r.height / 2, 12, 12);
        }
        // Border — bright accent when selected, barely visible when not
        g2.setColor(sel ? accent : new Color(accent.getRed() / 2, accent.getGreen() / 2, accent.getBlue() / 2, 160));
        g2.setStroke(new BasicStroke(sel ? 2.2f : 1.4f));
        g2.drawRoundRect(r.x, r.y, r.width, r.height, 12, 12);
        g2.setStroke(new BasicStroke(1));
        // Left accent bar only when selected
        if (sel) {
            g2.setColor(accent);
            g2.fillRoundRect(r.x, r.y + 6, 4, r.height - 12, 3, 3);
        }
        // Title — white when selected, dim when not
        g2.setFont(new Font("Arial", Font.BOLD, 20));
        g2.setColor(sel ? Color.WHITE : new Color(160, 160, 180));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(title, r.x + r.width / 2 - fm.stringWidth(title) / 2, r.y + 28);
        // Bullet points
        g2.setFont(new Font("Arial", Font.PLAIN, 13));
        g2.setColor(sel ? new Color(200, 220, 255) : new Color(170, 170, 200));
        g2.drawString("- " + l1, r.x + 20, r.y + 50);
        g2.drawString("- " + l2, r.x + 20, r.y + 68);
        // SELECTED badge on right side
        if (sel) {
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 35));
            g2.fillRoundRect(r.x + r.width - 82, r.y + r.height / 2 - 9, 72, 18, 6, 6);
            g2.setColor(accent);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(r.x + r.width - 82, r.y + r.height / 2 - 9, 72, 18, 6, 6);
            g2.setStroke(new BasicStroke(1));
            g2.setFont(new Font("Arial", Font.BOLD, 9));
            g2.setColor(accent);
            FontMetrics sfm = g2.getFontMetrics();
            g2.drawString("SELECTED", r.x + r.width - 82 + (72 - sfm.stringWidth("SELECTED")) / 2,
                    r.y + r.height / 2 + 3);
        }
    }

    // ── Class select (5 cards, single row, no DEF stat) ──────────────
    private static final String[] CLS_NAMES = { "MACHINE\nGUNNER", "NOVA", "PHANTOM", "VIPER" };
    private static final String[] CLS_DESC = {
            "Rapid fire.\nOverheats.",
            "Charge laser.\nOne big hit.",
            "Dash+decoy.\nSHIFT=blink.",
            "Every 5 hits\n applies stacking poison.\n(3-9% boss HP,\n ticks every 2s)"
    };
    private static final Color[] CLS_COL = {
            new Color(0, 220, 255), new Color(130, 80, 255), new Color(180, 0, 255),
            new Color(0, 255, 100)
    };
    // Stats: ATK, SPD, MOB (3 stats only — DEF removed)
    private static final int[][] CLS_STATS = {
            { 4, 5, 2 }, { 5, 1, 2 }, { 2, 3, 5 }, { 3, 2, 3 }
    };

    private void drawClassSelect(Graphics2D g2) {
        centeredTitle(g2, "SELECT CLASS", 55);
        divider(g2, 66);
        g2.setFont(new Font("Arial", Font.ITALIC, 12));
        g2.setColor(new Color(160, 160, 230));
        String sub = "Click to select  ·  click again to start  ·  SHIFT = special ability";
        g2.drawString(sub, WIDTH / 2 - g2.getFontMetrics().stringWidth(sub) / 2, 100);
        for (int i = 0; i < CLASS_COUNT; i++)
            drawSmallClassCard(g2, btnClass[i], i, selectedClass == i);
        drawBtn(g2, btnClassBack, "BACK", true);
        g2.setColor(new Color(0, 210, 255, 200));
        g2.fillPolygon(
                new int[] { btnClassBack.x + 18, btnClassBack.x + 28, btnClassBack.x + 28 },
                new int[] { btnClassBack.y + 25, btnClassBack.y + 18, btnClassBack.y + 32 }, 3);
        ;
    }

    private void drawSmallClassCard(Graphics2D g2, Rectangle r, int id, boolean sel) {
        Color accent = CLS_COL[id];
        if (sel) {
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 35));
            g2.fillRoundRect(r.x - 5, r.y - 5, r.width + 10, r.height + 10, 16, 16);
        }
        g2.setColor(sel ? new Color(8, 15, 50) : new Color(10, 10, 28));
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 12, 12);
        g2.setColor(sel ? accent : new Color(40, 40, 90));
        g2.setStroke(new BasicStroke(sel ? 2.5f : 1f));
        g2.drawRoundRect(r.x, r.y, r.width, r.height, 12, 12);
        g2.setStroke(new BasicStroke(1));

        // Mini ship icon
        int scx = r.x + r.width / 2, sy = r.y + 26;
        g2.setColor(accent);
        switch (id) {
            case CLASS_MACHINE_GUNNER:
                g2.fillPolygon(new int[] { scx, scx - 11, scx + 11 }, new int[] { sy, sy + 22, sy + 22 }, 3);
                break;
            case CLASS_NOVA:
                g2.fillPolygon(new int[] { scx, scx - 8, scx + 8 }, new int[] { sy, sy + 22, sy + 22 }, 3);
                g2.setColor(accent.darker());
                g2.fillPolygon(new int[] { scx - 8, scx - 16, scx - 8 }, new int[] { sy + 14, sy + 22, sy + 22 }, 3);
                g2.fillPolygon(new int[] { scx + 8, scx + 16, scx + 8 }, new int[] { sy + 14, sy + 22, sy + 22 }, 3);
                break;
            case CLASS_PHANTOM:
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 160));
                g2.fillPolygon(new int[] { scx, scx - 10, scx + 10 }, new int[] { sy, sy + 22, sy + 22 }, 3);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 60));
                g2.fillPolygon(new int[] { scx + 5, scx - 5, scx + 15 }, new int[] { sy + 3, sy + 23, sy + 23 }, 3);
                break;
            case CLASS_VIPER:
                g2.fillPolygon(new int[] { scx, scx - 8, scx + 8 }, new int[] { sy, sy + 22, sy + 22 }, 3);
                for (int i2 = 0; i2 < 3; i2++) {
                    double a = Math.toRadians(-80 + i2 * 80);
                    g2.drawLine(scx, (int) (sy + 11), (int) (scx + Math.cos(a) * 14),
                            (int) (sy + 11 + Math.sin(a) * 14));
                }
                break;
            default:
                g2.fillPolygon(new int[] { scx, scx - 10, scx + 10 }, new int[] { sy, sy + 22, sy + 22 }, 3);
                break;
        }

        // Name
        String[] tl = CLS_NAMES[id].split("\\n");
        g2.setFont(new Font("Arial", Font.BOLD, 11));
        g2.setColor(sel ? Color.WHITE : new Color(160, 160, 220));
        int ty = r.y + 60;
        for (String ln : tl) {
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(ln, r.x + r.width / 2 - fm.stringWidth(ln) / 2, ty);
            ty += 14;
        }

        // Desc
        String[] dl = CLS_DESC[id].split("\\n");
        g2.setFont(new Font("Arial", Font.PLAIN, 9));
        g2.setColor(sel ? new Color(180, 220, 255) : new Color(80, 80, 130));
        int dy = ty + 2;
        for (String ln : dl) {
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(ln, r.x + r.width / 2 - fm.stringWidth(ln) / 2, dy);
            dy += 11;
        }

        // Stat pips — 3 stats only (ATK, SPD, MOB)
        String[] sl2 = { "ATK", "FR", "MOB" };
        int[] sv = CLS_STATS[id];
        int sbY = r.y + r.height - 38;
        for (int i2 = 0; i2 < 3; i2++) {
            g2.setFont(new Font("Arial", Font.PLAIN, 7));
            g2.setColor(new Color(100, 100, 160));
            g2.drawString(sl2[i2], r.x + 4, sbY + i2 * 11 + 7);
            for (int p2 = 0; p2 < 5; p2++) {
                g2.setColor(p2 < sv[i2] ? (sel ? accent : accent.darker()) : new Color(20, 20, 40));
                g2.fillRect(r.x + 26 + p2 * 8, sbY + i2 * 11, 6, 6);
            }
        }
        if (sel) {
            g2.setFont(new Font("Arial", Font.BOLD, 12));
            g2.setColor(accent);
            String st = "CLICK TO START";
            int stX = r.x + r.width / 2 - g2.getFontMetrics().stringWidth(st) / 2 + 6;
            g2.drawString(st, stX, r.y + r.height - 4);
            g2.fillPolygon(new int[] { stX - 10, stX - 10, stX - 2 },
                    new int[] { r.y + r.height - 13, r.y + r.height - 3, r.y + r.height - 8 }, 3);
        }
    }

    // ── In-game ───────────────────────────────────────────────────────
    private void drawGame(Graphics2D g2) {
        if (boss.alive)
            boss.draw(g2, frameCount, player);
        for (PowerUp p : powerUps)
            p.draw(g2);
        for (Bullet b : playerBullets)
            b.draw(g2);
        for (Bullet b : enemyBullets)
            b.draw(g2);

        // Nova FX
        if (selectedClass == CLASS_NOVA) {
            if (novaCharging && novaChargeTimer > 0) {
                float cf = (float) novaChargeTimer / NOVA_CHARGE_FRAMES;
                int pcx = player.x + player.size / 2, pcy = player.y + player.size / 2;
                int ringR = (int) (14 + 28 * cf);
                g2.setColor(new Color((int) (80 + 175 * cf), (int) (80 + 160 * cf), 255, (int) (60 + 100 * cf)));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawOval(pcx - ringR, pcy - ringR, ringR * 2, ringR * 2);
                g2.setStroke(new BasicStroke(1));
                int barW = 60, barH = 6, barX = pcx - barW / 2, barY = player.y - 16;
                g2.setColor(new Color(10, 10, 40));
                g2.fillRoundRect(barX, barY, barW, barH, 4, 4);
                g2.setColor(new Color((int) (80 + 120 * cf), (int) (80 + 130 * cf), 255));
                g2.fillRoundRect(barX, barY, (int) (barW * cf), barH, 4, 4);
                g2.setFont(new Font("Arial", Font.BOLD, 10));
                g2.setColor(new Color(160, 180, 255));
                String ct = cf >= 0.99f ? "RELEASE!" : "CHARGING";
                g2.drawString(ct, barX + barW / 2 - g2.getFontMetrics().stringWidth(ct) / 2, barY - 3);
            }
            for (NovaParticle np : novaParticles)
                np.draw(g2);
            if (novaLaserActive) {
                float t = (float) novaLaserTimer / NOVA_LASER_FRAMES;
                float[] widths = { NOVA_LASER_WIDTH * 3f, NOVA_LASER_WIDTH * 2f, NOVA_LASER_WIDTH,
                        NOVA_LASER_WIDTH * 0.4f };
                Color[] cols = { new Color(60, 100, 255, (int) (18 * t)), new Color(100, 160, 255, (int) (50 * t)),
                        new Color(160, 210, 255, (int) (120 * t)), new Color(240, 250, 255, (int) (240 * t)) };
                for (int gi = 0; gi < 4; gi++) {
                    g2.setColor(cols[gi]);
                    g2.setStroke(new BasicStroke(widths[gi], BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(novaBeamX1, novaBeamY1, novaBeamX2, novaBeamY2);
                }
                g2.setStroke(new BasicStroke(1));
            }
            if (novaCooldownTimer > 0 && !novaCharging && !novaLaserActive) {
                float cdf = (float) novaCooldownTimer / NOVA_COOLDOWN_FRAMES;
                int pcx = player.x + player.size / 2, barW = 50, barH = 5, barX = pcx - barW / 2, barY = player.y - 14;
                g2.setColor(new Color(10, 10, 40));
                g2.fillRoundRect(barX, barY, barW, barH, 4, 4);
                g2.setColor(new Color(200, 100, 60));
                g2.fillRoundRect(barX, barY, (int) (barW * cdf), barH, 4, 4);
            }
        }

        for (ExplosionParticle ep : explosionParticles)
            ep.draw(g2);
        for (DamageIndicator di : damageIndicators)
            di.draw(g2);

        // Sentinel orb FX

        // Phantom decoy FX
        if (selectedClass == CLASS_PHANTOM && phantomDecoyT > 0) {
            float da = (float) phantomDecoyT / PHANTOM_DECOY_LIFE;
            g2.setColor(new Color(180, 0, 255, (int) (120 * da)));
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval((int) phantomDecoyX - 14, (int) phantomDecoyY - 14, 28, 28);
            g2.setStroke(new BasicStroke(1));
            g2.setFont(new Font("Arial", Font.BOLD, 8));
            g2.setColor(new Color(200, 100, 255, (int) (180 * da)));
            g2.drawString("DECOY", (int) phantomDecoyX - 16, (int) phantomDecoyY + 4);
        }
        // Phantom afterimage
        if (selectedClass == CLASS_PHANTOM && phantomAfterT > 0) {
            float fa = (float) phantomAfterT / 20;
            g2.setColor(new Color(180, 0, 255, (int) (70 * fa)));
            g2.fillPolygon(new int[] { phantomAfterX + 15, phantomAfterX, phantomAfterX + 30 },
                    new int[] { phantomAfterY, phantomAfterY + 30, phantomAfterY + 30 }, 3);
        }
        // Snake FX
        for (Snake s : snakes)
            s.draw(g2);

        if (player.alive)
            player.draw(g2);

        if (player.alive) {
            boolean canShow = selectedClass == CLASS_MACHINE_GUNNER ? !overheated
                    : (!novaLaserActive && novaCooldownTimer == 0);
            if (canShow) {
                int pcx = player.x + player.size / 2, pcy = player.y + player.size / 2;
                g2.setColor(new Color(255, 50, 50, 200));
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                        new float[] { 6f, 8f }, frameCount * 0.5f));
                g2.drawLine(pcx, pcy, mouseX, mouseY);
                g2.setStroke(new BasicStroke(1));
                Color xc = selectedClass == CLASS_NOVA ? new Color(160, 100, 255, 140) : new Color(0, 220, 255, 120);
                g2.setColor(xc);
                g2.drawOval(mouseX - 8, mouseY - 8, 16, 16);
                g2.drawLine(mouseX - 12, mouseY, mouseX - 4, mouseY);
                g2.drawLine(mouseX + 4, mouseY, mouseX + 12, mouseY);
                g2.drawLine(mouseX, mouseY - 12, mouseX, mouseY - 4);
                g2.drawLine(mouseX, mouseY + 4, mouseX, mouseY + 12);
            }
        }

        if (hasShield) {
            int r = player.size / 2 + 14, cx = player.x + player.size / 2, cy = player.y + player.size / 2;
            g2.setColor(new Color(100, 180, 255, 55));
            g2.fillOval(cx - r, cy - r, r * 2, r * 2);
            g2.setColor(new Color(100, 200, 255, 200));
            g2.setStroke(new BasicStroke(2));
            g2.drawOval(cx - r, cy - r, r * 2, r * 2);
            g2.setStroke(new BasicStroke(1));
        }

        g2.setFont(new Font("Courier New", Font.BOLD, 16));
        g2.setColor(Color.WHITE);
        g2.drawString("SCORE: " + score, 10, 24);
        g2.setFont(new Font("Arial", Font.PLAIN, 10));
        g2.setColor(new Color(120, 120, 180));
        g2.setFont(new Font("Arial", Font.PLAIN, 10));
        g2.setColor(new Color(120, 120, 180));
        if (hasSingularity) {
            g2.setFont(new Font("Arial", Font.BOLD, 11));
            g2.setColor(new Color(255, 80, 80, (int) (180 + 75 * Math.abs(Math.sin(frameCount * 0.08)))));
            g2.drawString("SINGULARITY READY", WIDTH - 145, HEIGHT - 70);
            g2.setFont(new Font("Arial", Font.PLAIN, 9));
            g2.setColor(new Color(200, 200, 200));
            g2.drawString("PRESS E TO USE", WIDTH - 130, HEIGHT - 56);
        }
        g2.setFont(new Font("Arial", Font.PLAIN, 9));
        g2.setColor(new Color(120, 120, 160));
        g2.drawString("BEST:", WIDTH - 100, 40);
        g2.setFont(new Font("Courier New", Font.BOLD, 13));
        g2.setColor(score >= highScores[difficulty] ? new Color(255, 220, 60) : new Color(160, 160, 210));
        g2.drawString(String.valueOf(highScores[difficulty]), WIDTH - 100, 55);
        g2.setFont(new Font("Arial", Font.PLAIN, 9));
        g2.setColor(new Color(120, 120, 160));
        g2.drawString("WAVE:", WIDTH - 100, 68);
        g2.setFont(new Font("Courier New", Font.BOLD, 13));
        g2.setColor(new Color(200, 220, 255));
        g2.drawString(String.valueOf(wave), WIDTH - 100, 81);
        if (wave % 5 == 0) {
            g2.setFont(new Font("Arial", Font.BOLD, 11));
            g2.setColor(new Color(255, 60, 60, (int) (180 + 70 * Math.abs(Math.sin(frameCount * 0.12)))));
            String apexLabel = "⚡ APEX";
            g2.drawString(apexLabel, WIDTH / 2 - g2.getFontMetrics().stringWidth(apexLabel) / 2, 24);
        } else {
            String[] dn = { "EASY", "NORMAL", "HARD" };
            g2.setFont(new Font("Arial", Font.PLAIN, 11));
            g2.setColor(new Color(120, 120, 180));
            g2.drawString(dn[difficulty], WIDTH / 2 - 20, 24);
        }
        for (int i = 0; i < player.lives; i++)
            drawMiniShip(g2, 10 + i * 22, HEIGHT - 32);
        drawActivePowerupIcons(g2);
        if (selectedClass == CLASS_MACHINE_GUNNER)
            drawHeatBar(g2);

        if (pickupTimer > 0) {
            float alpha = Math.min(1f, pickupTimer / 30f);
            g2.setFont(new Font("Arial", Font.BOLD, 22));
            FontMetrics fm = g2.getFontMetrics();
            int msgX = WIDTH / 2 - fm.stringWidth(pickupMsg) / 2;
            Color mc = overheated && pickupMsg.equals("OVERHEATED!") ? new Color(255, 80, 40, (int) (alpha * 230))
                    : new Color(255, 230, 80, (int) (alpha * 230));
            g2.setColor(new Color(0, 0, 0, (int) (alpha * 130)));
            g2.fillRoundRect(msgX - 10, HEIGHT / 2 - 50, fm.stringWidth(pickupMsg) + 20, 34, 10, 10);
            g2.setColor(mc);
            g2.drawString(pickupMsg, msgX, HEIGHT / 2 - 26);
        }

        if (boss.alive) {
            int bw = 300, bx = WIDTH / 2 - 150;
            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(bx, HEIGHT - 26, bw, 13);
            double pct = (double) boss.hp / boss.maxHp;
            if (wave % 5 == 0)
                g2.setColor(pct > 0.5 ? new Color(220, 160, 0) : pct > 0.25 ? new Color(255, 80, 0) : Color.RED);
            else
                g2.setColor(pct > 0.5 ? Color.RED : pct > 0.25 ? Color.ORANGE : Color.YELLOW);
            g2.fillRect(bx, HEIGHT - 26, (int) (pct * bw), 13);
            g2.setColor(Color.WHITE);
            g2.drawRect(bx, HEIGHT - 26, bw, 13);
            g2.setFont(new Font("Arial", Font.BOLD, 11));
            String bossName = boss.getBossName();
            g2.drawString(bossName, bx + bw / 2 - g2.getFontMetrics().stringWidth(bossName) / 2, HEIGHT - 14);
            g2.setFont(new Font("Arial", Font.BOLD, 10));
            g2.setColor(new Color(255, 255, 255, 180));
            String hpText = (int) boss.hp + " / " + (int) boss.maxHp;
            g2.drawString(hpText, bx + bw / 2 - g2.getFontMetrics().stringWidth(hpText) / 2, HEIGHT - 28);
        }

        drawPowerupLegend(g2);

        if (bossTransition) {
            g2.setColor(new Color(0, 0, 0, 170));
            g2.fillRect(0, 0, WIDTH, HEIGHT);
            boolean nextIsApex = (wave % 5 == 0 && wave != 10);
            g2.setColor(wave == 10 ? new Color(255, 200, 80) : nextIsApex ? new Color(255, 160, 0) : Color.YELLOW);
            g2.setFont(new Font("Arial", Font.BOLD, 46));
            String wt = "WAVE " + wave + "!";
            g2.drawString(wt, WIDTH / 2 - g2.getFontMetrics().stringWidth(wt) / 2, HEIGHT / 2 - 20);
            if (wave == 10) {
                g2.setFont(new Font("Arial", Font.BOLD, 22));
                g2.setColor(new Color(255, 180, 60));
                String kWarn = "🦊 KITSUNE INCOMING 🦊";
                g2.drawString(kWarn, WIDTH / 2 - g2.getFontMetrics().stringWidth(kWarn) / 2, HEIGHT / 2 + 20);
            } else if (nextIsApex) {
                g2.setFont(new Font("Arial", Font.BOLD, 22));
                g2.setColor(new Color(255, 80, 60));
                String apexWarn = "⚡ APEX BOSS INCOMING ⚡";
                g2.drawString(apexWarn, WIDTH / 2 - g2.getFontMetrics().stringWidth(apexWarn) / 2, HEIGHT / 2 + 20);
            }
            g2.setFont(new Font("Arial", Font.PLAIN, 20));
            g2.setColor(Color.WHITE);
            g2.drawString("Brace yourself...", WIDTH / 2 - 72, HEIGHT / 2 + (nextIsApex ? 54 : 40));
        }
    }

    private void drawHeatBar(Graphics2D g2) {
        int bW = 160, bH = 14, bX = WIDTH / 2 - bW / 2, bY = HEIGHT - 52;
        g2.setColor(new Color(10, 10, 30));
        g2.fillRoundRect(bX, bY, bW, bH, 7, 7);
        float hf = (float) heat / MAX_HEAT;
        Color bc;
        if (overheated) {
            int fl = (int) (Math.abs(Math.sin(frameCount * 0.18)) * 200) + 55;
            bc = new Color(fl, 0, 0);
        } else if (hf < 0.5f)
            bc = new Color((int) (hf * 2 * 255), 220, 0);
        else
            bc = new Color(255, (int) ((1f - (hf - 0.5f) * 2) * 180), 0);
        int fw = (int) (bW * hf);
        if (fw > 0) {
            g2.setColor(bc);
            g2.fillRoundRect(bX, bY, fw, bH, 7, 7);
        }
        g2.setStroke(new BasicStroke(overheated ? 2f : 1.2f));
        g2.setColor(overheated ? new Color(255, 80, 0) : new Color(80, 80, 140));
        g2.drawRoundRect(bX, bY, bW, bH, 7, 7);
        g2.setStroke(new BasicStroke(1));
        g2.setFont(new Font("Arial", Font.BOLD, 10));
        g2.setColor(overheated ? new Color(255, 100, 0) : new Color(160, 160, 220));
        String lbl = overheated ? "COOLING DOWN..." : "HEAT";
        g2.drawString(lbl, bX + bW / 2 - g2.getFontMetrics().stringWidth(lbl) / 2, bY - 2);
    }

    private void drawActivePowerupIcons(Graphics2D g2) {
        int px = 10, py = 36;
        // ── Boss drop powerups (timed, show draining bar) ─────────────
        if (doubleShot)
            px = drawHudIcon(g2, px, py, "2x", new Color(255, 200, 60), (float) doubleShotTimer / 480f, false);
        if (hasShield)
            px = drawHudIcon(g2, px, py, "SH", new Color(80, 180, 255), (float) shieldTimer / 600f, false);
        // ── Shop permanent upgrades (pulsing ★ bar) ────────────────────
        if (shopSpeedBoost)
            px = drawHudIcon(g2, px, py, "SPD*", SHOP_ACCENT[1], 1f, true);
        if (shopRapidFire)
            px = drawHudIcon(g2, px, py, "RF*", SHOP_ACCENT[2], 1f, true);
        if (shopBulletTime)
            px = drawHudIcon(g2, px, py, "BT*", SHOP_ACCENT[3], (float) bulletTimeTimer / 600f, false);
        if (shopScoreRush)
            px = drawHudIcon(g2, px, py, "SR*", SHOP_ACCENT[4], 1f, true);
        if (shopPhaseShift)
            px = drawHudIcon(g2, px, py, "PS*", SHOP_ACCENT[5], (float) ghostWalkTimer / 300f, false);
        if (voidMagnetReady)
            px = drawHudIcon(g2, px, py, "VM", SHOP_ACCENT[8], 1f, false);
        if (voidMagnetActive)
            px = drawHudIcon(g2, px, py, "VM!", SHOP_ACCENT[8], (float) voidMagnetTimer / 480f, false);
        // echo shot removed
        if (hasDeathMark)
            px = drawHudIcon(g2, px, py, "DM", SHOP_ACCENT[10], 1f, false);
    }

    private int drawHudIcon(Graphics2D g2, int px, int py,
            String label, Color c, float frac, boolean permanent) {
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 170));
        g2.fillRoundRect(px, py, 46, 26, 8, 8);
        if (permanent) {
            float pulse = 0.55f + 0.45f * (float) Math.sin(System.currentTimeMillis() * 0.004);
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (180 * pulse)));
            g2.fillRoundRect(px, py + 26, 46, 4, 2, 2);
            g2.setFont(new Font("Arial", Font.PLAIN, 7));
            g2.setColor(Color.WHITE);
            g2.drawString("∞", px + 19, py + 31);
        } else {
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 110));
            g2.fillRoundRect(px, py + 26, (int) (46 * Math.max(0, frac)), 4, 2, 2);
        }
        g2.setFont(new Font("Arial", Font.BOLD, label.length() > 3 ? 10 : 12));
        g2.setColor(Color.WHITE);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(label, px + 23 - fm.stringWidth(label) / 2, py + 18);
        return px + 52;
    }

    private void drawPowerupLegend(Graphics2D g2) {
        int lx = WIDTH - 150, ly = HEIGHT - 48;
        g2.setFont(new Font("Arial", Font.PLAIN, 9));
        g2.setColor(new Color(70, 70, 110));
        g2.drawString("BOSS DROP (timed): SH · 2x", lx, ly);
        g2.setColor(new Color(90, 160, 120));
        g2.drawString("SHOP: SPD RF BT SR PS ECH★  |  [Q]VM  [E]SNG  DM", lx, ly + 12);
    }

    private void drawPause(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, 0, WIDTH, HEIGHT);
        int px = WIDTH / 2 - 140, py = 150, pw = 280, ph = 300;
        g2.setColor(new Color(8, 8, 28, 245));
        g2.fillRoundRect(px, py, pw, ph, 18, 18);
        g2.setColor(new Color(0, 180, 255, 80));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(px, py, pw, ph, 18, 18);
        g2.setStroke(new BasicStroke(1));
        g2.setFont(new Font("Arial", Font.BOLD, 34));
        g2.setPaint(new GradientPaint(0, 170, new Color(80, 220, 255), 0, 210, new Color(120, 60, 255)));
        String title = "PAUSED";
        g2.drawString(title, WIDTH / 2 - g2.getFontMetrics().stringWidth(title) / 2, 205);
        g2.setColor(new Color(0, 160, 255, 60));
        g2.fillRect(WIDTH / 2 - 100, 215, 200, 1);
        if (!pauseConfirmQuit) {
            drawBtn(g2, btnPauseResume, "RESUME", true);
            drawBtn(g2, btnPauseSettings, "SETTINGS", true);
            drawBtn(g2, btnPauseQuit, "QUIT", true);

            g2.setFont(new Font("Arial", Font.PLAIN, 11));
            g2.setColor(new Color(80, 80, 130));
            String hint = "ESC to resume";
            g2.drawString(hint, WIDTH / 2 - g2.getFontMetrics().stringWidth(hint) / 2, py + ph + 18);
        } else {
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            g2.setColor(new Color(255, 100, 100));
            String q = "Quit this run?";
            g2.drawString(q, WIDTH / 2 - g2.getFontMetrics().stringWidth(q) / 2, 270);
            g2.setFont(new Font("Arial", Font.PLAIN, 11));
            g2.setColor(new Color(160, 160, 200));
            String sub = "Progress will be lost.";
            g2.drawString(sub, WIDTH / 2 - g2.getFontMetrics().stringWidth(sub) / 2, 292);
            drawBtn(g2, btnPauseConfirmYes, "YES, QUIT", true);
            drawBtn(g2, btnPauseConfirmNo, "KEEP PLAYING", true);
        }
    }

    private void drawWave10Choice(Graphics2D g2) {
        // Dark overlay
        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        // ── Main panel ───────────────────────────────────────────────
        int px = WIDTH / 2 - 170, py = 120, pw = 340, ph = 460;
        // Panel shadow
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRoundRect(px + 6, py + 6, pw, ph, 20, 20);
        // Panel body gradient
        GradientPaint pg = new GradientPaint(px, py, new Color(8, 10, 32), px, py + ph, new Color(4, 5, 18));
        g2.setPaint(pg);
        g2.fillRoundRect(px, py, pw, ph, 20, 20);

        // Gold top accent strip
        GradientPaint tg = new GradientPaint(px, py, new Color(255, 200, 60, 0), px + pw / 2, py,
                new Color(255, 210, 80, 220));
        g2.setPaint(tg);
        g2.fillRoundRect(px, py, pw, 6, 20, 20);
        g2.fillRect(px, py + 4, pw, 2);

        // Gold border
        g2.setPaint(null);
        g2.setColor(new Color(255, 200, 60, 90));
        g2.setStroke(new BasicStroke(1.6f));
        g2.drawRoundRect(px, py, pw, ph, 20, 20);
        g2.setStroke(new BasicStroke(1));

        // ── Trophy icon ───────────────────────────────────────────────
        int tcx = WIDTH / 2, tcy = py + 62;
        // Glow behind trophy
        g2.setColor(new Color(255, 200, 50, 18));
        g2.fillOval(tcx - 38, tcy - 38, 76, 76);

        // Gold gradient for trophy
        GradientPaint trG = new GradientPaint(tcx - 24, tcy - 28, new Color(255, 240, 120), tcx + 24, tcy + 30,
                new Color(200, 130, 20));
        g2.setPaint(trG);

        // Cup bowl — correct upper half (180 start, 180 sweep = top half)
        g2.fillArc(tcx - 24, tcy - 24, 48, 38, 180, 180);
        // Cup sides connecting bowl to stem
        g2.fillRect(tcx - 20, tcy + 14, 8, 8);
        g2.fillRect(tcx + 12, tcy + 14, 8, 8);
        // Stem
        g2.fillRect(tcx - 5, tcy + 22, 10, 12);
        // Base plate
        g2.fillRoundRect(tcx - 18, tcy + 34, 36, 7, 4, 4);

        // Handles — must reset paint before drawing
        g2.setPaint(null);
        g2.setColor(new Color(200, 140, 20, 230));
        g2.setStroke(new BasicStroke(4f));
        g2.drawArc(tcx - 34, tcy - 8, 14, 18, 90, -180); // left handle
        g2.drawArc(tcx + 20, tcy - 8, 14, 18, 90, 180); // right handle
        g2.setStroke(new BasicStroke(1));

        // Star on cup face
        g2.setPaint(null);
        g2.setColor(new Color(255, 255, 180, 200));
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        g2.drawString("★", tcx - 5, tcy + 10);

        // ── Title ─────────────────────────────────────────────────────
        g2.setFont(new Font("Arial", Font.BOLD, 28));
        GradientPaint titleG = new GradientPaint(0, py + 88, new Color(255, 230, 100), 0, py + 118,
                new Color(255, 150, 30));
        g2.setPaint(titleG);
        String t = "WAVE 10 CLEARED!";
        g2.drawString(t, WIDTH / 2 - g2.getFontMetrics().stringWidth(t) / 2, py + 110);

        // ── Divider ───────────────────────────────────────────────────
        g2.setPaint(null);
        g2.setColor(new Color(255, 200, 60, 40));
        g2.fillRect(px + 24, py + 118, pw - 48, 1);

        // ── Score block ───────────────────────────────────────────────
        boolean newRecord = score >= highScores[difficulty];
        // Score row
        g2.setFont(new Font("Arial", Font.PLAIN, 13));
        g2.setColor(new Color(140, 160, 210));
        g2.drawString("SCORE", px + 36, py + 148);
        g2.setFont(new Font("Courier New", Font.BOLD, 22));
        g2.setColor(Color.WHITE);
        g2.drawString(String.valueOf(score), px + pw - 36 - g2.getFontMetrics().stringWidth(String.valueOf(score)),
                py + 148);

        // High score row
        g2.setFont(new Font("Arial", Font.PLAIN, 13));
        String[] dnames = { "EASY", "NORMAL", "HARD" };
        g2.setColor(new Color(140, 160, 210));
        g2.drawString("BEST  (" + dnames[difficulty] + ")", px + 36, py + 174);
        g2.setFont(new Font("Courier New", Font.BOLD, 22));
        g2.setColor(newRecord ? new Color(255, 220, 60) : new Color(160, 160, 200));
        g2.drawString(String.valueOf(highScores[difficulty]),
                px + pw - 36 - g2.getFontMetrics().stringWidth(String.valueOf(highScores[difficulty])), py + 174);

        // NEW RECORD badge — large centered banner
        if (newRecord) {
            float pulse = (float) (0.55 + 0.45 * Math.sin(frameCount * 0.11));
            int bdW = 200, bdH = 30, bdX = WIDTH / 2 - bdW / 2, bdY = py + 180;
            // Outer glow
            g2.setColor(new Color(255, 220, 50, (int) (40 * pulse)));
            g2.fillRoundRect(bdX - 6, bdY - 6, bdW + 12, bdH + 12, 14, 14);
            // Badge fill
            GradientPaint bdG = new GradientPaint(bdX, bdY, new Color(255, 210, 40), bdX, bdY + bdH,
                    new Color(220, 140, 0));
            g2.setPaint(bdG);
            g2.fillRoundRect(bdX, bdY, bdW, bdH, 10, 10);
            // Shine
            g2.setColor(new Color(255, 255, 255, (int) (70 * pulse)));
            g2.fillRoundRect(bdX + 2, bdY + 2, bdW - 4, bdH / 2 - 2, 8, 8);
            // Border
            g2.setPaint(null);
            g2.setColor(new Color(255, 255, 180, (int) (200 * pulse)));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(bdX, bdY, bdW, bdH, 10, 10);
            g2.setStroke(new BasicStroke(1));
            // Text
            g2.setFont(new Font("Arial", Font.BOLD, 15));
            g2.setColor(new Color(30, 10, 0));
            String rec = "* NEW RECORD! *";
            FontMetrics rfm = g2.getFontMetrics();
            g2.drawString(rec, WIDTH / 2 - rfm.stringWidth(rec) / 2, bdY + bdH / 2 + 6);
        }

        // ── Divider ───────────────────────────────────────────────────
        g2.setColor(new Color(255, 200, 60, 30));
        g2.fillRect(px + 24, py + 206, pw - 48, 1);

        // ── Endless mode description ──────────────────────────────────
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        g2.setColor(new Color(180, 230, 255));
        String q = "Continue in Endless Mode?";
        g2.drawString(q, WIDTH / 2 - g2.getFontMetrics().stringWidth(q) / 2, py + 236);

        g2.setFont(new Font("Arial", Font.PLAIN, 11));
        g2.setColor(new Color(100, 120, 170));
        String[] lines = {
                "Bosses grow stronger with every wave.",
                "No checkpoints. Score keeps climbing.",
                "Your record will be saved when you stop."
        };
        int ly = py + 256;
        for (String ln : lines) {
            g2.drawString(ln, WIDTH / 2 - g2.getFontMetrics().stringWidth(ln) / 2, ly);
            ly += 16;
        }

        // ── Buttons ───────────────────────────────────────────────────
        // KEEP GOING — green accent
        int b1x = px + 20, b1y = py + 320, b1w = pw - 40, b1h = 52;
        g2.setColor(new Color(0, 180, 80, 30));
        g2.fillRoundRect(b1x - 3, b1y - 3, b1w + 6, b1h + 6, 14, 14);
        GradientPaint b1g = new GradientPaint(b1x, b1y, new Color(0, 140, 60), b1x, b1y + b1h, new Color(0, 90, 40));
        g2.setPaint(b1g);
        g2.fillRoundRect(b1x, b1y, b1w, b1h, 12, 12);
        g2.setPaint(null);
        g2.setColor(new Color(0, 255, 120, 180));
        g2.setStroke(new BasicStroke(1.6f));
        g2.drawRoundRect(b1x, b1y, b1w, b1h, 12, 12);
        g2.setStroke(new BasicStroke(1));
        // Shine
        g2.setColor(new Color(255, 255, 255, 25));
        g2.fillRoundRect(b1x + 2, b1y + 2, b1w - 4, b1h / 2 - 2, 10, 10);
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.setColor(Color.WHITE);
        String kg = ">> KEEP GOING -- ENDLESS";
        g2.drawString(kg, WIDTH / 2 - g2.getFontMetrics().stringWidth(kg) / 2, b1y + b1h / 2 + 6);
        btnContinueEndless.setBounds(b1x, b1y, b1w, b1h);

        // END RUN — subtle red/dark
        int b2x = px + 20, b2y = b1y + b1h + 12, b2w = pw - 40, b2h = 44;
        g2.setColor(new Color(10, 10, 30));
        g2.fillRoundRect(b2x, b2y, b2w, b2h, 10, 10);
        g2.setColor(new Color(180, 60, 60, 120));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(b2x, b2y, b2w, b2h, 10, 10);
        g2.setStroke(new BasicStroke(1));
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        g2.setColor(new Color(200, 120, 120));
        String er = "X  END RUN  &  SAVE SCORE";
        g2.drawString(er, WIDTH / 2 - g2.getFontMetrics().stringWidth(er) / 2, b2y + b2h / 2 + 5);
        btnEndRun.setBounds(b2x, b2y, b2w, b2h);
    }

    private void drawGameOver(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 190));
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        // ── Panel ────────────────────────────────────────────────────
        int px = WIDTH / 2 - 160, py = 130, pw = 320, ph = 430;
        g2.setColor(new Color(0, 0, 0, 100));
        g2.fillRoundRect(px + 5, py + 5, pw, ph, 18, 18);
        GradientPaint pg = new GradientPaint(px, py, new Color(22, 4, 4), px, py + ph, new Color(8, 2, 2));
        g2.setPaint(pg);
        g2.fillRoundRect(px, py, pw, ph, 18, 18);
        // Red top strip
        g2.setPaint(new GradientPaint(px, py, new Color(200, 0, 0, 0), px + pw / 2, py, new Color(220, 30, 30, 200)));
        g2.fillRoundRect(px, py, pw, 6, 18, 18);
        g2.fillRect(px, py + 4, pw, 2);
        g2.setPaint(null);
        g2.setColor(new Color(200, 40, 40, 80));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(px, py, pw, ph, 18, 18);
        g2.setStroke(new BasicStroke(1));

        // ── GAME OVER title ───────────────────────────────────────────
        g2.setFont(new Font("Arial", Font.BOLD, 46));
        // Red glow behind
        g2.setColor(new Color(255, 0, 0, 30));
        String go2 = "GAME OVER";
        int gx = WIDTH / 2 - g2.getFontMetrics().stringWidth(go2) / 2;
        for (int gl = 6; gl >= 1; gl--) {
            g2.setColor(new Color(220, 0, 0, 8 * gl));
            g2.drawString(go2, gx, py + 68);
        }
        g2.setColor(new Color(230, 40, 40));
        g2.drawString(go2, gx, py + 68);
        // White shimmer on top half of letters
        g2.setColor(new Color(255, 180, 180, 40));
        g2.drawString(go2, gx, py + 64);

        // ── Divider ───────────────────────────────────────────────────
        g2.setColor(new Color(200, 40, 40, 40));
        g2.fillRect(px + 24, py + 80, pw - 48, 1);

        // ── Stats rows ────────────────────────────────────────────────
        String[] labels = { "FINAL SCORE", "BEST SCORE", "DIFFICULTY", "WAVE REACHED" };
        String[] dnames = { "Easy", "Normal", "Hard" };
        boolean newRec = score > 0 && score >= highScores[difficulty];
        String[] values = {
                String.valueOf(score),
                String.valueOf(highScores[difficulty]),
                dnames[difficulty],
                String.valueOf(wave - 1)
        };
        Color[] vcols = {
                Color.WHITE,
                newRec ? new Color(255, 220, 60) : new Color(160, 160, 200),
                new Color[] { new Color(60, 220, 80), new Color(0, 180, 255), new Color(255, 80, 60) }[difficulty],
                new Color(180, 200, 255)
        };
        int rowY = py + 112;
        for (int i = 0; i < labels.length; i++) {
            // Label
            g2.setFont(new Font("Arial", Font.PLAIN, 11));
            g2.setColor(new Color(120, 130, 180));
            g2.drawString(labels[i], px + 28, rowY);
            // Value right-aligned
            g2.setFont(new Font("Courier New", Font.BOLD, 18));
            g2.setColor(vcols[i]);
            FontMetrics vfm = g2.getFontMetrics();
            g2.drawString(values[i], px + pw - 28 - vfm.stringWidth(values[i]), rowY);
            // Row separator
            g2.setColor(new Color(255, 255, 255, 10));
            g2.fillRect(px + 20, rowY + 6, pw - 40, 1);
            rowY += 46;
            // NEW RECORD badge under best score row — big and centered
            if (i == 1 && newRec) {
                float pulse = (float) (0.55 + 0.45 * Math.sin(frameCount * 0.11));
                int bdW = 190, bdH = 28, bdX = WIDTH / 2 - bdW / 2, bdY = rowY - 40;
                // Glow
                g2.setColor(new Color(255, 215, 0, (int) (35 * pulse)));
                g2.fillRoundRect(bdX - 5, bdY - 5, bdW + 10, bdH + 10, 12, 12);
                // Fill
                GradientPaint bdG2 = new GradientPaint(bdX, bdY, new Color(255, 210, 40), bdX, bdY + bdH,
                        new Color(210, 130, 0));
                g2.setPaint(bdG2);
                g2.fillRoundRect(bdX, bdY, bdW, bdH, 8, 8);
                // Shine
                g2.setColor(new Color(255, 255, 255, (int) (65 * pulse)));
                g2.fillRoundRect(bdX + 2, bdY + 2, bdW - 4, bdH / 2 - 2, 6, 6);
                // Border
                g2.setPaint(null);
                g2.setColor(new Color(255, 255, 180, (int) (190 * pulse)));
                g2.setStroke(new BasicStroke(1.4f));
                g2.drawRoundRect(bdX, bdY, bdW, bdH, 8, 8);
                g2.setStroke(new BasicStroke(1));
                // Text
                g2.setFont(new Font("Arial", Font.BOLD, 14));
                g2.setColor(new Color(30, 10, 0));
                String rec2 = "★  NEW RECORD!  ★";
                FontMetrics rfm2 = g2.getFontMetrics();
                g2.drawString(rec2, WIDTH / 2 - rfm2.stringWidth(rec2) / 2, bdY + bdH / 2 + 5);
            }
        }

        // ── Divider ───────────────────────────────────────────────────
        g2.setColor(new Color(200, 40, 40, 30));
        g2.fillRect(px + 24, rowY - 10, pw - 48, 1);

        // ── Action hints ──────────────────────────────────────────────
        int hy = rowY + 14;
        // R to restart
        g2.setColor(new Color(15, 15, 40));
        g2.fillRoundRect(px + 20, hy, pw - 40, 40, 8, 8);
        g2.setColor(new Color(0, 180, 255, 80));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(px + 20, hy, pw - 40, 40, 8, 8);
        g2.setStroke(new BasicStroke(1));
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        g2.setColor(Color.WHITE);
        g2.drawString("[ R ]  Play Again", WIDTH / 2 - g2.getFontMetrics().stringWidth("[ R ]  Play Again") / 2,
                hy + 26);

        hy += 50;
        g2.setColor(new Color(10, 10, 28));
        g2.fillRoundRect(px + 20, hy, pw - 40, 40, 8, 8);
        g2.setColor(new Color(100, 100, 160, 60));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(px + 20, hy, pw - 40, 40, 8, 8);
        g2.setStroke(new BasicStroke(1));
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        g2.setColor(new Color(180, 180, 220));
        g2.drawString("[ M ]  Main Menu", WIDTH / 2 - g2.getFontMetrics().stringWidth("[ M ]  Main Menu") / 2, hy + 26);
    }

    private void drawBtn(Graphics2D g2, Rectangle r, String label, boolean active) {
        if (active) {
            g2.setColor(new Color(0, 150, 255, 25));
            g2.fillRoundRect(r.x - 4, r.y - 4, r.width + 8, r.height + 8, 18, 18);
        }
        g2.setColor(active ? new Color(20, 25, 70) : new Color(12, 12, 30));
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 12, 12);
        g2.setColor(active ? new Color(0, 180, 255) : new Color(60, 60, 110));
        g2.setStroke(new BasicStroke(active ? 2f : 1f));
        g2.drawRoundRect(r.x, r.y, r.width, r.height, 12, 12);
        g2.setStroke(new BasicStroke(1));
        g2.setFont(new Font("Arial", Font.BOLD, 17));
        g2.setColor(active ? Color.WHITE : new Color(100, 100, 160));
        FontMetrics fm = g2.getFontMetrics();
        int tx = r.x + r.width / 2 - fm.stringWidth(label) / 2;
        int ty = r.y + r.height / 2 + 6;
        g2.drawString(label, tx, ty);
        // icon drawn 18px left of text, vertically centered
        int ix = tx - 20;
        int iy = r.y + r.height / 2;
        if (label.equals("RESUME") || label.equals("START")) {
            g2.setColor(new Color(0, 210, 255, 200));
            g2.fillPolygon(new int[] { ix, ix, ix + 11 }, new int[] { iy - 9, iy + 9, iy }, 3);
        } else if (label.equals("SETTINGS")) {
            g2.setColor(new Color(0, 210, 255, 200));
            g2.setStroke(new BasicStroke(1.8f));
            g2.drawOval(ix - 5, iy - 5, 10, 10);
            for (int i = 0; i < 8; i++) {
                double a = Math.toRadians(i * 45);
                g2.drawLine((int) (ix + Math.cos(a) * 6), (int) (iy + Math.sin(a) * 6),
                        (int) (ix + Math.cos(a) * 10), (int) (iy + Math.sin(a) * 10));
            }
            g2.setStroke(new BasicStroke(1));
        } else if (label.equals("QUIT")) {
            g2.setColor(new Color(255, 80, 80, 200));
            g2.setStroke(new BasicStroke(2.2f));
            g2.drawLine(ix - 6, iy - 6, ix + 6, iy + 6);
            g2.drawLine(ix + 6, iy - 6, ix - 6, iy + 6);
            g2.setStroke(new BasicStroke(1));
        } else if (label.equals("MOUSE CLICK")) {
            g2.setColor(new Color(0, 210, 255, 200));
            g2.setStroke(new BasicStroke(1.5f));
            // mouse body
            g2.drawRoundRect(ix - 6, iy - 9, 12, 16, 5, 5);
            // middle line
            g2.drawLine(ix, iy - 9, ix, iy - 3);
            // click dot
            g2.fillOval(ix - 2, iy - 6, 4, 4);
            g2.setStroke(new BasicStroke(1));
        } else if (label.equals("SPACEBAR")) {
            g2.setColor(new Color(0, 210, 255, 200));
            g2.setStroke(new BasicStroke(1.8f));
            // key outline
            g2.drawRoundRect(ix - 9, iy - 6, 18, 12, 4, 4);
            // spacebar bottom line
            g2.drawLine(ix - 5, iy + 2, ix + 5, iy + 2);
            g2.setStroke(new BasicStroke(1));
        } else if (label.startsWith("MUSIC")) {
            boolean on = label.contains("ON");
            g2.setColor(on ? new Color(0, 210, 255, 200) : new Color(150, 150, 180, 150));
            g2.setStroke(new BasicStroke(1.8f));
            // note stem
            g2.drawLine(ix + 4, iy - 8, ix + 4, iy + 2);
            // note head
            g2.fillOval(ix - 2, iy, 7, 6);
            // note flag
            g2.drawLine(ix + 4, iy - 8, ix + 10, iy - 5);
            if (!on) {
                // strike-through for OFF
                g2.setColor(new Color(255, 80, 80, 180));
                g2.drawLine(ix - 8, iy + 6, ix + 10, iy - 10);
            }
            g2.setStroke(new BasicStroke(1));
        }
    }

    private void centeredTitle(Graphics2D g2, String text, int y) {
        g2.setFont(new Font("Arial", Font.BOLD, 36));
        g2.setColor(Color.CYAN);
        g2.drawString(text, WIDTH / 2 - g2.getFontMetrics().stringWidth(text) / 2, y);
    }

    private void divider(Graphics2D g2, int y) {
        g2.setColor(new Color(0, 160, 255, 60));
        g2.fillRect(WIDTH / 2 - 130, y, 260, 2);
    }

    private void sectionLabel(Graphics2D g2, String label, int y) {
        g2.setFont(new Font("Arial", Font.BOLD, 13));
        g2.setColor(new Color(100, 180, 255));
        g2.drawString(label, WIDTH / 2 - 120, y);
        // small accent line under label
        g2.setColor(new Color(0, 180, 255, 60));
        g2.fillRect(WIDTH / 2 - 120, y + 3, g2.getFontMetrics().stringWidth(label), 1);
    }

    private void drawMiniShip(Graphics2D g2, int x, int y) {
        g2.setColor(Color.CYAN);
        g2.fillPolygon(new int[] { x + 7, x, x + 14 }, new int[] { y, y + 14, y + 14 }, 3);
    }

    // =================================================================
    // GAME MANAGEMENT
    // =================================================================
    private void startGame() {
        score = 0;
        wave = 1;
        frameCount = 0;
        bossTransition = false;
        heat = 0;
        overheated = false;
        overheatTimer = 0;
        novaCharging = false;
        novaChargeTimer = 0;
        novaLaserActive = false;
        novaLaserTimer = 0;
        novaCooldownTimer = 0;
        novaParticles.clear();
        hasShield = false;
        shieldTimer = 0;
        doubleShot = false;
        doubleShotTimer = 0;
        pickupMsg = "";
        pickupTimer = 0;
        shakeTimer = 0;
        shakeIntensity = 0;
        soundCooldown = 0;
        player = new Player(WIDTH / 2 - 15, HEIGHT - 100);
        player.lives = new int[] { 5, 3, 2 }[difficulty];
        boss = new Boss(WIDTH / 2 - 40, -80, wave);
        playerBullets.clear();
        enemyBullets.clear();
        powerUps.clear();
        snakes.clear();
        explosionParticles.clear();
        setScene(SCENE_SPACE);
        shopSpeedBoost = false;
        shopRapidFire = false;
        shopBulletTime = false;
        bulletTimeTimer = 0;
        shopScoreRush = false;
        shopPhaseShift = false;
        voidMagnetReady = false;
        voidMagnetActive = false;
        voidMagnetTimer = 0;
        shopEchoShot = false;
        echoShotCD = 0;
        hasDeathMark = false;
        powerUpDropCD = 0;
        for (int i = 0; i < SHOP_OFFERED; i++)
            shopBought[i] = false;
        phantomDashCD = 0;
        phantomInvinc = false;
        phantomInvincT = 0;
        phantomDecoyX = -999;
        phantomDecoyY = -999;
        phantomDecoyT = 0;
        phantomAfterT = 0;
        viperFireCD = 0;
        viperHitCount = 0;
        viperPoisonStacks = 0;
        viperPoisonTimer = 0;
        viperPoisonTickTimer = 0;
        gameState = STATE_PLAYING;
    }

    // =================================================================
    // INPUT
    // =================================================================
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() < 256)
            keys[e.getKeyCode()] = true;

        if (gameState == STATE_GAME_OVER) {
            if (e.getKeyCode() == KeyEvent.VK_R)
                startGame();
            if (e.getKeyCode() == KeyEvent.VK_M) {
                enemyBullets.clear();
                playerBullets.clear();
                powerUps.clear();
                gameState = STATE_MENU;
            }
        }
        if (gameState == STATE_PLAYING && e.getKeyCode() == KeyEvent.VK_Q) {
            if (voidMagnetReady) {
                voidMagnetReady = false;
                voidMagnetActive = true;
                voidMagnetTimer = 480;
                pickupMsg = "VOID MAGNET ACTIVE!";
                pickupTimer = 90;
            }
        }
        if (gameState == STATE_PLAYING && e.getKeyCode() == KeyEvent.VK_E) {
            if (hasSingularity) {
                hasSingularity = false;
                enemyBullets.clear();
                pickupMsg = "SINGULARITY — BULLETS CLEARED!";
                pickupTimer = 90;
            }
        }
        if (gameState == STATE_PLAYING && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            gameState = STATE_PAUSE;
        } else if (gameState == STATE_PAUSE && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            pauseConfirmQuit = false;
            gameState = STATE_PLAYING;
        }
        if (gameState == STATE_PAUSE && e.getKeyCode() == KeyEvent.VK_M) {
            enemyBullets.clear();
            playerBullets.clear();
            powerUps.clear();
            gameState = STATE_MENU;
        }
        if (gameState == STATE_CLASS_SEL && e.getKeyCode() == KeyEvent.VK_ENTER)
            startGame();
        if (gameState == STATE_SHOP && e.getKeyCode() == KeyEvent.VK_ENTER)
            leaveShop();
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() < 256)
            keys[e.getKeyCode()] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        Point p = e.getPoint();
        if (gameState == STATE_GAME_OVER) {
            int px = WIDTH / 2 - 160, pw = 320;
            // calculate hy same as drawGameOver — approximate button Y positions
            if (p.x >= px + 20 && p.x <= px + pw - 20) {
                if (p.y >= 440 && p.y <= 480)
                    startGame();
                else if (p.y >= 490 && p.y <= 530) {
                    enemyBullets.clear();
                    playerBullets.clear();
                    powerUps.clear();
                    gameState = STATE_MENU;
                    return;
                }
            }
        } else if (gameState == STATE_MENU) {
            if (btnStart.contains(p))
                gameState = STATE_DIFF_SEL;
            else if (btnSettings.contains(p))
                gameState = STATE_SETTINGS;
            else if (btnQuit.contains(p))
                System.exit(0);
        } else if (gameState == STATE_SETTINGS) {
            if (btnFireMouse.contains(p))
                fireMode = FIRE_MOUSE;
            else if (btnFireSpace.contains(p))
                fireMode = FIRE_SPACE;
            else if (btnMusicToggle.contains(p)) {
                musicEnabled = !musicEnabled;
                if (sequencer != null) {
                    if (musicEnabled)
                        sequencer.start();
                    else
                        sequencer.stop();
                }
            } else if (btnSettBack.contains(p)) {
                if (pauseInSettings) {
                    pauseInSettings = false;
                } else
                    gameState = STATE_MENU;
            }
        } else if (gameState == STATE_PAUSE) {
            if (!pauseConfirmQuit) {
                if (btnPauseResume.contains(p)) {
                    pauseConfirmQuit = false;
                    gameState = STATE_PLAYING;
                } else if (btnPauseSettings.contains(p)) {
                    pauseInSettings = true;
                    gameState = STATE_SETTINGS;
                } else if (btnPauseQuit.contains(p)) {
                    pauseConfirmQuit = true;
                }
            } else {
                if (btnPauseConfirmYes.contains(p)) {
                    enemyBullets.clear();
                    playerBullets.clear();
                    powerUps.clear();
                    pauseInSettings = false;
                    pauseConfirmQuit = false;
                    if (score > highScores[difficulty]) {
                        highScores[difficulty] = score;
                        prefs.putInt(new String[] { "hs_easy", "hs_normal", "hs_hard" }[difficulty],
                                highScores[difficulty]);
                    }
                    gameState = STATE_GAME_OVER;
                } else if (btnPauseConfirmNo.contains(p)) {
                    pauseConfirmQuit = false;
                }
            }
        } else if (gameState == STATE_DIFF_SEL) {
            if (btnDiffEasy.contains(p)) {
                difficulty = 0;
                gameState = STATE_CLASS_SEL;
            } else if (btnDiffNormal.contains(p)) {
                difficulty = 1;
                gameState = STATE_CLASS_SEL;
            } else if (btnDiffHard.contains(p)) {
                difficulty = 2;
                gameState = STATE_CLASS_SEL;
            } else if (btnDiffBack.contains(p))
                gameState = STATE_MENU;
        } else if (gameState == STATE_CLASS_SEL) {
            for (int ci = 0; ci < CLASS_COUNT; ci++) {
                if (btnClass[ci].contains(p)) {
                    if (selectedClass == ci)
                        startGame();
                    else
                        selectedClass = ci;
                    return;
                }
            }
            if (btnClassBack.contains(p))
                gameState = STATE_DIFF_SEL;
        } else if (gameState == STATE_SHOP) {
            for (int i = 0; i < SHOP_OFFERED; i++)
                if (btnShopItems[i].contains(p)) {
                    buyShopItem(i);
                    return;
                }
            if (btnShopContinue.contains(p))
                leaveShop();
        } else if (gameState == STATE_WAVE10_CHOICE) {
            if (btnContinueEndless.contains(p)) {
                leaveShop();
            } else if (btnEndRun.contains(p)) {
                if (score > highScores[difficulty]) {
                    highScores[difficulty] = score;
                    prefs.putInt(new String[] { "hs_easy", "hs_normal", "hs_hard" }[difficulty],
                            highScores[difficulty]);
                }
                gameState = STATE_GAME_OVER;
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            if (gameState == STATE_PLAYING)
                mouseFireHeld = true;
            if (gameState == STATE_SETTINGS && sliderTrack.contains(e.getPoint()))
                draggingSlider = true;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            mouseFireHeld = false;
            draggingSlider = false;
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        if (draggingSlider && gameState == STATE_SETTINGS) {
            int relX = e.getX() - sliderTrack.x;
            musicVolPct = Math.max(0, Math.min(100, relX * 100 / sliderTrack.width));
            applyMusicVolume();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    // =================================================================
    // POWERUP HELPERS
    // =================================================================
    static Color puColor(int type) {
        switch (type) {
            case PU_DOUBLE_SHOT:
                return new Color(0, 220, 255);
            case PU_SHIELD:
                return new Color(80, 120, 255);
            case PU_SPEED_BOOST:
                return new Color(255, 200, 0);
            case PU_SCORE_BURST:
                return new Color(255, 140, 0);
            case PU_HEAL:
                return new Color(60, 220, 80);
            default:
                return Color.WHITE;
        }
    }

    static String puLabel(int type) {
        switch (type) {
            case PU_DOUBLE_SHOT:
                return "2x";
            case PU_SHIELD:
                return "SH";
            case PU_SPEED_BOOST:
                return "SPD";
            case PU_SCORE_BURST:
                return "+500";
            case PU_HEAL:
                return "HP";
            default:
                return "?";
        }
    }

    static String puFullName(int type) {
        switch (type) {
            case PU_DOUBLE_SHOT:
                return "DOUBLE";
            case PU_SHIELD:
                return "SHIELD";
            default:
                return "???";
        }
    }

    // =================================================================
    // INNER CLASSES
    // =================================================================
    class Player {
        int x, y, size = 30, speed = 5, lives = 3;
        boolean alive = true;

        Player(int x, int y) {
            this.x = x;
            this.y = y;
        }

        Rectangle getHitbox() {
            return new Rectangle(x + 9, y + 9, size - 18, size - 9);
        }

        void draw(Graphics2D g2) {
            if (selectedClass == CLASS_NOVA) {
                int glowA = novaCharging ? (int) (80 + 120 * Math.abs(Math.sin(frameCount * 0.2))) : 120;
                g2.setColor(new Color(80, 40, 180, glowA));
                g2.fillOval(x + size / 2 - 6, y + size - 4, 12, 10);
                g2.setColor(new Color(160, 100, 255));
                g2.fillPolygon(new int[] { x + size / 2, x + 4, x + size - 4 }, new int[] { y, y + size, y + size }, 3);
                g2.setColor(new Color(220, 200, 255));
                g2.fillOval(x + size / 2 - 4, y + 7, 8, 8);
            } else {
                int glowA = firingThisFrame ? (int) (120 + 80 * Math.abs(Math.sin(frameCount * 0.5))) : 120;
                g2.setColor(new Color(0, 120, 255, glowA));
                g2.fillOval(x + size / 2 - 6, y + size - 4, 12, 10);
                Color shipColor;
                switch (selectedClass) {
                    case CLASS_PHANTOM:
                        shipColor = new Color(180, 0, 255);
                        break;

                    case CLASS_VIPER:
                        shipColor = new Color(0, 255, 100);
                        break;
                    default:
                        shipColor = Color.CYAN;
                        break;
                }
                g2.setColor(shipColor);
                g2.fillPolygon(new int[] { x + size / 2, x, x + size }, new int[] { y, y + size, y + size }, 3);
                g2.setColor(new Color(200, 255, 255));
                g2.fillOval(x + size / 2 - 4, y + 7, 8, 8);
                if (overheated) {
                    g2.setColor(new Color(255, 80, 0, (int) (80 * Math.abs(Math.sin(frameCount * 0.18)))));
                    g2.fillOval(x, y, size, size);
                }
            }
        }
    }

    // =================================================================
    // BOSS CLASS
    // =================================================================
    class Boss {
        int x, y, width = 80, height = 50;
        double hp, maxHp;
        boolean alive = true;
        boolean isApex;
        static final int PHASE_PATROL = 0, PHASE_CIRCLE = 1, PHASE_DIVE = 2, PHASE_ZIGZAG = 3, PHASE_CHARGE = 4;
        int phase = PHASE_PATROL, phaseTimer = 0, phaseDuration = 180;
        double bx, by, vx = 2, vy = 0, circleAngle = 0;
        int diveTargetX, diveTargetY, zigDir = 1;
        float pulsePhase = 0;
        int waveNum;
        static final int LASER_NONE = 0, LASER_TELEGRAPH = 1, LASER_SWEEP = 2, LASER_TRACKING = 3, LASER_CHANNELING = 4,
                LASER_PERSISTENT = 5;
        int laserState = LASER_NONE, laserTimer = 0;
        boolean laserActive = false;
        double beamAngle = Math.PI / 2, sweepDir = 1;
        int channelingBeamWidth = 2, laserCooldown = 0, laserInterval, apexLaserCycle = 0;
        int kitsuneRestTimer = 0;
        boolean kitsuneResting = false;

        Boss(int bx2, int by2, int wave) {
            this.bx = bx2;
            this.by = by2;
            this.x = (int) bx;
            this.y = (int) by;
            this.waveNum = wave;
            this.isApex = (wave % 5 == 0 && wave != 10);
            maxHp = hp = isApex ? (10 + wave * 20) * 1.1 : (waveNum == 10 ? 150 : Math.min(125, 10 + wave * 20));
            laserInterval = isApex ? Math.max(120, 260 - wave * 8) : 999999;
            laserCooldown = isApex ? laserInterval / 2 : 999999;
        }

        Rectangle getBounds() {
            return new Rectangle((int) bx, (int) by, width, height);
        }

        String getBossName() {
            if (isApex) {
                String[] n = { "APEX DREADNOUGHT", "APEX ANNIHILATOR", "APEX NEMESIS", "APEX OBLITERATOR",
                        "APEX GOD-SLAYER" };
                return n[Math.min(waveNum / 5 - 1, n.length - 1)] + "  HP";
            }
            String[] n = { "VOID SCOUT", "REAPER MK-II", "CRIMSON TITAN", "OMEGA CORE", "HELL'S EYE",
                    "BAKENEKO", "NEKOMATA", "TENGU", "TANUKI", "KITSUNE" };
            return n[Math.min(waveNum - 1, n.length - 1)] + "  HP";
        }

        int originX() {
            return (int) bx + width / 2;
        }

        int originY() {
            return (int) by + height;
        }

        double angleToPlayer(Player player) {
            int ox = originX(), oy = originY(), px = player.x + player.size / 2, py = player.y + player.size / 2;
            double dx = px - ox, dy = py - oy;
            if (dy < 0)
                dy = 10;
            double angle = Math.atan2(dy, dx);
            return Math.max(Math.PI * 0.05, Math.min(Math.PI * 0.95, angle));
        }

        int beamEndX() {
            return originX() + (int) (Math.cos(beamAngle) * 1200);
        }

        int beamEndY() {
            return originY() + (int) (Math.sin(beamAngle) * 1200);
        }

        void update(int frame, Player player) {
            if (!alive)
                return;
            pulsePhase += isApex ? 0.08f : 0.06f;
            phaseTimer++;
            int effectivePhaseDuration = isApex ? (int) (phaseDuration * 0.75) : phaseDuration;
            if (phaseTimer >= effectivePhaseDuration) {
                phaseTimer = 0;
                int maxPhase = Math.min(waveNum + 1, 5);
                phase = (int) (Math.random() * maxPhase);
                phaseDuration = 120 + rand.nextInt(120);
                if (phase == PHASE_DIVE) {
                    diveTargetX = player.x + player.size / 2 - width / 2;
                    diveTargetY = Math.min(HEIGHT / 2, player.y - 60);
                }
                if (phase == PHASE_ZIGZAG)
                    zigDir = rand.nextBoolean() ? 1 : -1;
                if (phase == PHASE_CHARGE) {
                    vx = rand.nextBoolean() ? (isApex ? 8 : 7) : (isApex ? -8 : -7);
                    vy = 0;
                }
                if (phase == PHASE_CIRCLE)
                    circleAngle = Math.atan2(by - HEIGHT / 3.0, bx - WIDTH / 2.0);
            }
            if (waveNum == 10) {
                kitsuneRestTimer++;
                if (!kitsuneResting && kitsuneRestTimer >= 420) { // 7 seconds (60fps x 7)
                    kitsuneResting = true;
                    kitsuneRestTimer = 0;
                } else if (kitsuneResting && kitsuneRestTimer >= 420) { // 7 seconds rest
                    kitsuneResting = false;
                    kitsuneRestTimer = 0;
                }
            }
            double speedMult = isApex ? 1.4
                    : waveNum == 10 ? (kitsuneResting ? 0.08 : 0.5) : Math.min(1.0, 0.6 + waveNum * 0.04);
            switch (phase) {
                case PHASE_PATROL:
                    bx += vx * speedMult;
                    by += (Math.sin(frame * 0.025) * 0.7 * speedMult);
                    if (bx <= 10 || bx + width >= WIDTH - 10)
                        vx *= -1;
                    break;
                case PHASE_CIRCLE:
                    circleAngle += (0.022 + waveNum * 0.003) * speedMult;
                    double cr = 160 + 30 * Math.sin(frame * 0.015);
                    bx = WIDTH / 2.0 - width / 2.0 + Math.cos(circleAngle) * cr;
                    by = 160 + Math.sin(circleAngle) * 70;
                    break;
                case PHASE_DIVE:
                    double tdx = diveTargetX - bx, tdy = diveTargetY - by, tlen = Math.sqrt(tdx * tdx + tdy * tdy);
                    if (tlen > 3) {
                        bx += tdx / tlen * (3.5 + waveNum * 0.4) * speedMult;
                        by += tdy / tlen * (3.5 + waveNum * 0.4) * speedMult;
                    } else {
                        phase = PHASE_PATROL;
                        phaseTimer = 0;
                        phaseDuration = 120;
                    }
                    break;
                case PHASE_ZIGZAG:
                    bx += zigDir * (3.5 + waveNum * 0.3) * speedMult;
                    by += Math.sin(frame * 0.08) * 1.5 * speedMult;
                    if (bx <= 10 || bx + width >= WIDTH - 10)
                        zigDir *= -1;
                    break;
                case PHASE_CHARGE:
                    bx += vx * speedMult;
                    if (bx <= 5) {
                        bx = 5;
                        vx = Math.abs(vx);
                    }
                    if (bx + width >= WIDTH - 5) {
                        bx = WIDTH - 5 - width;
                        vx = -Math.abs(vx);
                    }
                    break;
            }
            if (laserState != LASER_NONE) {
                bx = WIDTH / 2.0 - width / 2.0;
                by = 35;
            } else {
                bx = Math.max(5, Math.min(WIDTH - 5 - width, bx));
                by = waveNum == 10 ? Math.max(20, Math.min(80, by)) : Math.max(20, Math.min(HEIGHT / 2.5, by));
            }
            x = (int) bx;
            y = (int) by;
            if (!isApex)
                return;
            if (laserState == LASER_NONE) {
                laserCooldown--;
                if (laserCooldown <= 0) {
                    laserCooldown = laserInterval + rand.nextInt(40);
                    bx = WIDTH / 2.0 - width / 2.0;
                    by = 40;
                    x = (int) bx;
                    y = (int) by;
                    switch (apexLaserCycle % 4) {
                        case 0:
                            startSweepLaser(player);
                            break;
                        case 1:
                            startTrackingBeam(player);
                            break;
                        case 2:
                            startChannelingBeam(player);
                            break;
                        case 3:
                            startPersistentBeam(player);
                            break;
                    }
                    apexLaserCycle++;
                }
            } else {
                laserTimer--;
                updateLaserState(player);
                if (laserTimer <= 0) {
                    laserState = LASER_NONE;
                    laserActive = false;
                    laserCooldown = laserInterval + rand.nextInt(40);
                }
            }
        }

        void startSweepLaser(Player player) {
            beamAngle = angleToPlayer(player) - Math.PI * 0.25;
            beamAngle = Math.max(Math.PI * 0.05, beamAngle);
            sweepDir = 1;
            laserState = LASER_TELEGRAPH;
            laserTimer = 90;
            laserActive = false;
        }

        void startTrackingBeam(Player player) {
            beamAngle = angleToPlayer(player);
            laserState = LASER_TELEGRAPH;
            laserTimer = 120;
            laserActive = false;
            sweepDir = 99; // flag: telegraph leads to tracking not sweep
        }

        void startChannelingBeam(Player player) {
            beamAngle = angleToPlayer(player);
            channelingBeamWidth = 2;
            laserState = LASER_TELEGRAPH;
            laserTimer = 90;
            laserActive = false;
            sweepDir = 88;
        }

        void startPersistentBeam(Player player) {
            beamAngle = angleToPlayer(player);
            laserState = LASER_TELEGRAPH;
            laserTimer = 90;
            laserActive = false;
            sweepDir = 77;
        }

        void updateLaserState(Player player) {
            if (laserState == LASER_TELEGRAPH) {
                if (laserTimer <= 40) {
                    if (sweepDir == 88) {
                        beamAngle = Math.PI / 2;
                        channelingBeamWidth = 2;
                        laserState = LASER_CHANNELING;
                        laserTimer = 240;
                        laserActive = true;
                        sweepDir = 1;
                        playBossLaserSound();
                    } else if (sweepDir == 77) {
                        laserState = LASER_PERSISTENT;
                        laserTimer = 300;
                        laserActive = true;
                        sweepDir = rand.nextBoolean() ? 1 : -1;
                        playBossLaserSound();
                    } else {
                        laserState = LASER_SWEEP;
                        laserTimer = 80;
                        laserActive = true;
                        sweepDir = 1;
                        playBossLaserSound();
                    }
                }
            } else if (laserState == LASER_SWEEP) {
                beamAngle += sweepDir * 0.018;
                beamAngle = Math.max(Math.PI * 0.04, Math.min(Math.PI * 0.96, beamAngle));
                if (beamAngle >= Math.PI * 0.96 || beamAngle <= Math.PI * 0.04)
                    sweepDir *= -1;
            } else if (laserState == LASER_TRACKING) {
                beamAngle += sweepDir * 0.025;
                beamAngle = Math.max(Math.PI * 0.04, Math.min(Math.PI * 0.96, beamAngle));
                if (beamAngle >= Math.PI * 0.96 || beamAngle <= Math.PI * 0.04)
                    sweepDir *= -1;
            } else if (laserState == LASER_CHANNELING) {
                int elapsed = 240 - laserTimer;
                channelingBeamWidth = Math.min(2 + elapsed / 8, 28);
            } else if (laserState == LASER_PERSISTENT) {
                double target = angleToPlayer(player), diff = target - beamAngle;
                while (diff > Math.PI)
                    diff -= 2 * Math.PI;
                while (diff < -Math.PI)
                    diff += 2 * Math.PI;
                beamAngle += 0.012 * Math.signum(diff) * Math.min(Math.abs(diff), 1.0);
                beamAngle = Math.max(Math.PI * 0.04, Math.min(Math.PI * 0.96, beamAngle));
            }
        }

        boolean laserHitsPlayer(Rectangle hitbox) {
            if (!laserActive)
                return false;
            int ox = originX(), oy = originY(), ex = beamEndX(), ey = beamEndY();
            if (laserState == LASER_TRACKING)
                return false;
            int hw = (laserState == LASER_CHANNELING) ? channelingBeamWidth / 2 + 4 : 10;
            Rectangle fat = new Rectangle(hitbox.x - hw, hitbox.y - hw, hitbox.width + hw * 2, hitbox.height + hw * 2);
            return fat.intersectsLine(ox, oy, ex, ey);
        }

        void draw(Graphics2D g2, int frame, Player player) {
            if (!alive)
                return;
            float pulse = (float) (0.5 + 0.5 * Math.sin(pulsePhase));
            if (isApex && laserState != LASER_NONE)
                drawBossLaser(g2, frame);
            Color baseColor = getBossColor();
            int glowAlpha = isApex ? (int) (55 + 45 * pulse) : (int) (35 + 25 * pulse);
            g2.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), glowAlpha));
            g2.fillRoundRect((int) bx - 10, (int) by - 10, width + 20, height + 20, 22, 22);
            drawBossBody(g2, frame, pulse, baseColor);
            drawEngineTrail(g2, frame, pulse);
            if (viperPoisonStacks > 0) {
                // purple tint overlay
                float poisonAlpha = viperPoisonStacks * 0.08f;
                g2.setColor(new Color(120, 0, 180, (int) (poisonAlpha * 80)));
                g2.fillRoundRect((int) bx - 5, (int) by - 5, width + 10, height + 10, 16, 16);
                // smoke particles
                Random pr = new Random(frame * 7 + (int) bx);
                int smokeCount = viperPoisonStacks * 4;
                for (int i = 0; i < smokeCount; i++) {
                    float sf = (float) (frame * 0.04 + i * 1.3f);
                    int sx = (int) bx + pr.nextInt(width);
                    int sy = (int) by + (int) (((sf % 1.0f)) * (height + 30)) - 10;
                    int sa = (int) (80 + 60 * Math.abs(Math.sin(sf)));
                    int ssz = 6 + pr.nextInt(8);
                    g2.setColor(new Color(140, 0, 200, sa));
                    g2.fillOval(sx - ssz / 2, sy - ssz / 2, ssz, ssz);
                    g2.setColor(new Color(80, 0, 120, sa / 2));
                    g2.fillOval(sx - ssz, sy - ssz, ssz * 2, ssz * 2);
                }
                // poison stack indicator
                g2.setFont(new Font("Arial", Font.BOLD, 11));
                g2.setColor(new Color(200, 100, 255, 220));
                String poisonLabel = "☠ x" + viperPoisonStacks;
                g2.drawString(poisonLabel, (int) bx + width / 2 - 14, (int) by - 8);
                // pulsing border
                float pb = (float) (0.5 + 0.5 * Math.sin(frame * 0.2));
                g2.setColor(new Color(160, 0, 255, (int) (80 + 80 * pb)));
                g2.setStroke(new BasicStroke(2.5f + pb * 2f));
                g2.drawRoundRect((int) bx - 3, (int) by - 3, width + 6, height + 6, 14, 14);
                g2.setStroke(new BasicStroke(1));
            }
        }

        private Color getBossColor() {
            if (isApex)
                return new Color(255, 60, 0);
            if (waveNum == 10)
                return new Color(245, 242, 235); // white fox
            if (waveNum == 9)
                return new Color(80, 160, 60);
            if (waveNum == 8)
                return new Color(20, 120, 200);
            if (waveNum == 7)
                return new Color(180, 60, 160);
            if (waveNum == 6)
                return new Color(200, 80, 40);
            switch (waveNum % 5) {
                case 1:
                    return new Color(180, 20, 20);
                case 2:
                    return new Color(160, 0, 200);
                case 3:
                    return new Color(220, 100, 0);
                case 4:
                    return new Color(20, 180, 220);
                default:
                    return new Color(220, 30, 80);
            }
        }

        private void drawBossBody(Graphics2D g2, int frame, float pulse, Color base) {
            int cxb = cx();
            // Shadow
            g2.setColor(new Color(0, 0, 0, 60));
            g2.fillOval((int) bx + 6, (int) by + height - 6, width - 12, 10);

            if (waveNum == 6) {
                // ── BAKENEKO — realistic cat silhouette ──
                // Fur body — rounded, layered
                g2.setColor(new Color(180, 90, 40));
                g2.fillOval((int) bx - 4, (int) by + 10, width + 8, height - 6);
                // Belly fur highlight
                g2.setColor(new Color(240, 200, 160, 160));
                g2.fillOval(cxb - 14, (int) by + 18, 28, height - 24);
                // Fur stripes
                g2.setColor(new Color(120, 55, 20, 120));
                g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawArc(cxb - 20, (int) by + 12, 16, 20, -30, 160);
                g2.drawArc(cxb + 4, (int) by + 12, 16, 20, -150, 160);
                g2.drawArc(cxb - 10, (int) by + 8, 20, 16, 0, 180);
                g2.setStroke(new BasicStroke(1));
                // Rounded head
                g2.setColor(new Color(185, 95, 42));
                g2.fillOval(cxb - 24, (int) by - 14, 48, 38);
                // Cheek fur puffs
                g2.setColor(new Color(220, 160, 110, 180));
                g2.fillOval(cxb - 28, (int) by + 2, 22, 16);
                g2.fillOval(cxb + 6, (int) by + 2, 22, 16);
                // Realistic triangular ears with depth
                // Left ear — outer
                g2.setColor(new Color(185, 95, 42));
                g2.fillPolygon(
                        new int[] { cxb - 22, cxb - 34, cxb - 10 },
                        new int[] { (int) by - 12, (int) by - 36, (int) by - 32 }, 3);
                // Left ear — inner pink
                g2.setColor(new Color(220, 130, 130));
                g2.fillPolygon(
                        new int[] { cxb - 22, cxb - 30, cxb - 13 },
                        new int[] { (int) by - 14, (int) by - 30, (int) by - 28 }, 3);
                // Right ear — outer
                g2.setColor(new Color(185, 95, 42));
                g2.fillPolygon(
                        new int[] { cxb + 22, cxb + 34, cxb + 10 },
                        new int[] { (int) by - 12, (int) by - 36, (int) by - 32 }, 3);
                // Right ear — inner pink
                g2.setColor(new Color(220, 130, 130));
                g2.fillPolygon(
                        new int[] { cxb + 22, cxb + 30, cxb + 13 },
                        new int[] { (int) by - 14, (int) by - 30, (int) by - 28 }, 3);
                // Muzzle — realistic protruding snout
                g2.setColor(new Color(235, 190, 150));
                g2.fillOval(cxb - 13, (int) by + 6, 26, 16);
                // Nose — small triangle
                g2.setColor(new Color(220, 100, 110));
                g2.fillPolygon(
                        new int[] { cxb - 5, cxb + 5, cxb },
                        new int[] { (int) by + 9, (int) by + 9, (int) by + 14 }, 3);
                // Mouth lines
                g2.setColor(new Color(160, 80, 60, 180));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawArc(cxb - 8, (int) by + 13, 8, 6, 180, 180);
                g2.drawArc(cxb, (int) by + 13, 8, 6, 180, 180);
                g2.setStroke(new BasicStroke(1));
                // Slit pupils — realistic cat eyes
                g2.setColor(new Color(255, 210, 0));
                g2.fillOval(cxb - 18, (int) by, 13, 11);
                g2.fillOval(cxb + 5, (int) by, 13, 11);
                g2.setColor(Color.BLACK);
                // vertical slit
                g2.fillOval(cxb - 13, (int) by + 1, 4, 9);
                g2.fillOval(cxb + 9, (int) by + 1, 4, 9);
                // Eye glint
                g2.setColor(new Color(255, 255, 255, 200));
                g2.fillOval(cxb - 15, (int) by + 1, 3, 3);
                g2.fillOval(cxb + 7, (int) by + 1, 3, 3);
                // Eye glow
                int eg = (int) (100 + 120 * pulse);
                g2.setColor(new Color(255, 200, 0, eg / 2));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(cxb - 19, (int) by - 1, 15, 13);
                g2.drawOval(cxb + 4, (int) by - 1, 15, 13);
                g2.setStroke(new BasicStroke(1));
                // Whiskers — thin realistic lines
                g2.setColor(new Color(255, 245, 220, 210));
                g2.setStroke(new BasicStroke(0.9f));
                g2.drawLine(cxb - 4, (int) by + 12, cxb - 32, (int) by + 8);
                g2.drawLine(cxb - 4, (int) by + 15, cxb - 32, (int) by + 15);
                g2.drawLine(cxb - 4, (int) by + 18, cxb - 30, (int) by + 22);
                g2.drawLine(cxb + 4, (int) by + 12, cxb + 32, (int) by + 8);
                g2.drawLine(cxb + 4, (int) by + 15, cxb + 32, (int) by + 15);
                g2.drawLine(cxb + 4, (int) by + 18, cxb + 30, (int) by + 22);
                g2.setStroke(new BasicStroke(1));
                // Realistic curved tail
                g2.setColor(new Color(160, 75, 30));
                g2.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int tailSwing = (int) (16 * Math.sin(frame * 0.07));
                g2.drawArc((int) bx + width - 4, (int) by + 8 + tailSwing, 44, 36, -20, 210);
                // Tail tip lighter
                g2.setColor(new Color(230, 175, 120));
                g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawArc((int) bx + width + 10, (int) by + tailSwing + 18, 26, 22, -10, 140);
                g2.setStroke(new BasicStroke(1));
                // Fur edge soft outline
                g2.setColor(new Color(140, 65, 25, 80));
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval((int) bx - 4, (int) by + 10, width + 8, height - 6);
                g2.setStroke(new BasicStroke(1));

            } else if (waveNum == 7) {
                // ── NEKOMATA — spectral twin-tailed cat, split ethereal body ──
                // Ghostly aura
                float gf = (float) (0.4 + 0.6 * Math.abs(Math.sin(frame * 0.09)));
                g2.setColor(new Color(160, 60, 220, (int) (30 * gf)));
                g2.fillOval(cxb - 42, (int) by - 18, 84, height + 36);
                // Split body — left half with fur texture
                g2.setColor(new Color(80, 40, 120));
                g2.fillRoundRect((int) bx, (int) by + 12, width / 2 - 3, height - 12, 14, 14);
                // Right half
                g2.fillRoundRect(cxb + 3, (int) by + 12, width / 2 - 3, height - 12, 14, 14);
                // Belly lighter
                g2.setColor(new Color(140, 90, 180, 160));
                g2.fillOval((int) bx + 4, (int) by + 18, width / 2 - 10, height - 28);
                g2.fillOval(cxb + 6, (int) by + 18, width / 2 - 10, height - 28);
                // Rift glow between halves
                g2.setColor(new Color(220, 140, 255, (int) (120 * gf)));
                g2.fillRect(cxb - 4, (int) by + 12, 8, height - 12);
                g2.setColor(new Color(255, 200, 255, (int) (200 * gf)));
                g2.fillRect(cxb - 1, (int) by + 12, 2, height - 12);
                // Rounded head
                g2.setColor(new Color(90, 45, 130));
                g2.fillOval(cxb - 25, (int) by - 12, 50, 38);
                // Cheek fur
                g2.setColor(new Color(130, 80, 170, 160));
                g2.fillOval(cxb - 30, (int) by + 4, 22, 15);
                g2.fillOval(cxb + 8, (int) by + 4, 22, 15);
                // Ears — sharp ghost cat
                g2.setColor(new Color(90, 45, 130));
                g2.fillPolygon(
                        new int[] { cxb - 20, cxb - 32, cxb - 8 },
                        new int[] { (int) by - 10, (int) by - 36, (int) by - 32 }, 3);
                g2.fillPolygon(
                        new int[] { cxb + 20, cxb + 32, cxb + 8 },
                        new int[] { (int) by - 10, (int) by - 36, (int) by - 32 }, 3);
                // Inner ear glow — purple ethereal
                g2.setColor(new Color(200, 100, 255));
                g2.fillPolygon(
                        new int[] { cxb - 20, cxb - 28, cxb - 10 },
                        new int[] { (int) by - 12, (int) by - 30, (int) by - 27 }, 3);
                g2.fillPolygon(
                        new int[] { cxb + 20, cxb + 28, cxb + 10 },
                        new int[] { (int) by - 12, (int) by - 30, (int) by - 27 }, 3);
                // Muzzle
                g2.setColor(new Color(160, 110, 200));
                g2.fillOval(cxb - 12, (int) by + 8, 24, 14);
                // Nose
                g2.setColor(new Color(200, 80, 200));
                g2.fillPolygon(
                        new int[] { cxb - 4, cxb + 4, cxb },
                        new int[] { (int) by + 11, (int) by + 11, (int) by + 15 }, 3);
                // Mouth
                g2.setColor(new Color(120, 60, 140, 180));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawArc(cxb - 7, (int) by + 14, 7, 5, 180, 180);
                g2.drawArc(cxb, (int) by + 14, 7, 5, 180, 180);
                g2.setStroke(new BasicStroke(1));
                // Spectral slit eyes — glowing
                g2.setColor(new Color(220, 100, 255));
                g2.fillOval(cxb - 17, (int) by + 2, 13, 10);
                g2.fillOval(cxb + 4, (int) by + 2, 13, 10);
                g2.setColor(Color.BLACK);
                g2.fillOval(cxb - 14, (int) by + 3, 4, 8);
                g2.fillOval(cxb + 7, (int) by + 3, 4, 8);
                g2.setColor(new Color(255, 255, 255, 200));
                g2.fillOval(cxb - 15, (int) by + 3, 3, 3);
                g2.fillOval(cxb + 6, (int) by + 3, 3, 3);
                int eg = (int) (100 + 120 * pulse);
                g2.setColor(new Color(200, 60, 255, eg / 2));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(cxb - 18, (int) by + 1, 15, 12);
                g2.drawOval(cxb + 3, (int) by + 1, 15, 12);
                g2.setStroke(new BasicStroke(1));
                // Whiskers
                g2.setColor(new Color(220, 180, 255, 180));
                g2.setStroke(new BasicStroke(0.9f));
                g2.drawLine(cxb - 4, (int) by + 13, cxb - 30, (int) by + 9);
                g2.drawLine(cxb - 4, (int) by + 16, cxb - 30, (int) by + 16);
                g2.drawLine(cxb + 4, (int) by + 13, cxb + 30, (int) by + 9);
                g2.drawLine(cxb + 4, (int) by + 16, cxb + 30, (int) by + 16);
                g2.setStroke(new BasicStroke(1));
                // Twin tails — thick and fluffy with a ghostly fade
                int ts = (int) (15 * Math.sin(frame * 0.08));
                g2.setColor(new Color(100, 50, 150, 200));
                g2.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                // left tail sweeps left/up
                g2.drawArc((int) bx - 42, (int) by + 14 - ts, 44, 32, -10, 200);
                // right tail sweeps right/up
                g2.drawArc((int) bx + width - 2, (int) by + 14 + ts, 44, 32, -170, 200);
                // tail tip glow
                g2.setColor(new Color(200, 120, 255, (int) (160 * gf)));
                g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawArc((int) bx - 38, (int) by + 18 - ts, 28, 20, 10, 130);
                g2.drawArc((int) bx + width + 10, (int) by + 18 + ts, 28, 20, -140, 130);
                g2.setStroke(new BasicStroke(1));
                // Soft fur outline
                g2.setColor(new Color(160, 80, 200, 70));
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect((int) bx, (int) by + 12, width / 2 - 3, height - 12, 14, 14);
                g2.drawRoundRect(cxb + 3, (int) by + 12, width / 2 - 3, height - 12, 14, 14);
                g2.setStroke(new BasicStroke(1));

            } else if (waveNum == 8) {
                // ── TENGU — realistic winged demon, feathered, long beak ──
                int wingFlap = (int) (14 * Math.sin(frame * 0.10));
                // Wing shadow
                g2.setColor(new Color(0, 0, 0, 50));
                g2.fillPolygon(
                        new int[] { cxb - 8, (int) bx - 56, (int) bx - 28, cxb - 6 },
                        new int[] { (int) by + 22, (int) by + 14 - wingFlap + 6, (int) by + height + 6,
                                (int) by + height + 6 },
                        4);
                g2.fillPolygon(
                        new int[] { cxb + 8, (int) bx + width + 56, (int) bx + width + 28, cxb + 6 },
                        new int[] { (int) by + 22, (int) by + 14 - wingFlap + 6, (int) by + height + 6,
                                (int) by + height + 6 },
                        4);
                // Main wing membrane — dark blue-black
                g2.setColor(new Color(18, 22, 55, 220));
                g2.fillPolygon(
                        new int[] { cxb - 8, (int) bx - 58, (int) bx - 26, cxb - 5 },
                        new int[] { (int) by + 20, (int) by + 12 - wingFlap, (int) by + height, (int) by + height }, 4);
                g2.fillPolygon(
                        new int[] { cxb + 8, (int) bx + width + 58, (int) bx + width + 26, cxb + 5 },
                        new int[] { (int) by + 20, (int) by + 12 - wingFlap, (int) by + height, (int) by + height }, 4);
                // Wing feather layers — each a darker polygon
                g2.setColor(new Color(30, 40, 80, 200));
                g2.fillPolygon(
                        new int[] { cxb - 10, (int) bx - 40, (int) bx - 20, cxb - 8 },
                        new int[] { (int) by + 22, (int) by + 18 - wingFlap, (int) by + height, (int) by + height }, 4);
                g2.fillPolygon(
                        new int[] { cxb + 10, (int) bx + width + 40, (int) bx + width + 20, cxb + 8 },
                        new int[] { (int) by + 22, (int) by + 18 - wingFlap, (int) by + height, (int) by + height }, 4);
                // Feather quill lines
                g2.setColor(new Color(60, 80, 140, 140));
                g2.setStroke(new BasicStroke(1.2f));
                for (int f2 = 1; f2 <= 5; f2++) {
                    float ft = f2 / 6f;
                    int wx1 = (int) (cxb - 9 - (58 - 9) * ft);
                    int wy1 = (int) (by + 20 - (20 - 12 + wingFlap) * ft);
                    int wx2 = (int) (cxb - 8 - (26 - 8) * ft);
                    int wy2 = (int) (by + height);
                    g2.drawLine(wx1, wy1, wx2, wy2);
                    int rx1 = (int) (cxb + 9 + (58 - 9) * ft);
                    int rx2 = (int) (cxb + 8 + (26 - 8) * ft);
                    g2.drawLine(rx1, wy1, rx2, wy2);
                }
                g2.setStroke(new BasicStroke(1));
                // Body — layered robes
                // Outer robe — dark
                g2.setColor(new Color(18, 18, 45));
                g2.fillRoundRect((int) bx + 5, (int) by + 6, width - 10, height - 6, 18, 18);
                // Inner robe — blue stripe
                g2.setColor(new Color(20, 60, 120, 180));
                g2.fillRoundRect((int) bx + 14, (int) by + 10, width - 28, height - 10, 12, 12);
                // Robe sash
                g2.setColor(new Color(160, 30, 30, 200));
                g2.fillRect((int) bx + 8, (int) by + height / 2, width - 16, 6);
                // Head — fierce humanoid face
                g2.setColor(new Color(20, 18, 45));
                g2.fillOval(cxb - 20, (int) by - 10, 40, 36);
                // Face skin — more humanoid
                g2.setColor(new Color(50, 38, 28));
                g2.fillOval(cxb - 14, (int) by - 4, 28, 28);
                // Realistic long tengu nose/beak — protruding
                g2.setColor(new Color(180, 60, 30));
                // Ridge of nose
                int[] noseX = { cxb - 5, cxb + 5, cxb + 3, cxb, cxb - 3 };
                int[] noseY = { (int) by + 10, (int) by + 10, (int) by + 20, (int) by + 38, (int) by + 20 };
                g2.fillPolygon(noseX, noseY, 5);
                // Nostril shading
                g2.setColor(new Color(120, 35, 20));
                g2.fillOval(cxb - 4, (int) by + 16, 4, 4);
                g2.fillOval(cxb + 1, (int) by + 16, 4, 4);
                // Eyes — fierce slanted
                g2.setColor(new Color(255, 60, 0));
                // Left eye
                int[] lew = { cxb - 16, cxb - 8, cxb - 6, cxb - 14 };
                int[] leh = { (int) by + 4, (int) by + 2, (int) by + 10, (int) by + 12 };
                g2.fillPolygon(lew, leh, 4);
                // Right eye
                int[] rew = { cxb + 16, cxb + 8, cxb + 6, cxb + 14 };
                g2.fillPolygon(rew, leh, 4);
                // Pupils
                g2.setColor(Color.BLACK);
                g2.fillOval(cxb - 14, (int) by + 4, 6, 7);
                g2.fillOval(cxb + 8, (int) by + 4, 6, 7);
                // Iris glow
                int eg = (int) (110 + 110 * pulse);
                g2.setColor(new Color(255, 80, 0, eg));
                g2.fillOval(cxb - 13, (int) by + 5, 3, 4);
                g2.fillOval(cxb + 9, (int) by + 5, 3, 4);
                // Eye whites glint
                g2.setColor(new Color(255, 180, 100, 180));
                g2.fillOval(cxb - 15, (int) by + 4, 3, 3);
                g2.fillOval(cxb + 7, (int) by + 4, 3, 3);
                // Eyebrows — fierce angled
                g2.setColor(new Color(10, 10, 30));
                g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(cxb - 17, (int) by + 2, cxb - 6, (int) by + 5);
                g2.drawLine(cxb + 17, (int) by + 2, cxb + 6, (int) by + 5);
                g2.setStroke(new BasicStroke(1));
                // Tokin hat — realistic black box
                g2.setColor(new Color(10, 10, 25));
                g2.fillRect(cxb - 6, (int) by - 26, 12, 18);
                g2.setColor(new Color(15, 15, 38));
                g2.fillOval(cxb - 16, (int) by - 12, 32, 10);
                // Hat brim highlight
                g2.setColor(new Color(50, 50, 90, 130));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(cxb - 16, (int) by - 12, 32, 10);
                g2.setStroke(new BasicStroke(1));
                // Robes outline
                g2.setColor(new Color(80, 100, 180, 60));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect((int) bx + 5, (int) by + 6, width - 10, height - 6, 18, 18);
                g2.setStroke(new BasicStroke(1));
            } else if (waveNum == 9) {
                // ── TANUKI — realistic raccoon dog ──
                // Fluffy round body with fur gradient
                g2.setColor(new Color(90, 65, 30));
                g2.fillOval((int) bx - 10, (int) by + 4, width + 20, height + 4);
                // Belly cream patch
                g2.setColor(new Color(200, 175, 120, 200));
                g2.fillOval(cxb - 14, (int) by + 16, 28, height - 14);
                // Fur stripe texture on back
                g2.setColor(new Color(60, 40, 15, 100));
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawArc(cxb - 24, (int) by + 6, 48, 30, 0, 180);
                g2.drawArc(cxb - 18, (int) by + 10, 36, 22, 0, 180);
                g2.setStroke(new BasicStroke(1));
                // Big round head
                g2.setColor(new Color(95, 68, 32));
                g2.fillOval(cxb - 26, (int) by - 14, 52, 44);
                // Forehead darker stripe
                g2.setColor(new Color(40, 28, 10, 140));
                g2.fillOval(cxb - 16, (int) by - 14, 32, 16);
                // Classic raccoon mask — dark patches around eyes
                g2.setColor(new Color(25, 18, 8, 230));
                g2.fillOval(cxb - 22, (int) by, 18, 14);
                g2.fillOval(cxb + 4, (int) by, 18, 14);
                // Eyes — bright realistic
                g2.setColor(new Color(70, 180, 70));
                g2.fillOval(cxb - 20, (int) by + 2, 13, 11);
                g2.fillOval(cxb + 7, (int) by + 2, 13, 11);
                // Pupils
                g2.setColor(Color.BLACK);
                g2.fillOval(cxb - 16, (int) by + 3, 7, 9);
                g2.fillOval(cxb + 9, (int) by + 3, 7, 9);
                // Iris glint
                g2.setColor(new Color(255, 255, 255, 200));
                g2.fillOval(cxb - 15, (int) by + 4, 3, 3);
                g2.fillOval(cxb + 10, (int) by + 4, 3, 3);
                // Eye glow
                int eg = (int) (100 + 120 * pulse);
                g2.setColor(new Color(60, 220, 60, eg / 2));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawOval(cxb - 21, (int) by + 1, 15, 13);
                g2.drawOval(cxb + 6, (int) by + 1, 15, 13);
                g2.setStroke(new BasicStroke(1));
                // Cheek fur puffs
                g2.setColor(new Color(140, 100, 50, 180));
                g2.fillOval(cxb - 32, (int) by + 8, 22, 18);
                g2.fillOval(cxb + 10, (int) by + 8, 22, 18);
                // Realistic muzzle protrusion
                g2.setColor(new Color(185, 155, 100));
                g2.fillOval(cxb - 14, (int) by + 14, 28, 18);
                // Nose — prominent black tanuki nose
                g2.setColor(new Color(20, 15, 10));
                g2.fillOval(cxb - 7, (int) by + 15, 14, 10);
                // Nose highlight
                g2.setColor(new Color(80, 60, 50, 160));
                g2.fillOval(cxb - 5, (int) by + 16, 5, 4);
                // Mouth
                g2.setColor(new Color(100, 70, 30, 180));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawArc(cxb - 9, (int) by + 22, 9, 6, 180, 180);
                g2.drawArc(cxb, (int) by + 22, 9, 6, 180, 180);
                g2.setStroke(new BasicStroke(1));
                // Ears — small round furry
                g2.setColor(new Color(85, 60, 28));
                g2.fillOval(cxb - 28, (int) by - 14, 18, 15);
                g2.fillOval(cxb + 10, (int) by - 14, 18, 15);
                // Inner ears
                g2.setColor(new Color(200, 140, 100));
                g2.fillOval(cxb - 25, (int) by - 11, 12, 9);
                g2.fillOval(cxb + 13, (int) by - 11, 12, 9);
                // Straw hat — kasa
                g2.setColor(new Color(110, 80, 24));
                g2.fillOval(cxb - 42, (int) by - 20, 84, 18);
                g2.setColor(new Color(140, 105, 35));
                g2.fillOval(cxb - 26, (int) by - 30, 52, 18);
                g2.setColor(new Color(80, 55, 14));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(cxb - 42, (int) by - 20, 84, 18);
                // Concentric hat rings for realism
                g2.setColor(new Color(90, 65, 18, 100));
                g2.drawOval(cxb - 34, (int) by - 19, 68, 14);
                g2.drawOval(cxb - 22, (int) by - 27, 44, 14);
                g2.setStroke(new BasicStroke(1));
                // Belly fur outline
                g2.setColor(new Color(70, 50, 20, 80));
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval((int) bx - 10, (int) by + 4, width + 20, height + 4);
                g2.setStroke(new BasicStroke(1));

            } else if (waveNum == 10) {
                // ── KITSUNE — white fox with red markings, flowing orange tails ──
                float sf2 = (float) (0.5 + 0.5 * Math.sin(frame * 0.10));

                // ── TAILS — drawn first (behind body) ──
                // 5 flowing tails, orange-red gradient, fluffy
                int tailCount = 5;
                for (int t2 = 0; t2 < tailCount; t2++) {
                    float tSwing = (float) (20 * Math.sin(frame * 0.07 + t2 * 0.85));
                    int baseAlpha = 220 - t2 * 18;

                    // Outer fur — deep orange
                    g2.setColor(new Color(200, 80, 20, baseAlpha - 50));
                    g2.setStroke(new BasicStroke(10f - t2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawArc(
                            (int) bx + width - 10 + t2 * 6,
                            (int) by + 4 + (int) (tSwing),
                            58 + t2 * 10, 44,
                            -55 + t2 * 6, 230);

                    // Mid fur — orange
                    g2.setColor(new Color(240, 120, 30, baseAlpha));
                    g2.setStroke(new BasicStroke(7f - t2 * 0.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawArc(
                            (int) bx + width - 6 + t2 * 6,
                            (int) by + 8 + (int) (tSwing),
                            50 + t2 * 10, 38,
                            -48 + t2 * 5, 210);

                    // Inner tip — white/cream
                    g2.setColor(new Color(255, 245, 220, baseAlpha));
                    g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawArc(
                            (int) bx + width + 2 + t2 * 8,
                            (int) by + 14 + (int) (tSwing),
                            36 + t2 * 8, 26,
                            -38 + t2 * 4, 170);

                    // Glowing tail tip
                    float tp2 = (float) (0.5 + 0.5 * Math.sin(frame * 0.09 + t2 * 1.2));
                    g2.setColor(new Color(255, 160, 60, (int) (150 * tp2)));
                    g2.fillOval(
                            (int) bx + width + 42 + t2 * 8,
                            (int) by + 10 + (int) (tSwing) + 6,
                            14, 14);
                    g2.setColor(new Color(255, 220, 140, (int) (180 * tp2)));
                    g2.fillOval(
                            (int) bx + width + 46 + t2 * 8,
                            (int) by + 14 + (int) (tSwing) + 6,
                            6, 6);
                }
                g2.setStroke(new BasicStroke(1));

                // ── BODY ──
                // Main white body
                g2.setColor(new Color(245, 242, 235));
                g2.fillRoundRect((int) bx + 4, (int) by + 8, width - 8, height - 8, 18, 18);
                // Cream belly
                g2.setColor(new Color(255, 250, 240));
                g2.fillOval(cxb - 16, (int) by + 14, 32, height - 16);
                // Red stripe markings on body — like the reference
                g2.setColor(new Color(200, 40, 20, 180));
                g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawArc(cxb - 22, (int) by + 10, 18, 22, -20, 160);
                g2.drawArc(cxb + 4, (int) by + 10, 18, 22, -160, 160);
                g2.drawArc(cxb - 14, (int) by + 6, 28, 14, 0, 180);
                g2.setStroke(new BasicStroke(1));
                // Side flank fur tufts — orange
                g2.setColor(new Color(230, 110, 30, 180));
                g2.fillPolygon(
                        new int[] { (int) bx + 4, (int) bx - 18, (int) bx - 6, (int) bx + 18 },
                        new int[] { (int) by + 14, (int) by + height - 2, (int) by + height, (int) by + height }, 4);
                g2.fillPolygon(
                        new int[] { (int) bx + width - 4, (int) bx + width + 18, (int) bx + width + 6,
                                (int) bx + width - 18 },
                        new int[] { (int) by + 14, (int) by + height - 2, (int) by + height, (int) by + height }, 4);

                // ── HEAD ──
                // White head base
                g2.setColor(new Color(248, 244, 238));
                g2.fillOval(cxb - 24, (int) by - 16, 48, 42);
                // Red forehead V marking — key feature from reference
                g2.setColor(new Color(200, 35, 15));
                int[] vMarkX = { cxb, cxb - 12, cxb - 7, cxb, cxb + 7, cxb + 12 };
                int[] vMarkY = { (int) by - 4, (int) by - 16, (int) by - 7, (int) by - 2, (int) by - 7, (int) by - 16 };
                g2.fillPolygon(vMarkX, vMarkY, 6);
                // Red cheek stripes
                g2.setColor(new Color(200, 35, 15, 180));
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(cxb - 20, (int) by + 8, cxb - 30, (int) by + 14);
                g2.drawLine(cxb - 20, (int) by + 12, cxb - 28, (int) by + 20);
                g2.drawLine(cxb + 20, (int) by + 8, cxb + 30, (int) by + 14);
                g2.drawLine(cxb + 20, (int) by + 12, cxb + 28, (int) by + 20);
                g2.setStroke(new BasicStroke(1));

                // ── EARS — large pointed white with orange-red inner ──
                // Left ear — white outer
                g2.setColor(new Color(248, 244, 238));
                g2.fillPolygon(
                        new int[] { cxb - 18, cxb - 32, cxb - 6 },
                        new int[] { (int) by - 14, (int) by - 44, (int) by - 40 }, 3);
                // Left ear — orange-red inner
                g2.setColor(new Color(220, 80, 30));
                g2.fillPolygon(
                        new int[] { cxb - 19, cxb - 28, cxb - 9 },
                        new int[] { (int) by - 16, (int) by - 38, (int) by - 34 }, 3);
                // Left ear — dark grey tip
                g2.setColor(new Color(80, 70, 65));
                g2.fillPolygon(
                        new int[] { cxb - 24, cxb - 32, cxb - 18 },
                        new int[] { (int) by - 34, (int) by - 44, (int) by - 42 }, 3);

                // Right ear — white outer
                g2.setColor(new Color(248, 244, 238));
                g2.fillPolygon(
                        new int[] { cxb + 18, cxb + 32, cxb + 6 },
                        new int[] { (int) by - 14, (int) by - 44, (int) by - 40 }, 3);
                // Right ear — orange-red inner
                g2.setColor(new Color(220, 80, 30));
                g2.fillPolygon(
                        new int[] { cxb + 19, cxb + 28, cxb + 9 },
                        new int[] { (int) by - 16, (int) by - 38, (int) by - 34 }, 3);
                // Right ear — dark grey tip
                g2.setColor(new Color(80, 70, 65));
                g2.fillPolygon(
                        new int[] { cxb + 24, cxb + 32, cxb + 18 },
                        new int[] { (int) by - 34, (int) by - 44, (int) by - 42 }, 3);

                // ── MUZZLE — white protruding snout ──
                g2.setColor(new Color(252, 248, 242));
                g2.fillOval(cxb - 14, (int) by + 8, 28, 18);
                // Nose — small dark red
                g2.setColor(new Color(160, 30, 20));
                g2.fillPolygon(
                        new int[] { cxb - 5, cxb + 5, cxb },
                        new int[] { (int) by + 12, (int) by + 12, (int) by + 17 }, 3);
                // Nose highlight
                g2.setColor(new Color(200, 100, 100, 150));
                g2.fillOval(cxb - 4, (int) by + 12, 4, 3);
                // Mouth lines
                g2.setColor(new Color(140, 60, 40, 160));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawArc(cxb - 8, (int) by + 16, 8, 6, 180, 180);
                g2.drawArc(cxb, (int) by + 16, 8, 6, 180, 180);
                g2.setStroke(new BasicStroke(1));

                // ── EYES — fierce amber/orange, almond-shaped ──
                // Eye white base
                g2.setColor(new Color(240, 200, 100));
                int[] lExK = { cxb - 21, cxb - 8, cxb - 6, cxb - 19 };
                int[] lEyK = { (int) by + 1, (int) by - 2, (int) by + 10, (int) by + 13 };
                g2.fillPolygon(lExK, lEyK, 4);
                int[] rExK = { cxb + 21, cxb + 8, cxb + 6, cxb + 19 };
                g2.fillPolygon(rExK, lEyK, 4);
                // Amber iris
                g2.setColor(new Color(210, 130, 20));
                g2.fillOval(cxb - 19, (int) by, 11, 10);
                g2.fillOval(cxb + 8, (int) by, 11, 10);
                // Pupils — vertical slit like fox
                g2.setColor(Color.BLACK);
                g2.fillOval(cxb - 16, (int) by + 1, 5, 9);
                g2.fillOval(cxb + 11, (int) by + 1, 5, 9);
                // Glint
                g2.setColor(new Color(255, 255, 255, 210));
                g2.fillOval(cxb - 18, (int) by + 1, 3, 3);
                g2.fillOval(cxb + 9, (int) by + 1, 3, 3);
                // Red eye marking — like reference image stripe above eye
                g2.setColor(new Color(200, 35, 15, 200));
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(cxb - 22, (int) by - 1, cxb - 7, (int) by - 3);
                g2.drawLine(cxb + 22, (int) by - 1, cxb + 7, (int) by - 3);
                g2.setStroke(new BasicStroke(1));
                // Eye glow
                int eg = (int) (100 + 120 * pulse);
                g2.setColor(new Color(255, 160, 20, eg / 2));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(cxb - 20, (int) by - 1, 13, 12);
                g2.drawOval(cxb + 7, (int) by - 1, 13, 12);
                g2.setStroke(new BasicStroke(1));

                // ── WHISKERS ──
                g2.setColor(new Color(255, 252, 245, 200));
                g2.setStroke(new BasicStroke(0.9f));
                g2.drawLine(cxb - 4, (int) by + 14, cxb - 34, (int) by + 10);
                g2.drawLine(cxb - 4, (int) by + 17, cxb - 34, (int) by + 17);
                g2.drawLine(cxb - 4, (int) by + 20, cxb - 32, (int) by + 24);
                g2.drawLine(cxb + 4, (int) by + 14, cxb + 34, (int) by + 10);
                g2.drawLine(cxb + 4, (int) by + 17, cxb + 34, (int) by + 17);
                g2.drawLine(cxb + 4, (int) by + 20, cxb + 32, (int) by + 24);
                g2.setStroke(new BasicStroke(1));

                // ── SPIRIT ORB — fox fire green tinted ──
                g2.setColor(new Color(80, 200, 120, (int) (40 * sf2)));
                g2.fillOval(cxb - 14, (int) by + height / 2 - 10, 28, 20);
                g2.setColor(new Color(160, 255, 200, (int) (80 * sf2)));
                g2.fillOval(cxb - 7, (int) by + height / 2 - 5, 14, 10);

                // Fur outline
                if (kitsuneResting) {
                    float zz = (float) (0.5 + 0.5 * Math.sin(frame * 0.15));
                    // Simple blue border pulse — no fill, no loop
                    g2.setColor(new Color(100, 140, 255, (int) (100 * zz)));
                    g2.setStroke(new BasicStroke(3f));
                    g2.drawRoundRect((int) bx - 6, (int) by - 16, width + 12, height + 22, 20, 20);
                    g2.setStroke(new BasicStroke(1));
                    // Closed eye lines
                    g2.setColor(new Color(60, 80, 180, 200));
                    g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(cxb - 19, (int) by + 5, cxb - 8, (int) by + 5);
                    g2.drawLine(cxb + 8, (int) by + 5, cxb + 19, (int) by + 5);
                    g2.setStroke(new BasicStroke(1));
                    // 3 staggered Z's — no loop, no trig per sparkle
                    g2.setFont(new Font("Arial", Font.BOLD, 10));
                    g2.setColor(new Color(180, 210, 255, (int) (140 * zz)));
                    g2.drawString("z", cxb + 14, (int) by - 8);
                    g2.setFont(new Font("Arial", Font.BOLD, 14));
                    g2.setColor(new Color(160, 200, 255, (int) (140 * zz)));
                    g2.drawString("z", cxb + 20, (int) by - 22);
                    g2.setFont(new Font("Arial", Font.BOLD, 19));
                    g2.setColor(new Color(140, 180, 255, (int) (140 * zz)));
                    g2.drawString("Z", cxb + 26, (int) by - 38);
                }
            } else {
                // ── DEFAULT boss for all other waves ──
                Color hullColor = new Color(
                        Math.min(255, base.getRed() + 30),
                        Math.min(255, base.getGreen() + 20),
                        Math.min(255, base.getBlue() + 20));
                GradientPaint hull = new GradientPaint(
                        (int) bx, (int) by, hullColor,
                        (int) bx, (int) by + height, base.darker());
                g2.setPaint(hull);
                int[] hx = {
                        cxb - width / 2 + 4, cxb - width / 2 + 16,
                        cxb + width / 2 - 16, cxb + width / 2 - 4,
                        cxb + width / 2 - 4, cxb - width / 2 + 4 };
                int[] hy = {
                        (int) by + 8, (int) by,
                        (int) by, (int) by + 8,
                        (int) by + height - 4, (int) by + height - 4 };
                g2.fillPolygon(hx, hy, 6);
                g2.setPaint(null);
                g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 160));
                g2.fillPolygon(
                        new int[] { (int) bx, (int) bx - 22, (int) bx - 10, (int) bx + 16 },
                        new int[] { (int) by + 12, (int) by + height - 4, (int) by + height, (int) by + height }, 4);
                g2.fillPolygon(
                        new int[] { (int) bx + width, (int) bx + width + 22, (int) bx + width + 10,
                                (int) bx + width - 16 },
                        new int[] { (int) by + 12, (int) by + height - 4, (int) by + height, (int) by + height }, 4);
                g2.setColor(new Color(255, 255, 255, 60));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawPolygon(hx, hy, 6);
                g2.setStroke(new BasicStroke(1));
                if (waveNum >= 2) {
                    float cf = (float) (0.4 + 0.6 * Math.abs(Math.sin(frame * (isApex ? 0.14 : 0.07))));
                    g2.setColor(new Color(255, 255, 255, (int) (90 * cf)));
                    g2.fillOval(cxb - 18, (int) by + height / 2 - 12, 36, 24);
                }
                g2.setColor(isApex ? new Color(255, 80, 0) : Color.YELLOW);
                g2.fillOval(cxb - 22, (int) by + 14, 14, 14);
                g2.fillOval(cxb + 8, (int) by + 14, 14, 14);
                g2.setColor(Color.BLACK);
                g2.fillOval(cxb - 19, (int) by + 17, 8, 8);
                g2.fillOval(cxb + 11, (int) by + 17, 8, 8);
                int eyeGlow = (int) (120 + 100 * pulse);
                g2.setColor(isApex ? new Color(255, 120, 0, eyeGlow) : new Color(255, 200, 0, eyeGlow));
                g2.fillOval(cxb - 21, (int) by + 15, 4, 4);
                g2.fillOval(cxb + 9, (int) by + 15, 4, 4);
                if (isApex && laserState == LASER_TELEGRAPH) {
                    float tf = 1f - (laserTimer / 120f);
                    int ringR = (int) (20 + 60 * tf);
                    g2.setColor(new Color(255, 30, 30, (int) (80 * (1 - tf))));
                    g2.setStroke(new BasicStroke(3f));
                    g2.drawOval(cxb - ringR, (int) by + height / 2 - ringR / 2, ringR * 2, ringR);
                    g2.setStroke(new BasicStroke(1));
                    int ra = (int) (80 + 120 * tf);
                    g2.setColor(new Color(255, 30, 30, ra));
                    g2.setStroke(new BasicStroke(2f + tf * 3));
                    g2.drawOval(cxb - 24, (int) by - 6, 48, 48);
                    g2.setStroke(new BasicStroke(1));
                }
            }

        }

        private void drawEngineTrail(Graphics2D g2, int frame, float pulse) {
            int engineCount = isApex ? 5 : 3;
            int[] ex2 = new int[engineCount];
            if (isApex) {
                int step = width / (engineCount - 1);
                for (int i = 0; i < engineCount; i++)
                    ex2[i] = cx() - width / 2 + i * step;
            } else {
                ex2 = new int[] { cx() - 18, cx(), cx() + 18 };
            }
            for (int i = 0; i < engineCount; i++) {
                int len = (int) ((isApex ? 27 : 18) + 12 * Math.abs(Math.sin(frame * 0.15 + i * 1.2)));
                g2.setColor(new Color(255, 120, 0, (int) (130 * pulse)));
                g2.fillPolygon(new int[] { ex2[i] - 5, ex2[i] + 5, ex2[i] },
                        new int[] { (int) by + height, (int) by + height, (int) by + height + len }, 3);
                g2.setColor(new Color(255, 220, 80, (int) (90 * pulse)));
                g2.fillPolygon(new int[] { ex2[i] - 2, ex2[i] + 2, ex2[i] },
                        new int[] { (int) by + height, (int) by + height, (int) by + height + len - 4 }, 3);
            }
        }

        private void drawBossLaser(Graphics2D g2, int frame) {
            int ox = originX(), oy = originY();
            if (laserState == LASER_TELEGRAPH) {
                int ex = beamEndX(), ey = beamEndY();
                int alpha = (int) (60 + 120 * Math.abs(Math.sin(frame * 0.25)));
                g2.setColor(new Color(255, 0, 0, alpha));
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                        new float[] { 8f, 6f }, frame * 0.8f));
                g2.drawLine(ox, oy, ex, ey);
                g2.setStroke(new BasicStroke(1));
                float tf = 1f - laserTimer / 60f;
                if (tf > 0.4f) {
                    g2.setFont(new Font("Arial", Font.BOLD, 14));
                    g2.setColor(new Color(255, 60, 60, (int) (180 * Math.abs(Math.sin(frame * 0.3)))));
                    g2.drawString("⚠ LASER!", ox - 20, oy - 12);
                }
            } else if (laserState == LASER_SWEEP) {
                drawActiveBossBeam(g2, frame, ox, oy, beamEndX(), beamEndY(), new Color(255, 30, 30), 80, 8f, null);
            } else if (laserState == LASER_TRACKING) {
                drawActiveBossBeam(g2, frame, ox, oy, beamEndX(), beamEndY(), new Color(0, 200, 255), 180, 7f,
                        "SWEEP");
            } else if (laserState == LASER_CHANNELING) {
                int w = channelingBeamWidth;
                float intensity = Math.min(1f, w / 28f);
                Color beamColor = new Color((int) (255 * intensity), (int) (100 - 100 * intensity),
                        (int) (255 - 200 * intensity));
                int ex = beamEndX(), ey = beamEndY();
                g2.setColor(new Color(beamColor.getRed(), beamColor.getGreen(), beamColor.getBlue(),
                        (int) (20 * intensity)));
                g2.setStroke(new BasicStroke(w * 3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(ox, oy, ex, ey);
                g2.setColor(new Color(beamColor.getRed(), beamColor.getGreen(), beamColor.getBlue(),
                        (int) (200 * intensity)));
                g2.setStroke(new BasicStroke(Math.max(1, w), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(ox, oy, ex, ey);
                g2.setColor(new Color(255, 255, 255, (int) (220 * intensity)));
                g2.setStroke(new BasicStroke(Math.max(1, w / 3), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(ox, oy, ex, ey);
                g2.setStroke(new BasicStroke(1));
            } else if (laserState == LASER_PERSISTENT) {
                drawActiveBossBeam(g2, frame, ox, oy, beamEndX(), beamEndY(), new Color(255, 80, 0), 300, 10f,
                        "PERSISTENT");
            }
        }

        private void drawActiveBossBeam(Graphics2D g2, int frame, int x1, int y1, int x2, int y2, Color beamColor,
                int totalFrames, float baseWidth, String label) {
            float t = Math.max(0.1f, (float) laserTimer / totalFrames);
            g2.setColor(new Color(beamColor.getRed(), beamColor.getGreen(), beamColor.getBlue(), (int) (15 * t)));
            g2.setStroke(new BasicStroke(baseWidth * 4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x1, y1, x2, y2);
            g2.setColor(new Color(beamColor.getRed(), beamColor.getGreen(), beamColor.getBlue(), (int) (55 * t)));
            g2.setStroke(new BasicStroke(baseWidth * 2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x1, y1, x2, y2);
            g2.setColor(new Color(255, 160, 80, (int) (130 * t)));
            g2.setStroke(new BasicStroke(baseWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x1, y1, x2, y2);
            g2.setColor(new Color(255, 255, 255, (int) (230 * t)));
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x1, y1, x2, y2);
            g2.setStroke(new BasicStroke(1));
            if (label != null) {
                g2.setFont(new Font("Arial", Font.BOLD, 11));
                g2.setColor(new Color(beamColor.getRed(), beamColor.getGreen(), beamColor.getBlue(),
                        (int) (160 * Math.abs(Math.sin(frame * 0.12)))));
                g2.drawString(label, x1 - 26, y1 - 14);
            }
        }

        private int cx() {
            return (int) bx + width / 2;
        }
    }

    class Bullet {
        double x, y, dx, dy;
        int size;
        Color color;
        boolean enemy;

        Bullet(double x, double y, double dx, double dy, Color c, boolean enemy) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
            color = c;
            this.enemy = enemy;
            size = enemy ? 8 : 6;
        }

        void update() {
            x += dx;
            y += dy;
        }

        Rectangle getBounds() {
            return new Rectangle((int) x - size / 2, (int) y - size / 2, size, size);
        }

        void draw(Graphics2D g2) {
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 65));
            g2.fillOval((int) x - size, (int) y - size, size * 2, size * 2);
            g2.setColor(color);
            g2.fillOval((int) x - size / 2, (int) y - size / 2, size, size);
        }
    }

    class KnifeBullet extends Bullet {
        boolean leftSide;
        int spawnAnim = 12; // spawn animation frames
        final java.util.Deque<double[]> trail = new java.util.ArrayDeque<>();

        KnifeBullet(double x, double y, double vx, double vy, boolean leftSide) {
            super(x, y, vx, vy, new Color(180, 0, 255), false);
            this.leftSide = leftSide;
            this.size = 7;
        }

        @Override
        void update() {
            trail.addFirst(new double[] { x, y });
            if (trail.size() > 10)
                trail.removeLast();
            super.update();
            if (spawnAnim > 0)
                spawnAnim--;
        }

        @Override
        void draw(Graphics2D g2) {
            double dx = this.dx, dy = this.dy;
            double len = Math.sqrt(dx * dx + dy * dy);
            if (len < 0.001)
                return;
            double nx = dx / len, ny = dy / len;
            double angle = Math.atan2(dy, dx);

            // spawn animation: knives slide outward from center
            if (spawnAnim > 0) {
                float sf = spawnAnim / 12f;
                double perpX = -ny * (leftSide ? 1 : -1) * 18 * sf;
                double perpY = nx * (leftSide ? 1 : -1) * 18 * sf;
                // draw spawn flash
                g2.setColor(new Color(200, 0, 255, (int) (180 * sf)));
                g2.fillOval((int) (x + perpX) - 5, (int) (y + perpY) - 5, 10, 10);
            }

            // dark trail
            int ti = 0;
            for (double[] tp : trail) {
                float ta = (float) (trail.size() - ti) / trail.size();
                int alpha = (int) (120 * ta * ta);
                g2.setColor(new Color(80, 0, 120, alpha));
                int tsz = Math.max(1, (int) (5 * ta));
                g2.fillOval((int) tp[0] - tsz / 2, (int) tp[1] - tsz / 2, tsz, tsz);
                ti++;
            }

            // knife blade: rotated rectangle
            Graphics2D g3 = (Graphics2D) g2.create();
            g3.translate((int) x, (int) y);
            g3.rotate(angle + Math.PI / 2);
            // glow
            g3.setColor(new Color(140, 0, 200, 80));
            g3.fillRoundRect(-5, -12, 10, 24, 3, 3);
            // blade
            g3.setColor(new Color(200, 180, 255));
            g3.fillRoundRect(-2, -11, 4, 18, 2, 2);
            // edge highlight
            g3.setColor(new Color(255, 255, 255, 180));
            g3.fillRect(-1, -11, 1, 16);
            // handle
            g3.setColor(new Color(60, 0, 80));
            g3.fillRoundRect(-3, 6, 6, 5, 2, 2);
            g3.dispose();
        }
    }

    class PowerUp {
        double x, y;
        int type, anim = 0;
        final double dy = 2.5;

        PowerUp(double x, double y, int type) {
            this.x = x;
            this.y = y;
            this.type = type;
        }

        void update() {
            y += dy;
            anim++;
        }

        Rectangle getBounds() {
            return new Rectangle((int) x - 20, (int) y - 20, 40, 40);
        }

        void draw(Graphics2D g2) {
            Color c = puColor(type);
            float pulse = (float) (0.65 + 0.35 * Math.sin(anim * 0.14));
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (80 * pulse)));
            g2.fillOval((int) x - 22, (int) y - 22, 44, 44);
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (220 * pulse)));
            g2.fillRoundRect((int) x - 16, (int) y - 16, 32, 32, 9, 9);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(1.8f));
            g2.drawRoundRect((int) x - 16, (int) y - 16, 32, 32, 9, 9);
            g2.setStroke(new BasicStroke(1));
            g2.setFont(new Font("Arial", Font.BOLD, 13));
            FontMetrics fm = g2.getFontMetrics();
            String abbr = puLabel(type);
            g2.setColor(Color.WHITE);
            g2.drawString(abbr, (int) x - fm.stringWidth(abbr) / 2, (int) y + 5);
        }
    }

    class NovaParticle {
        double x, y, vx, vy;
        Color color;
        int life, maxLife;

        NovaParticle(double x, double y, double vx, double vy, Color c, int life) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            color = c;
            this.life = maxLife = life;
        }

        boolean update() {
            x += vx;
            y += vy;
            vx *= 0.88;
            vy *= 0.88;
            return --life > 0;
        }

        void draw(Graphics2D g2) {
            float frac = (float) life / maxLife;
            int alpha = (int) (200 * frac), sz = Math.max(1, (int) (4 * frac));
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            g2.fillOval((int) x - sz / 2, (int) y - sz / 2, sz, sz);
        }
    }

    class Snake {
        double x, y, vx, vy;
        boolean dead = false;
        int life = 180;
        int[] tx = new int[12], ty2 = new int[12];
        int tp = 0;

        Snake(double x, double y, double vx, double vy) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
        }

        void update(Boss boss) {
            if (dead)
                return;
            if (--life <= 0) {
                dead = true;
                return;
            }
            // no homing - straight shots only
            tx[tp % 12] = (int) x;
            ty2[tp % 12] = (int) y;
            tp++;
            x += vx;
            y += vy;
            if (y < -20 || y > HEIGHT + 20 || x < -20 || x > WIDTH + 20)
                dead = true;
        }

        Rectangle getBounds() {
            return new Rectangle((int) x - 6, (int) y - 6, 12, 12);
        }

        void draw(Graphics2D g2) {
            if (dead)
                return;
            for (int i = 1; i < Math.min(tp, 12); i++) {
                int ai = (tp - i) % 12, bi = (tp - i - 1 + 12) % 12;
                float a = (float) (12 - i) / 12;
                g2.setColor(new Color(0, 200, 80, (int) (180 * a)));
                g2.setStroke(new BasicStroke(3f - i * 0.2f));
                g2.drawLine(tx[ai], ty2[ai], tx[bi], ty2[bi]);
            }
            g2.setStroke(new BasicStroke(1));
            g2.setColor(new Color(0, 255, 100));
            g2.fillOval((int) x - 6, (int) y - 6, 12, 12);
        }
    }// Kitsune lance — fires fast then decelerates sharply, player can see gap
     // forming

    class KitsuneLanceBullet extends Bullet {
        double speed;
        static final double INITIAL_SPEED = 7.0;
        static final double MIN_SPEED = 4.5;
        static final double DECEL = 0.88;
        final java.util.Deque<double[]> trail = new java.util.ArrayDeque<>();

        KitsuneLanceBullet(double x, double y, double dirX, double dirY) {
            super(x, y, dirX * INITIAL_SPEED, dirY * INITIAL_SPEED,
                    new Color(255, 140, 140), true);
            this.speed = INITIAL_SPEED;
            this.size = 8;
        }

        @Override
        void update() {
            trail.addFirst(new double[] { x, y });
            if (trail.size() > 12)
                trail.removeLast();
            double slowThreshold = HEIGHT * 0.8;
            if (speed > MIN_SPEED && y >= slowThreshold) {
                speed = Math.max(MIN_SPEED, speed * DECEL);
                double len = Math.sqrt(dx * dx + dy * dy);
                if (len > 0.001) {
                    dx = (dx / len) * speed;
                    dy = (dy / len) * speed;
                }
            }
            x += dx;
            y += dy;
        }

        @Override
        void draw(Graphics2D g2) {
            float speedFrac = (float) Math.min(1.0, speed / INITIAL_SPEED);
            double angle = Math.atan2(dy, dx);

            // ── Trail ──
            int ti = 0;
            for (double[] tp : trail) {
                float ta = (float) (trail.size() - ti) / trail.size();
                g2.setColor(new Color(255, 80, 80, (int) (60 * ta * ta)));
                int tsz = Math.max(1, (int) (4 * ta));
                g2.fillOval((int) tp[0] - tsz / 2, (int) tp[1] - tsz / 2, tsz, tsz);
                ti++;
            }

            // ── Draw knife rotated along travel direction ──
            Graphics2D g3 = (Graphics2D) g2.create();
            g3.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g3.translate((int) x, (int) y);
            g3.rotate(angle + Math.PI / 2);

            // Glow around whole knife
            g3.setColor(new Color(255, 200, 200, (int) (60 * speedFrac)));
            g3.fillRoundRect(-7, -18, 14, 36, 6, 6);

            // ── Blade — glowing white metal ──
            int[] bladeX = { 0, -4, -3, 0, 3, 4 };
            int[] bladeY = { -18, -4, 8, 12, 8, -4 };
            g3.setColor(new Color(220, 230, 255, 230));
            g3.fillPolygon(bladeX, bladeY, 6);

            // Metal blade glow — pulsing white-blue
            g3.setColor(new Color(180, 220, 255, (int) (140 * speedFrac)));
            g3.setStroke(new BasicStroke(3f));
            g3.drawPolygon(bladeX, bladeY, 6);
            g3.setStroke(new BasicStroke(1));
            g3.setColor(new Color(255, 255, 255, (int) (100 * speedFrac)));
            g3.fillPolygon(bladeX, bladeY, 6);

            // Sharp edge highlight
            g3.setColor(new Color(255, 255, 255, 240));
            g3.setStroke(new BasicStroke(0.8f));
            g3.drawLine(-3, -14, -4, 6);

            // Blade shine shimmer
            g3.setColor(new Color(180, 210, 255, (int) (160 * speedFrac)));
            g3.fillPolygon(
                    new int[] { -1, -3, -2 },
                    new int[] { -18, -6, -6 }, 3);

            // ── Guard — dark red crossguard ──
            g3.setColor(new Color(180, 30, 30));
            g3.fillRoundRect(-6, 10, 12, 4, 3, 3);
            g3.setColor(new Color(220, 80, 80, 180));
            g3.fillRect(-5, 10, 10, 2);

            // ── Handle — red wrapped grip ──
            g3.setColor(new Color(160, 20, 20));
            g3.fillRoundRect(-3, 14, 6, 14, 3, 3);
            // Wrap bands
            g3.setColor(new Color(220, 60, 60, 200));
            for (int wrap = 0; wrap < 4; wrap++) {
                g3.setColor(new Color(220, 60, 60, 200));
                g3.fillRect(-3, 15 + wrap * 3, 6, 2);
            }
            // Handle shine
            g3.setColor(new Color(255, 120, 120, 120));
            g3.fillRect(-2, 14, 2, 12);

            // ── Pommel ──
            g3.setColor(new Color(180, 30, 30));
            g3.fillOval(-4, 26, 8, 6);
            g3.setColor(new Color(220, 80, 80, 180));
            g3.fillOval(-2, 27, 4, 3);

            // Blade outline
            g3.setColor(new Color(160, 180, 220, 180));
            g3.setStroke(new BasicStroke(0.8f));
            g3.drawPolygon(bladeX, bladeY, 6);

            g3.setStroke(new BasicStroke(1));
            g3.dispose();
        }
    }

    // Kitsune fox fire — slow drifting green-white orb, lingers as a hazard
    class KitsuneFoxFireBullet extends Bullet {
        int age = 0;
        static final int LIFE = 280;

        KitsuneFoxFireBullet(double x, double y, double dx, double dy) {
            super(x, y, dx, dy, new Color(255, 140, 140), true);
            this.size = 4;
        }

        @Override
        void update() {
            age++;
            // Gently drift — slight wobble
            dx += (Math.random() - 0.5) * 0.06;
            dy += (Math.random() - 0.5) * 0.06;
            // Cap speed
            double spd2 = Math.sqrt(dx * dx + dy * dy);
            if (spd2 > 1.4) {
                dx *= 1.4 / spd2;
                dy *= 1.4 / spd2;
            }
            x += dx;
            y += dy;
            // Die after lifetime
            if (age > LIFE) {
                y = -999;
            }
        }

        @Override

        void draw(Graphics2D g2) {
            if (age > LIFE)
                return;
            float life2 = 1f - (float) age / LIFE;
            float pulse = (float) (0.6 + 0.4 * Math.sin(age * 0.18));
            double angle = Math.atan2(dy, dx);

            Graphics2D g3 = (Graphics2D) g2.create();
            g3.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g3.translate((int) x, (int) y);
            g3.rotate(angle + Math.PI / 2);

            // Outer glow — fox fire green tint
            g3.setColor(new Color(180, 255, 180, (int) (40 * pulse * life2)));
            g3.fillRoundRect(-7, -18, 14, 36, 6, 6);

            // Blade — glowing white-green metal
            int[] bladeX = { 0, -4, -3, 0, 3, 4 };
            int[] bladeY = { -18, -4, 8, 12, 8, -4 };
            g3.setColor(new Color(200, 255, 210, (int) (230 * life2)));
            g3.fillPolygon(bladeX, bladeY, 6);

            // Fuller groove
            g3.setColor(new Color(255, 255, 255, (int) (200 * pulse * life2)));
            g3.fillRect(-1, -16, 2, 22);

            // Sharp edge highlight
            g3.setColor(new Color(255, 255, 255, (int) (240 * life2)));
            g3.setStroke(new BasicStroke(0.8f));
            g3.drawLine(-3, -14, -4, 6);

            // Blade shimmer — green fox fire glow
            g3.setColor(new Color(120, 255, 160, (int) (160 * pulse * life2)));
            g3.fillPolygon(new int[] { -1, -3, -2 }, new int[] { -18, -6, -6 }, 3);

            // Guard
            g3.setColor(new Color(180, 30, 30));
            g3.fillRoundRect(-6, 10, 12, 4, 3, 3);
            g3.setColor(new Color(220, 80, 80, 180));
            g3.fillRect(-5, 10, 10, 2);

            // Handle
            g3.setColor(new Color(160, 20, 20));
            g3.fillRoundRect(-3, 14, 6, 14, 3, 3);
            for (int wrap = 0; wrap < 4; wrap++) {
                g3.setColor(new Color(220, 60, 60, 200));
                g3.fillRect(-3, 15 + wrap * 3, 6, 2);
            }
            // Pommel
            g3.setColor(new Color(180, 30, 30));
            g3.fillOval(-4, 26, 8, 6);

            // Blade outline
            g3.setColor(new Color(160, 255, 200, (int) (180 * life2)));
            g3.setStroke(new BasicStroke(0.8f));
            g3.drawPolygon(bladeX, bladeY, 6);

            g3.setStroke(new BasicStroke(1));
            g3.dispose();
        }
    }

    class ExplosionParticle {
        double x, y, vx, vy;
        Color color;
        int life, maxLife;

        ExplosionParticle(double x, double y, double vx, double vy, Color c, int life) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            color = c;
            this.life = maxLife = life;
        }

        boolean update() {
            x += vx;
            y += vy;
            vx *= 0.92;
            vy *= 0.92;
            return --life > 0;
        }

        void draw(Graphics2D g2) {
            float frac = (float) life / maxLife;
            int alpha = (int) (220 * frac), sz = Math.max(1, (int) (5 * frac));
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            g2.fillOval((int) x - sz / 2, (int) y - sz / 2, sz, sz);
        }
    }

    class DamageIndicator {
        double x, y;
        String text;
        Color color;
        int life = 50;

        DamageIndicator(double x, double y, String text, Color color) {
            this.x = x;
            this.y = y;
            this.text = text;
            this.color = color;
        }

        boolean update() {
            y -= 1.2;
            return --life > 0;
        }

        void draw(Graphics2D g2) {
            float f = (float) life / 50f;
            g2.setFont(new Font("Arial", Font.BOLD, 13));
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (220 * f)));
            g2.drawString(text, (int) x, (int) y);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Bullet Hell");
            BulletHellGame game = new BulletHellGame();
            frame.add(game);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            frame.setVisible(true);
        });
    }
}