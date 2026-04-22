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

    static final int FIRE_MOUSE = 0;
    static final int FIRE_SPACE = 1;

    static final int PU_DOUBLE_SHOT = 0;
    static final int PU_SHIELD = 1;
    static final int PU_COUNT = 2;

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
    static final int CLASS_SENTINEL = 3;
    static final int CLASS_VIPER = 4;
    static final int CLASS_COUNT = 5;

    static final int NOVA_CHARGE_FRAMES = 180;
    static final int NOVA_COOLDOWN_FRAMES = 60;
    static final int NOVA_LASER_FRAMES = 18;
    static final int NOVA_LASER_WIDTH = 18;
    // Phantom
    static final int PHANTOM_DASH_CD = 180;
    static final int PHANTOM_DECOY_LIFE = 120;
    // Sentinel
    static final int SENTINEL_ORB_COUNT = 4;
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
    static final int SCENE_COUNT = 6;
    private int currentScene = SCENE_SPACE;
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
    private double sentinelAngle = 0;

    // Viper
    private final ArrayList<Snake> snakes = new ArrayList<>();
    private int viperFireCD = 0;

    // PowerUp timers
    private boolean hasShield = false;
    private int shieldTimer = 0;
    private boolean doubleShot = false;
    private int doubleShotTimer = 0;

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
    private final Rectangle btnMusicToggle = new Rectangle(WIDTH / 2 - 120, 375, 240, 46);
    private final Rectangle sliderTrack = new Rectangle(WIDTH / 2 - 110, 440, 220, 18);
    private final Rectangle btnSettBack = new Rectangle(WIDTH / 2 - 100, 700, 200, 50);

    private final Rectangle btnDiffEasy = new Rectangle(WIDTH / 2 - 120, 300, 240, 80);
    private final Rectangle btnDiffNormal = new Rectangle(WIDTH / 2 - 120, 400, 240, 80);
    private final Rectangle btnDiffHard = new Rectangle(WIDTH / 2 - 120, 500, 240, 80);
    private final Rectangle btnDiffBack = new Rectangle(WIDTH / 2 - 100, 640, 200, 50);

    private final Rectangle btnClassBack = new Rectangle(WIDTH / 2 - 100, 660, 200, 50);
    // 5 class cards in a single row (centered)
    private static final int CW = 108, CH = 200, CGAP = 10;
    private static final int CROW1Y = 140;
    private final Rectangle[] btnClass = {
            new Rectangle(WIDTH / 2 - (CW * 5 + CGAP * 4) / 2 + 0 * (CW + CGAP), CROW1Y, CW, CH),
            new Rectangle(WIDTH / 2 - (CW * 5 + CGAP * 4) / 2 + 1 * (CW + CGAP), CROW1Y, CW, CH),
            new Rectangle(WIDTH / 2 - (CW * 5 + CGAP * 4) / 2 + 2 * (CW + CGAP), CROW1Y, CW, CH),
            new Rectangle(WIDTH / 2 - (CW * 5 + CGAP * 4) / 2 + 3 * (CW + CGAP), CROW1Y, CW, CH),
            new Rectangle(WIDTH / 2 - (CW * 5 + CGAP * 4) / 2 + 4 * (CW + CGAP), CROW1Y, CW, CH),
    };

    // =================================================================
    public BulletHellGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(6, 6, 22));
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        Random sr = new Random(7777);
        for (int i = 0; i < starX.length; i++) {
            starX[i] = sr.nextInt(WIDTH);
            starY[i] = sr.nextInt(HEIGHT);
            starSz[i] = sr.nextInt(3) + 1;
        }
        initScenery();
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
        int baseSpd = player.speed;
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
            case CLASS_SENTINEL:
                firingThisFrame = false;
                updateSentinel(wantsFire);
                spd = Math.max(2, (int) (spd * 0.80));
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
        if (doubleShotTimer > 0 && --doubleShotTimer == 0)
            doubleShot = false;
        if (pickupTimer > 0)
            pickupTimer--;

        if (frameCount % 360 == 0 && !bossTransition) {
            int type = (frameCount / 360) % PU_COUNT;
            boolean already = (type == PU_DOUBLE_SHOT && doubleShot) || (type == PU_SHIELD && hasShield);
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
                boss.hp -= 1;
                score += 6;
                if (boss.hp <= 0 && boss.alive) {
                    boss.alive = false; // mark immediately to prevent re-entry
                    bossDefeated();
                }
            }
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
                score += 10;
                if (rand.nextInt(20) == 0) {
                    int dt = rand.nextInt(PU_COUNT);
                    boolean dup = (dt == PU_DOUBLE_SHOT && doubleShot) || (dt == PU_SHIELD && hasShield);
                    if (!dup)
                        powerUps.add(new PowerUp(boss.x + boss.width / 2, boss.y + boss.height, dt));
                }
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
            b.update();
            if (b.y > HEIGHT + 10 || b.x < -10 || b.x > WIDTH + 10) {
                enemyBullets.remove(i);
                continue;
            }
            if (player.alive && player.getHitbox().intersects(b.getBounds())) {
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
            if (player.alive && boss.laserHitsPlayer(player.getHitbox())) {
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
                score += 15;
                s.dead = true;
                if (boss.hp <= 0 && boss.alive) {
                    boss.alive = false;
                    bossDefeated();
                    break;
                }
            }
        }

        // Sentinel orb bullet-reflect
        if (selectedClass == CLASS_SENTINEL) {
            int pcx = player.x + player.size / 2, pcy = player.y + player.size / 2;
            for (int i = enemyBullets.size() - 1; i >= 0; i--) {
                Bullet b = enemyBullets.get(i);
                for (int o = 0; o < SENTINEL_ORB_COUNT; o++) {
                    double a = sentinelAngle + 2 * Math.PI * o / SENTINEL_ORB_COUNT;
                    double ox = pcx + Math.cos(a) * 32, oy = pcy + Math.sin(a) * 32;
                    if (Math.sqrt((b.x - ox) * (b.x - ox) + (b.y - oy) * (b.y - oy)) < 10) {
                        double tx2 = boss.x + boss.width / 2 - b.x, ty2 = boss.y + boss.height / 2 - b.y;
                        double tl = Math.sqrt(tx2 * tx2 + ty2 * ty2);
                        if (tl > 0) {
                            b.dx = tx2 / tl * 6;
                            b.dy = ty2 / tl * 6;
                            b.enemy = false;
                        }
                        playerBullets.add(b);
                        enemyBullets.remove(i);
                        break;
                    }
                }
            }
        }

        // Explosion particles
        Iterator<ExplosionParticle> ep = explosionParticles.iterator();
        while (ep.hasNext())
            if (!ep.next().update())
                ep.remove();

        updateScenery();
    }

    // ── FIX: bossDefeated now safe against double-call ────────────────
    private void bossDefeated() {
        // boss.alive is already set to false by the caller before invoking this
        score += 500;
        wave++;
        bossTransition = true;
        novaLaserActive = false;
        playerBullets.clear();
        enemyBullets.clear();
        setScene((wave - 1) % SCENE_COUNT);
        Timer t = new Timer(1800, ev -> {
            boss = new Boss(WIDTH / 2 - 40, 60, wave);
            bossTransition = false;
        });
        t.setRepeats(false);
        t.start();
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

        if (!overheated && wantsFire && frameCount % FIRE_RATE == 0) {
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

    // ── Sentinel ──────────────────────────────────────────────────────
    private void updateSentinel(boolean wantsFire) {
        sentinelAngle += 0.04 + (wantsFire ? 0.06 : 0);
        if ((keys[KeyEvent.VK_SHIFT] || keys[KeyEvent.VK_Z]) && frameCount % 120 == 0) {
            int pcx = player.x + player.size / 2, pcy = player.y + player.size / 2;
            for (Bullet b : enemyBullets) {
                double dx = b.x - pcx, dy2 = b.y - pcy, len = Math.sqrt(dx * dx + dy2 * dy2);
                if (len < 100 && len > 0) {
                    b.dx += dx / len * 5;
                    b.dy += dy2 / len * 5;
                }
            }
            score += 20;
            pickupMsg = "PULSE!";
            pickupTimer = 50;
            shakeTimer = 10;
            shakeIntensity = 4;
        }
        int rate = Math.max(20, 35);
        if (frameCount % rate == 0) {
            int pcx = player.x + player.size / 2, pcy = player.y + player.size / 2;
            double dx = mouseX - pcx, dy = mouseY - pcy, len = Math.sqrt(dx * dx + dy * dy), bs = 9;
            double bvx = 0, bvy = -bs;
            if (len > 1) {
                bvx = (dx / len) * bs;
                bvy = (dy / len) * bs;
            }
            playerBullets.add(new Bullet(pcx, pcy, bvx * 1.2, bvy * 1.2, new Color(80, 255, 200), false));
            if (soundCooldown == 0) {
                playSpacegunSound();
                soundCooldown = rate;
            }
        }
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

        double speedScale, countScale;
        int ringInterval, aimedInterval, spiralInterval;
        if (wave <= 5) {
            speedScale = 0.55;
            countScale = 0.45;
            ringInterval = 130;
            aimedInterval = 90;
            spiralInterval = 22;
        } else if (wave <= 10) {
            speedScale = 0.75;
            countScale = 0.70;
            ringInterval = 100;
            aimedInterval = 55;
            spiralInterval = 14;
        } else {
            speedScale = 1.0;
            countScale = 1.0;
            ringInterval = 80;
            aimedInterval = 35;
            spiralInterval = 8;
        }

        if (!boss.isApex && frameCount % ringInterval == 0) {
            int cnt = (int) Math.max(4, Math.min(8 + wave * 2, 20) * countScale);
            for (int i = 0; i < cnt; i++) {
                double a = 2 * Math.PI * i / cnt + Math.toRadians(frameCount * 2);
                double s = (2.2 + wave * 0.15) * dm * speedScale;
                enemyBullets.add(new Bullet(cx, cy, Math.cos(a) * s, Math.sin(a) * s, Color.RED, true));
            }
        }
        if (!boss.isApex && wave >= 2 && frameCount % aimedInterval == 0) {
            double dx = player.x - cx, dy2 = player.y - cy, len = Math.sqrt(dx * dx + dy2 * dy2);
            if (len > 0) {
                double s = 3.0 * dm * speedScale;
                enemyBullets.add(new Bullet(cx, cy, dx / len * s, dy2 / len * s, Color.ORANGE, true));
            }
        }
        if (!boss.isApex && wave >= 3 && frameCount % spiralInterval == 0) {
            double a = Math.toRadians(frameCount * 5), s = 2.5 * dm * speedScale;
            enemyBullets.add(new Bullet(cx, cy, Math.cos(a) * s, Math.sin(a) * s, Color.MAGENTA, true));
            enemyBullets.add(new Bullet(cx, cy, -Math.cos(a) * s, -Math.sin(a) * s, Color.MAGENTA, true));
        }
        if (!boss.isApex && wave >= 4 && wave > 5 && frameCount % 20 == 0) {
            for (int i = 0; i < 4; i++) {
                double a = Math.PI / 2 * i + Math.toRadians(frameCount * 2);
                double s = 3.0 * dm * speedScale;
                enemyBullets.add(new Bullet(cx, cy, Math.cos(a) * s, Math.sin(a) * s, new Color(255, 80, 0), true));
            }
        }
        if (!boss.isApex && wave >= 5 && wave > 5 && frameCount % 55 == 0) {
            for (int i = 0; i < 14; i++) {
                double a = 2 * Math.PI * i / 14 + Math.toRadians(frameCount);
                double s = 3.5 * dm * speedScale;
                enemyBullets.add(new Bullet(cx, cy, Math.cos(a) * s, Math.sin(a) * s, Color.YELLOW, true));
            }
        }
        if (boss.isApex) {
            if (frameCount % 80 == 0) {
                int cnt = (int) Math.max(8, Math.min(10 + wave, 18) * countScale);
                ;
                for (int i = 0; i < cnt; i++) {
                    double a = 2 * Math.PI * i / cnt + Math.toRadians(frameCount * 3);
                    double s = 1.6 * dm * speedScale;
                    enemyBullets
                            .add(new Bullet(cx, cy, Math.cos(a) * s, Math.sin(a) * s, new Color(255, 100, 200), true));
                }
            }
            if (frameCount % 30 == 0) {
                double dx = player.x - cx, dy2 = player.y - cy, len = Math.sqrt(dx * dx + dy2 * dy2);
                if (len > 0) {
                    double s = 2.2 * dm * speedScale;
                    enemyBullets.add(new Bullet(cx, cy, dx / len * s, dy2 / len * s, new Color(255, 60, 180), true));
                }
            }
        }
    }

    private void applyPowerUp(int type) {
        switch (type) {
            case PU_DOUBLE_SHOT:
                doubleShot = true;
                doubleShotTimer = 480;
                pickupMsg = "DOUBLE SHOT!";
                break;
            case PU_SHIELD:
                hasShield = true;
                shieldTimer = 600;
                pickupMsg = "SHIELD ON!";
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
        sentinelAngle += 0.04;
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
            case STATE_GAME_OVER:
                drawGame(g2);
                drawGameOver(g2);
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
    }

    private void drawDiffBtn(Graphics2D g2, Rectangle r, String title, Color accent, String l1, String l2,
            boolean sel) {
        if (sel) {
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 40));
            g2.fillRoundRect(r.x - 5, r.y - 5, r.width + 10, r.height + 10, 16, 16);
        }
        g2.setColor(sel ? new Color(15, 20, 60) : new Color(10, 10, 30));
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 12, 12);
        g2.setColor(accent);
        g2.setStroke(new BasicStroke(sel ? 2.5f : 1.2f));
        g2.drawRoundRect(r.x, r.y, r.width, r.height, 12, 12);
        g2.setStroke(new BasicStroke(1));
        g2.setFont(new Font("Arial", Font.BOLD, 20));
        g2.setColor(sel ? Color.WHITE : new Color(180, 180, 220));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(title, r.x + r.width / 2 - fm.stringWidth(title) / 2, r.y + 28);
        g2.setFont(new Font("Arial", Font.PLAIN, 13));
        g2.setColor(sel ? new Color(210, 230, 255) : new Color(110, 110, 160));
        g2.drawString("• " + l1, r.x + 20, r.y + 50);
        g2.drawString("• " + l2, r.x + 20, r.y + 68);
    }

    // ── Class select (5 cards, single row, no DEF stat) ──────────────
    private static final String[] CLS_NAMES = { "MACHINE\nGUNNER", "NOVA", "PHANTOM", "SENTINEL", "VIPER" };
    private static final String[] CLS_DESC = {
            "Rapid fire.\nOverheats.",
            "Charge laser.\nOne big hit.",
            "Dash+decoy.\nSHIFT=blink.",
            "Orb shield.\nDeflects bullets.",
            "Homing snakes.\nAuto-tracks boss."
    };
    private static final Color[] CLS_COL = {
            new Color(0, 220, 255), new Color(130, 80, 255), new Color(180, 0, 255),
            new Color(80, 255, 200), new Color(0, 255, 100)
    };
    // Stats: ATK, SPD, MOB (3 stats only — DEF removed)
    private static final int[][] CLS_STATS = {
            { 5, 3, 3 }, { 2, 1, 2 }, { 3, 2, 5 }, { 2, 3, 2 }, { 4, 2, 3 }
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
            case CLASS_SENTINEL:
                g2.fillPolygon(new int[] { scx, scx - 9, scx + 9 }, new int[] { sy, sy + 22, sy + 22 }, 3);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 130));
                for (int o = 0; o < 4; o++) {
                    double a = sentinelAngle + Math.PI * o / 2;
                    g2.fillOval((int) (scx + Math.cos(a) * 15) - 4, (int) (sy + 11 + Math.sin(a) * 10) - 4, 8, 8);
                }
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
        String[] sl2 = { "ATK", "SPD", "MOB" };
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
            g2.setFont(new Font("Courier New", Font.BOLD, 8));
            g2.setColor(accent);
            String st = "▶ CLICK START";
            g2.drawString(st, r.x + r.width / 2 - g2.getFontMetrics().stringWidth(st) / 2, r.y + r.height - 4);
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

        // Sentinel orb FX
        if (selectedClass == CLASS_SENTINEL && player.alive) {
            int pcx2 = player.x + player.size / 2, pcy2 = player.y + player.size / 2;
            for (int o = 0; o < SENTINEL_ORB_COUNT; o++) {
                double a = sentinelAngle + 2 * Math.PI * o / SENTINEL_ORB_COUNT;
                int ox = (int) (pcx2 + Math.cos(a) * 32), oy = (int) (pcy2 + Math.sin(a) * 32);
                g2.setColor(new Color(80, 255, 200, 160));
                g2.fillOval(ox - 7, oy - 7, 14, 14);
                g2.setColor(new Color(180, 255, 220, 100));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(ox - 7, oy - 7, 14, 14);
                g2.setStroke(new BasicStroke(1));
            }
        }
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
                g2.setColor(new Color(0, 200, 255, 30));
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
        g2.drawString("WAVE: " + wave, WIDTH - 100, 24);
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
        }

        drawPowerupLegend(g2);

        if (bossTransition) {
            g2.setColor(new Color(0, 0, 0, 170));
            g2.fillRect(0, 0, WIDTH, HEIGHT);
            boolean nextIsApex = (wave % 5 == 0);
            g2.setColor(nextIsApex ? new Color(255, 160, 0) : Color.YELLOW);
            g2.setFont(new Font("Arial", Font.BOLD, 46));
            String wt = "WAVE " + wave + "!";
            g2.drawString(wt, WIDTH / 2 - g2.getFontMetrics().stringWidth(wt) / 2, HEIGHT / 2 - 20);
            if (nextIsApex) {
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
        int[][] pus = { { PU_DOUBLE_SHOT, doubleShot ? 1 : 0, doubleShotTimer, 480 },
                { PU_SHIELD, hasShield ? 1 : 0, shieldTimer, 600 } };
        int px = 10, py = 36;
        for (int[] pu : pus) {
            if (pu[1] == 0)
                continue;
            Color c = puColor(pu[0]);
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 180));
            g2.fillRoundRect(px, py, 44, 26, 8, 8);
            float frac = pu[3] > 0 ? (float) pu[2] / pu[3] : 0f;
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 100));
            g2.fillRoundRect(px, py + 26, (int) (44 * frac), 4, 2, 2);
            g2.setFont(new Font("Arial", Font.BOLD, 12));
            g2.setColor(Color.WHITE);
            FontMetrics fm = g2.getFontMetrics();
            String lbl = puLabel(pu[0]);
            g2.drawString(lbl, px + 22 - fm.stringWidth(lbl) / 2, py + 18);
            px += 50;
        }
    }

    private void drawPowerupLegend(Graphics2D g2) {
        int lx = WIDTH - 134, ly = HEIGHT - 60;
        g2.setFont(new Font("Arial", Font.PLAIN, 9));
        g2.setColor(new Color(80, 80, 130));
        g2.drawString("POWERUPS:", lx, ly);
        g2.drawString("2x=Double Shot", lx, ly + 12);
        g2.drawString("SH=Shield", lx, ly + 24);
    }

    private void drawGameOver(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 175));
        g2.fillRect(0, 0, WIDTH, HEIGHT);
        g2.setFont(new Font("Arial", Font.BOLD, 54));
        String go = "GAME  OVER";
        int gx = WIDTH / 2 - g2.getFontMetrics().stringWidth(go) / 2;
        g2.setColor(new Color(255, 0, 0, 80));
        g2.drawString(go, gx - 2, HEIGHT / 2 - 18);
        g2.setColor(Color.RED);
        g2.drawString(go, gx, HEIGHT / 2 - 20);
        g2.setFont(new Font("Arial", Font.PLAIN, 22));
        g2.setColor(Color.WHITE);
        g2.drawString("Final Score: " + score, WIDTH / 2 - 82, HEIGHT / 2 + 30);
        g2.drawString("Reached Wave: " + wave, WIDTH / 2 - 82, HEIGHT / 2 + 58);
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.setColor(new Color(200, 200, 255));
        g2.drawString("[ R ]  Restart", WIDTH / 2 - 60, HEIGHT / 2 + 100);
        g2.drawString("[ M ]  Main Menu", WIDTH / 2 - 67, HEIGHT / 2 + 124);
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
        g2.drawString(label, r.x + r.width / 2 - fm.stringWidth(label) / 2, r.y + r.height / 2 + 6);
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
        g2.drawString("▸  " + label, WIDTH / 2 - 120, y);
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
        boss = new Boss(WIDTH / 2 - 40, 60, wave);
        playerBullets.clear();
        enemyBullets.clear();
        powerUps.clear();
        snakes.clear();
        explosionParticles.clear();
        setScene(SCENE_SPACE);
        phantomDashCD = 0;
        phantomInvinc = false;
        phantomInvincT = 0;
        phantomDecoyX = -999;
        phantomDecoyY = -999;
        phantomDecoyT = 0;
        phantomAfterT = 0;
        sentinelAngle = 0;
        viperFireCD = 0;
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
            if (e.getKeyCode() == KeyEvent.VK_M)
                gameState = STATE_MENU;
        }
        if (gameState == STATE_PLAYING && e.getKeyCode() == KeyEvent.VK_ESCAPE)
            gameState = STATE_MENU;
        if (gameState == STATE_CLASS_SEL && e.getKeyCode() == KeyEvent.VK_ENTER)
            startGame();
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
        if (gameState == STATE_MENU) {
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
            } else if (btnSettBack.contains(p))
                gameState = STATE_MENU;
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
                    case CLASS_SENTINEL:
                        shipColor = new Color(80, 255, 200);
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

        Boss(int bx2, int by2, int wave) {
            this.bx = bx2;
            this.by = by2;
            this.x = (int) bx;
            this.y = (int) by;
            this.waveNum = wave;
            this.isApex = (wave % 5 == 0);
            maxHp = hp = isApex ? (10 + wave * 20) * 1.1 : (10 + wave * 20);
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
            String[] n = { "VOID SCOUT", "REAPER MK-II", "CRIMSON TITAN", "OMEGA CORE", "HELL'S EYE" };
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
            double speedMult = isApex ? 1.4 : 1.0;
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
                by = Math.max(20, Math.min(HEIGHT / 2.5, by));
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
        }

        private Color getBossColor() {
            if (isApex)
                return new Color(255, 60, 0);
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
            g2.setColor(new Color(0, 0, 0, 60));
            g2.fillOval((int) bx + 6, (int) by + height - 6, width - 12, 10);
            Color hullColor = new Color(Math.min(255, base.getRed() + 30), Math.min(255, base.getGreen() + 20),
                    Math.min(255, base.getBlue() + 20));
            GradientPaint hull = new GradientPaint((int) bx, (int) by, hullColor, (int) bx, (int) by + height,
                    base.darker());
            g2.setPaint(hull);
            int[] hx = { cxb - width / 2 + 4, cxb - width / 2 + 16, cxb + width / 2 - 16, cxb + width / 2 - 4,
                    cxb + width / 2 - 4, cxb - width / 2 + 4 };
            int[] hy = { (int) by + 8, (int) by, (int) by, (int) by + 8, (int) by + height - 4, (int) by + height - 4 };
            g2.fillPolygon(hx, hy, 6);
            g2.setPaint(hull);
            g2.fillPolygon(new int[] { (int) bx, (int) bx - 22, (int) bx - 10, (int) bx + 16 },
                    new int[] { (int) by + 12, (int) by + height - 4, (int) by + height, (int) by + height }, 4);
            g2.fillPolygon(
                    new int[] { (int) bx + width, (int) bx + width + 22, (int) bx + width + 10, (int) bx + width - 16 },
                    new int[] { (int) by + 12, (int) by + height - 4, (int) by + height, (int) by + height }, 4);
            if (isApex) {
                g2.setPaint(null);
                g2.setColor(base.darker());
                g2.fillRect((int) bx - 36, (int) by + 18, 14, 8);
                g2.fillRect((int) bx + width + 22, (int) by + 18, 14, 8);
                g2.setColor(new Color(255, 160, 0, (int) (180 * pulse)));
                g2.fillOval((int) bx - 40, (int) by + 17, 8, 8);
                g2.fillOval((int) bx + width + 32, (int) by + 17, 8, 8);
            }
            g2.setPaint(null);
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
                // expanding ring
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
            if (boss.alive && !bossTransition) {
                double dx = boss.x + boss.width / 2 - x, dy = boss.y + boss.height / 2 - y;
                double len = Math.sqrt(dx * dx + dy * dy);
                if (len > 1) {
                    double spd = Math.sqrt(vx * vx + vy * vy), hm = 0.12;
                    vx = vx * (1 - hm) + (dx / len * spd) * hm;
                    vy = vy * (1 - hm) + (dy / len * spd) * hm;
                    double ns = Math.sqrt(vx * vx + vy * vy);
                    if (ns > 0) {
                        vx = vx / ns * spd;
                        vy = vy / ns * spd;
                    }
                }
            }
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