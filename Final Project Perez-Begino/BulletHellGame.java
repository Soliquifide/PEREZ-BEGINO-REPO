import javax.swing.*;
import javax.sound.midi.*;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class BulletHellGame extends JPanel
        implements ActionListener, KeyListener, MouseListener, MouseMotionListener {

    // ── Dimensions ───────────────────────────────────────────────────
    static final int WIDTH = 600;
    static final int HEIGHT = 800;

    // ── Game States ──────────────────────────────────────────────────
    static final int STATE_MENU = 0;
    static final int STATE_SETTINGS = 1;
    static final int STATE_DIFF_SEL = 2;
    static final int STATE_CLASS_SEL = 5; // NEW
    static final int STATE_PLAYING = 3;
    static final int STATE_GAME_OVER = 4;

    // ── Fire Modes ───────────────────────────────────────────────────
    static final int FIRE_MOUSE = 0;
    static final int FIRE_SPACE = 1;

    // ── PowerUp Types ────────────────────────────────────────────────
    static final int PU_SPEED = 0;
    static final int PU_DOUBLE_SHOT = 1;
    static final int PU_TRIPLE_SHOT = 2;
    static final int PU_SHIELD = 3;
    static final int PU_BOMB = 4;
    static final int PU_COUNT = 5;

    // ── Heat System Constants ─────────────────────────────────────────
    static final int MAX_HEAT = 100;
    static final int HEAT_PER_SHOT = 5;
    static final int HEAT_COOL_RATE = 1;
    static final int OVERHEAT_FRAMES = 120;
    static final int FIRE_RATE = 4;

    // ── Game state ───────────────────────────────────────────────────
    private int gameState = STATE_MENU;
    private int score = 0;
    private int wave = 1;
    private int frameCount = 0;
    private boolean bossTransition = false;

    // ── Settings ─────────────────────────────────────────────────────
    private int fireMode = FIRE_MOUSE;
    private boolean musicEnabled = true;
    private int musicVolPct = 75;
    private int difficulty = 1;

    // ── Input tracking ───────────────────────────────────────────────
    private boolean mouseFireHeld = false;
    private boolean draggingSlider = false;
    private final boolean[] keys = new boolean[256];

    // ── Mouse position (for aiming) ───────────────────────────────────
    private int mouseX = WIDTH / 2;
    private int mouseY = 0;

    // ── Heat / Overheat ───────────────────────────────────────────────
    private int heat = 0;
    private boolean overheated = false;
    private int overheatTimer = 0;
    private boolean firingThisFrame = false;

    // ── PowerUp timers ───────────────────────────────────────────────
    private boolean hasShield = false;
    private int shieldTimer = 0;
    private boolean doubleShot = false;
    private int doubleShotTimer = 0;
    private boolean tripleShot = false;
    private int tripleShotTimer = 0;
    private boolean speedBoosted = false;
    private int speedBoostTimer = 0;

    // ── Pickup notification ───────────────────────────────────────────
    private String pickupMsg = "";
    private int pickupTimer = 0;

    // ── Game objects ─────────────────────────────────────────────────
    private Player player;
    private Boss boss;
    private final ArrayList<Bullet> playerBullets = new ArrayList<>();
    private final ArrayList<Bullet> enemyBullets = new ArrayList<>();
    private final ArrayList<PowerUp> powerUps = new ArrayList<>();
    private final Random rand = new Random();

    // ── MIDI ─────────────────────────────────────────────────────────
    private Sequencer sequencer;
    private Synthesizer synth;

    // ── Static star field ─────────────────────────────────────────────
    private final int[] starX = new int[120];
    private final int[] starY = new int[120];
    private final int[] starSz = new int[120];

    private Timer gameTimer;

    // ── Main-menu buttons ────────────────────────────────────────────
    private final Rectangle btnStart = new Rectangle(WIDTH / 2 - 110, 360, 220, 54);
    private final Rectangle btnSettings = new Rectangle(WIDTH / 2 - 110, 430, 220, 54);
    private final Rectangle btnQuit = new Rectangle(WIDTH / 2 - 110, 500, 220, 54);

    // ── Settings buttons + slider ─────────────────────────────────────
    private final Rectangle btnFireMouse = new Rectangle(WIDTH / 2 - 120, 175, 240, 46);
    private final Rectangle btnFireSpace = new Rectangle(WIDTH / 2 - 120, 231, 240, 46);
    private final Rectangle btnMusicToggle = new Rectangle(WIDTH / 2 - 120, 375, 240, 46);
    private final Rectangle sliderTrack = new Rectangle(WIDTH / 2 - 110, 440, 220, 18);
    private final Rectangle btnSettBack = new Rectangle(WIDTH / 2 - 100, 700, 200, 50);

    // ── Difficulty-select buttons ─────────────────────────────────────
    private final Rectangle btnDiffEasy = new Rectangle(WIDTH / 2 - 120, 300, 240, 80);
    private final Rectangle btnDiffNormal = new Rectangle(WIDTH / 2 - 120, 400, 240, 80);
    private final Rectangle btnDiffHard = new Rectangle(WIDTH / 2 - 120, 500, 240, 80);
    private final Rectangle btnDiffBack = new Rectangle(WIDTH / 2 - 100, 640, 200, 50);

    // ── Class-select buttons ──────────────────────────────────────────
    private final Rectangle btnClassMachineGunner = new Rectangle(WIDTH / 2 - 150, 280, 300, 220);
    private final Rectangle btnClassBack = new Rectangle(WIDTH / 2 - 100, 650, 200, 50);

    // ── NEW: Vibration / Screen-shake state ──────────────────────────
    private int shakeTimer = 0;
    private int shakeIntensity = 0;

    // ── NEW: Sound (spacegun) ─────────────────────────────────────────
    private SourceDataLine soundLine;
    private int soundCooldown = 0; // prevents sound spam every 4 frames

    // ═════════════════════════════════════════════════════════════════
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

        initMusic();
        initSound();
        gameTimer = new Timer(16, this);
        gameTimer.start();
    }

    // ═════════════════════════════════════════════════════════════════
    // SPACEGUN SOUND (NEW)
    // Synthesises a sci-fi "pew" by mixing a short frequency-swept sine
    // with noise — all in software, no external files needed.
    // ═════════════════════════════════════════════════════════════════
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

    /**
     * Plays a short "spacegun pew" asynchronously so it never blocks the game loop.
     */
    private void playSpacegunSound() {
        if (soundLine == null)
            return;
        new Thread(() -> {
            try {
                int sampleRate = 44100;
                // Very short — 80 ms
                int samples = sampleRate * 80 / 1000;
                byte[] buf = new byte[samples * 2];
                for (int i = 0; i < samples; i++) {
                    double t = (double) i / sampleRate;
                    // Frequency sweeps rapidly from 1400 Hz down to 300 Hz
                    double freq = 1400 - 13750 * t;
                    if (freq < 80)
                        freq = 80;
                    double sine = Math.sin(2 * Math.PI * freq * t);
                    // Add a thin layer of noise for "laser grit"
                    double noise = (Math.random() * 2 - 1) * 0.18;
                    // Envelope: fast attack, exponential decay
                    double env = Math.exp(-t * 38.0);
                    double sample = (sine * 0.82 + noise) * env;
                    short s = (short) Math.max(-32768, Math.min(32767, (int) (sample * 22000)));
                    buf[i * 2] = (byte) (s & 0xFF);
                    buf[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
                }
                soundLine.write(buf, 0, buf.length);
            } catch (Exception ignored) {
            }
        }, "sfx-spacegun").start();
    }

    // ═════════════════════════════════════════════════════════════════
    // MIDI MUSIC (unchanged)
    // ═════════════════════════════════════════════════════════════════
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
        int[] mel = { 72, 74, 76, 79, 79, 76, 74, 72, 74, 76, 77, 81, 79, 77, 76, 74,
                72, 76, 79, 84, 83, 81, 79, 77, 76, 74, 72, 71, 72, 0, 0, 0 };
        int[] mDur = { 12, 12, 12, 18, 6, 12, 12, 12, 12, 12, 12, 18, 6, 12, 12, 12,
                12, 12, 12, 18, 6, 12, 12, 12, 12, 12, 12, 12, 24, 24, 24, 24 };
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
        int[] hm = { 60, 62, 64, 67, 67, 64, 62, 60, 62, 64, 65, 69, 67, 65, 64, 62,
                60, 64, 67, 72, 71, 69, 67, 65, 64, 62, 60, 59, 60, 0, 0, 0 };
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

    // ═════════════════════════════════════════════════════════════════
    // GAME LOOP
    // ═════════════════════════════════════════════════════════════════
    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameState == STATE_PLAYING)
            update();
        repaint();
    }

    private void update() {
        frameCount++;

        // ── Shake timer countdown ─────────────────────────────────────
        if (shakeTimer > 0)
            shakeTimer--;
        if (soundCooldown > 0)
            soundCooldown--;

        // ── Determine if player wants to fire ────────────────────────
        boolean wantsFire = (fireMode == FIRE_SPACE && keys[KeyEvent.VK_SPACE])
                || (fireMode == FIRE_MOUSE && mouseFireHeld);

        // ── Movement — slowed while firing ───────────────────────────
        firingThisFrame = wantsFire && !overheated;
        int spd = player.speed + (speedBoosted ? 3 : 0);
        if (firingThisFrame)
            spd = Math.max(2, (int) (spd * 0.55));

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

        // ── Heat / Overheat logic ─────────────────────────────────────
        if (overheated) {
            if (--overheatTimer <= 0) {
                overheated = false;
                heat = 0;
            }
        } else {
            if (!wantsFire)
                heat = Math.max(0, heat - HEAT_COOL_RATE);
        }

        // ── Power-up timers ───────────────────────────────────────────
        if (shieldTimer > 0 && --shieldTimer == 0)
            hasShield = false;
        if (doubleShotTimer > 0 && --doubleShotTimer == 0)
            doubleShot = false;
        if (tripleShotTimer > 0 && --tripleShotTimer == 0)
            tripleShot = false;
        if (speedBoostTimer > 0 && --speedBoostTimer == 0)
            speedBoosted = false;
        if (pickupTimer > 0)
            pickupTimer--;

        // ── Firing ───────────────────────────────────────────────────
        if (!overheated && wantsFire && frameCount % FIRE_RATE == 0) {
            int pcx = player.x + player.size / 2;
            int pcy = player.y + player.size / 2;
            double dx = mouseX - pcx;
            double dy = mouseY - pcy;
            double len = Math.sqrt(dx * dx + dy * dy);
            double bulletSpeed = 14.0;
            double bvx = 0, bvy = -bulletSpeed;
            if (len > 1) {
                bvx = (dx / len) * bulletSpeed;
                bvy = (dy / len) * bulletSpeed;
            }

            int bx = pcx, by = pcy;
            if (tripleShot) {
                double perpX = -bvy / bulletSpeed;
                double perpY = bvx / bulletSpeed;
                playerBullets.add(new Bullet(bx, by, bvx, bvy, Color.CYAN, false));
                playerBullets.add(new Bullet(bx + perpX * 10, by + perpY * 10, bvx * 0.92 + perpX * 2.5,
                        bvy * 0.92 + perpY * 2.5, Color.CYAN, false));
                playerBullets.add(new Bullet(bx - perpX * 10, by - perpY * 10, bvx * 0.92 - perpX * 2.5,
                        bvy * 0.92 - perpY * 2.5, Color.CYAN, false));
                heat += HEAT_PER_SHOT * 3;
            } else if (doubleShot) {
                double perpX = -bvy / bulletSpeed;
                double perpY = bvx / bulletSpeed;
                playerBullets.add(new Bullet(bx + perpX * 8, by + perpY * 8, bvx, bvy, Color.CYAN, false));
                playerBullets.add(new Bullet(bx - perpX * 8, by - perpY * 8, bvx, bvy, Color.CYAN, false));
                heat += HEAT_PER_SHOT * 2;
            } else {
                playerBullets.add(new Bullet(bx, by, bvx, bvy, Color.CYAN, false));
                heat += HEAT_PER_SHOT;
            }

            // ── NEW: Spacegun sound + screen vibration ────────────────
            if (soundCooldown == 0) {
                playSpacegunSound();
                soundCooldown = FIRE_RATE; // one sound per burst window
            }
            // Small shake: intensity 2, lasts 3 frames — rapid bursts stack briefly
            shakeTimer = Math.min(shakeTimer + 3, 6);
            shakeIntensity = 2;

            if (heat >= MAX_HEAT) {
                heat = MAX_HEAT;
                overheated = true;
                overheatTimer = OVERHEAT_FRAMES;
                // Bigger shake on overheat
                shakeTimer = 18;
                shakeIntensity = 5;
                pickupMsg = "OVERHEATED!";
                pickupTimer = OVERHEAT_FRAMES;
            }
        }

        // ── Timed power-up drop ───────────────────────────────────────
        if (frameCount % 360 == 0 && !bossTransition) {
            int type = (frameCount / 360) % PU_COUNT;
            boolean alreadyActive = (type == PU_SPEED && speedBoosted)
                    || (type == PU_DOUBLE_SHOT && doubleShot)
                    || (type == PU_TRIPLE_SHOT && tripleShot)
                    || (type == PU_SHIELD && hasShield);
            if (!alreadyActive)
                powerUps.add(new PowerUp(rand.nextInt(WIDTH - 80) + 40, -60, type));
        }

        // ── Power-up movement & collection ────────────────────────────
        for (int i = powerUps.size() - 1; i >= 0; i--) {
            PowerUp p = powerUps.get(i);
            p.update();
            if (p.y > HEIGHT + 30) {
                powerUps.remove(i);
                continue;
            }
            int pcx = player.x + player.size / 2, pcy = player.y + player.size / 2;
            Rectangle pBox = new Rectangle(pcx - 20, pcy - 20, 40, 40);
            if (player.alive && p.getBounds().intersects(pBox)) {
                applyPowerUp(p.type);
                powerUps.remove(i);
            }
        }

        // ── Boss update ───────────────────────────────────────────────
        if (!bossTransition) {
            boss.update(frameCount);
            spawnBossPattern();
        }

        // ── Player bullets vs boss ────────────────────────────────────
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
                    int dropType = rand.nextInt(PU_COUNT);
                    boolean dupActive = (dropType == PU_SPEED && speedBoosted)
                            || (dropType == PU_DOUBLE_SHOT && doubleShot)
                            || (dropType == PU_TRIPLE_SHOT && tripleShot)
                            || (dropType == PU_SHIELD && hasShield);
                    if (!dupActive)
                        powerUps.add(new PowerUp(boss.x + boss.width / 2, boss.y + boss.height, dropType));
                }
                if (boss.hp <= 0) {
                    boss.alive = false;
                    score += 500;
                    wave++;
                    bossTransition = true;
                    playerBullets.clear();
                    enemyBullets.clear();
                    Timer t = new Timer(1800, ev -> {
                        boss = new Boss(WIDTH / 2 - 40, 60, wave);
                        bossTransition = false;
                    });
                    t.setRepeats(false);
                    t.start();
                    break;
                }
            }
        }

        // ── Enemy bullets vs player ───────────────────────────────────
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
                    gameState = STATE_GAME_OVER;
                } else
                    enemyBullets.clear();
                break;
            }
        }
    }

    // ── Boss patterns (unchanged) ─────────────────────────────────────
    private void spawnBossPattern() {
        if (!boss.alive)
            return;
        int cx = boss.x + boss.width / 2, cy = boss.y + boss.height;
        double dm = new double[] { 0.7, 1.0, 1.5 }[difficulty];

        if (frameCount % 60 == 0) {
            int cnt = 12 + wave * 2;
            for (int i = 0; i < cnt; i++) {
                double a = 2 * Math.PI * i / cnt, s = (3.0 + wave * 0.3) * dm;
                enemyBullets.add(new Bullet(cx, cy, Math.cos(a) * s, Math.sin(a) * s, Color.RED, true));
            }
        }
        if (wave >= 2 && frameCount % 20 == 0) {
            double dx = player.x - cx, dy2 = player.y - cy, len = Math.sqrt(dx * dx + dy2 * dy2);
            if (len > 0) {
                double s = 4.5 * dm;
                enemyBullets.add(new Bullet(cx, cy, dx / len * s, dy2 / len * s, Color.ORANGE, true));
            }
        }
        if (wave >= 3 && frameCount % 5 == 0) {
            double a = Math.toRadians(frameCount * 7), s = 3.5 * dm;
            enemyBullets.add(new Bullet(cx, cy, Math.cos(a) * s, Math.sin(a) * s, Color.MAGENTA, true));
            enemyBullets.add(new Bullet(cx, cy, -Math.cos(a) * s, -Math.sin(a) * s, Color.MAGENTA, true));
        }
        if (wave >= 4 && frameCount % 12 == 0) {
            for (int i = 0; i < 4; i++) {
                double a = Math.PI / 2 * i + Math.toRadians(frameCount * 3);
                enemyBullets.add(
                        new Bullet(cx, cy, Math.cos(a) * 4 * dm, Math.sin(a) * 4 * dm, new Color(255, 80, 0), true));
            }
        }
        if (wave >= 5 && frameCount % 40 == 0) {
            for (int i = 0; i < 20; i++) {
                double a = 2 * Math.PI * i / 20 + Math.toRadians(frameCount);
                enemyBullets.add(new Bullet(cx, cy, Math.cos(a) * 5 * dm, Math.sin(a) * 5 * dm, Color.YELLOW, true));
            }
        }
    }

    private void applyPowerUp(int type) {
        switch (type) {
            case PU_SPEED:
                speedBoosted = true;
                speedBoostTimer = 360;
                pickupMsg = "SPEED BOOST!";
                break;
            case PU_DOUBLE_SHOT:
                doubleShot = true;
                doubleShotTimer = 480;
                tripleShot = false;
                tripleShotTimer = 0;
                pickupMsg = "DOUBLE SHOT!";
                break;
            case PU_TRIPLE_SHOT:
                tripleShot = true;
                tripleShotTimer = 480;
                doubleShot = false;
                doubleShotTimer = 0;
                pickupMsg = "TRIPLE SHOT!";
                break;
            case PU_SHIELD:
                hasShield = true;
                shieldTimer = 600;
                pickupMsg = "SHIELD ON!";
                break;
            case PU_BOMB:
                enemyBullets.clear();
                score += 100;
                pickupMsg = "BOMB! +100";
                break;
        }
        pickupTimer = 120;
    }

    // ═════════════════════════════════════════════════════════════════
    // RENDERING
    // ═════════════════════════════════════════════════════════════════
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // ── NEW: Apply screen-shake transform while firing ────────────
        int shakeX = 0, shakeY = 0;
        if (shakeTimer > 0 && gameState == STATE_PLAYING) {
            shakeX = rand.nextInt(shakeIntensity * 2 + 1) - shakeIntensity;
            shakeY = rand.nextInt(shakeIntensity * 2 + 1) - shakeIntensity;
        }
        g2.translate(shakeX, shakeY);

        g2.setColor(new Color(6, 6, 22));
        g2.fillRect(-10, -10, WIDTH + 20, HEIGHT + 20); // slightly larger to cover shake edges
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
                break; // NEW
            case STATE_PLAYING:
                drawGame(g2);
                break;
            case STATE_GAME_OVER:
                drawGame(g2);
                drawGameOver(g2);
                break;
        }

        // Reset transform
        g2.translate(-shakeX, -shakeY);
    }

    private void drawStarfield(Graphics2D g2) {
        if (gameState == STATE_PLAYING || gameState == STATE_GAME_OVER) {
            rand.setSeed(42);
            for (int i = 0; i < 90; i++) {
                int sx = rand.nextInt(WIDTH), sy = (rand.nextInt(HEIGHT) + frameCount * (i % 3 + 1)) % HEIGHT;
                int br = 140 + rand.nextInt(115);
                g2.setColor(new Color(br, br, br));
                g2.fillRect(sx, sy, 1, 1);
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

    // ── Main menu (unchanged) ──────────────────────────────────────────
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
        int sx = (int) ((frameCount * 2) % (WIDTH + 80)) - 40;
        g2.setColor(Color.CYAN);
        g2.fillPolygon(new int[] { sx + 7, sx, sx + 14 }, new int[] { 298, 312, 312 }, 3);
        drawBtn(g2, btnStart, "START", true);
        drawBtn(g2, btnSettings, "SETTINGS", true);
        drawBtn(g2, btnQuit, "QUIT", true);
        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        g2.setColor(new Color(80, 80, 120));
        g2.drawString("v3.2", WIDTH - 40, HEIGHT - 10);
    }

    // ── Settings (unchanged) ──────────────────────────────────────────
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

    // ── Difficulty select (unchanged) ─────────────────────────────────
    private void drawDiffSelect(Graphics2D g2) {
        centeredTitle(g2, "SELECT DIFFICULTY", 100);
        divider(g2, 112);
        g2.setFont(new Font("Arial", Font.ITALIC, 14));
        g2.setColor(new Color(160, 160, 230));
        String sub = "Choose your challenge — click to start!";
        g2.drawString(sub, WIDTH / 2 - g2.getFontMetrics().stringWidth(sub) / 2, 150);
        drawDiffBtn(g2, btnDiffEasy, "EASY", new Color(60, 200, 80), "Slower bullets", "5 lives to spend",
                difficulty == 0);
        drawDiffBtn(g2, btnDiffNormal, "NORMAL", new Color(0, 180, 255), "Standard pace", "3 lives", difficulty == 1);
        drawDiffBtn(g2, btnDiffHard, "HARD", new Color(255, 80, 60), "Faster bullets", "Only 2 lives", difficulty == 2);
        drawBtn(g2, btnDiffBack, "BACK", true);
    }

    private void drawDiffBtn(Graphics2D g2, Rectangle r, String title, Color accent,
            String line1, String line2, boolean selected) {
        if (selected) {
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 40));
            g2.fillRoundRect(r.x - 5, r.y - 5, r.width + 10, r.height + 10, 16, 16);
        }
        g2.setColor(selected ? new Color(15, 20, 60) : new Color(10, 10, 30));
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 12, 12);
        g2.setColor(accent);
        g2.setStroke(new BasicStroke(selected ? 2.5f : 1.2f));
        g2.drawRoundRect(r.x, r.y, r.width, r.height, 12, 12);
        g2.setStroke(new BasicStroke(1));
        g2.setFont(new Font("Arial", Font.BOLD, 20));
        g2.setColor(selected ? Color.WHITE : new Color(180, 180, 220));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(title, r.x + r.width / 2 - fm.stringWidth(title) / 2, r.y + 28);
        g2.setFont(new Font("Arial", Font.PLAIN, 13));
        g2.setColor(selected ? new Color(210, 230, 255) : new Color(110, 110, 160));
        g2.drawString("• " + line1, r.x + 20, r.y + 50);
        g2.drawString("• " + line2, r.x + 20, r.y + 68);
    }

    // ─────────────────────────────────────────────────────────────────
    // NEW: Class Selection Screen
    // ─────────────────────────────────────────────────────────────────
    private void drawClassSelect(Graphics2D g2) {
        // Title
        centeredTitle(g2, "SELECT CLASS", 85);
        divider(g2, 97);

        g2.setFont(new Font("Arial", Font.ITALIC, 14));
        g2.setColor(new Color(160, 160, 230));
        String sub = "Choose your pilot — more classes coming soon!";
        g2.drawString(sub, WIDTH / 2 - g2.getFontMetrics().stringWidth(sub) / 2, 130);

        // ── Machine Gunner card ───────────────────────────────────────
        Rectangle r = btnClassMachineGunner;
        Color accent = new Color(0, 220, 255);

        // Glow
        g2.setColor(new Color(0, 200, 255, 28));
        g2.fillRoundRect(r.x - 8, r.y - 8, r.width + 16, r.height + 16, 22, 22);

        // Card body
        g2.setColor(new Color(8, 18, 55));
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 14, 14);

        // Accent border
        g2.setColor(accent);
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawRoundRect(r.x, r.y, r.width, r.height, 14, 14);
        g2.setStroke(new BasicStroke(1));

        // SELECTED badge
        g2.setColor(accent);
        g2.fillRoundRect(r.x + r.width - 88, r.y + 10, 78, 22, 8, 8);
        g2.setFont(new Font("Arial", Font.BOLD, 11));
        g2.setColor(new Color(6, 6, 22));
        g2.drawString("SELECTED", r.x + r.width - 84, r.y + 25);

        // Ship icon (scaled-up mini-ship)
        int sx = r.x + 44, sy = r.y + 26;
        g2.setColor(Color.CYAN);
        g2.fillPolygon(new int[] { sx + 16, sx, sx + 32 }, new int[] { sy, sy + 32, sy + 32 }, 3);
        g2.setColor(new Color(200, 255, 255));
        g2.fillOval(sx + 8, sy + 9, 16, 16);
        // Engine glow
        g2.setColor(new Color(0, 100, 255, 120));
        g2.fillOval(sx + 8, sy + 28, 16, 12);

        // Class name
        g2.setFont(new Font("Arial", Font.BOLD, 26));
        g2.setColor(Color.WHITE);
        g2.drawString("MACHINE GUNNER", r.x + 88, r.y + 46);

        // Subtitle / flavour
        g2.setFont(new Font("Arial", Font.ITALIC, 12));
        g2.setColor(new Color(160, 200, 255));
        g2.drawString("Spray and pray — overheat at your own risk", r.x + 88, r.y + 66);

        // Divider inside card
        g2.setColor(new Color(0, 160, 255, 50));
        g2.fillRect(r.x + 14, r.y + 78, r.width - 28, 1);

        // Stats
        int sy2 = r.y + 100;
        drawStatRow(g2, r.x + 20, sy2, "FIRE RATE", 5, 5, new Color(0, 220, 255));
        drawStatRow(g2, r.x + 20, sy2 + 34, "HEAT BUILD", 4, 5, new Color(255, 140, 0));
        drawStatRow(g2, r.x + 20, sy2 + 68, "SPEED", 3, 5, new Color(80, 255, 80));
        drawStatRow(g2, r.x + 20, sy2 + 102, "DEFENSE", 2, 5, new Color(180, 100, 255));

        // "More coming soon" placeholder
        g2.setColor(new Color(30, 30, 70));
        g2.fillRoundRect(r.x, r.y + r.height + 20, r.width, 60, 14, 14);
        g2.setColor(new Color(50, 50, 100));
        g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                0, new float[] { 6, 6 }, 0));
        g2.drawRoundRect(r.x, r.y + r.height + 20, r.width, 60, 14, 14);
        g2.setStroke(new BasicStroke(1));
        g2.setFont(new Font("Arial", Font.ITALIC, 13));
        g2.setColor(new Color(80, 80, 130));
        String coming = "More classes coming soon...";
        g2.drawString(coming, WIDTH / 2 - g2.getFontMetrics().stringWidth(coming) / 2, r.y + r.height + 57);

        drawBtn(g2, btnClassBack, "BACK", true);

        // "Press Enter or click card" hint
        g2.setFont(new Font("Arial", Font.PLAIN, 11));
        g2.setColor(new Color(90, 90, 150));
        String hint = "Click the card or press ENTER to begin";
        g2.drawString(hint, WIDTH / 2 - g2.getFontMetrics().stringWidth(hint) / 2, 632);
    }

    /** Draws a labelled pip-bar stat row inside the class card. */
    private void drawStatRow(Graphics2D g2, int x, int y, String label, int value, int max, Color pipColor) {
        g2.setFont(new Font("Arial", Font.BOLD, 11));
        g2.setColor(new Color(120, 160, 220));
        g2.drawString(label, x, y + 13);
        int pipW = 22, pipH = 12, gap = 5, startX = x + 110;
        for (int i = 0; i < max; i++) {
            boolean filled = i < value;
            g2.setColor(filled ? pipColor : new Color(20, 20, 50));
            g2.fillRoundRect(startX + i * (pipW + gap), y, pipW, pipH, 4, 4);
            if (filled) {
                g2.setColor(new Color(255, 255, 255, 60));
                g2.fillRoundRect(startX + i * (pipW + gap), y, pipW, pipH / 2, 4, 4);
            }
            g2.setColor(filled ? pipColor.darker() : new Color(35, 35, 70));
            g2.setStroke(new BasicStroke(0.8f));
            g2.drawRoundRect(startX + i * (pipW + gap), y, pipW, pipH, 4, 4);
            g2.setStroke(new BasicStroke(1));
        }
    }

    // ── In-game HUD (unchanged except shake is handled in paintComponent) ──
    private void drawGame(Graphics2D g2) {
        if (boss.alive)
            boss.draw(g2);
        for (PowerUp p : powerUps)
            p.draw(g2);
        for (Bullet b : playerBullets)
            b.draw(g2);
        for (Bullet b : enemyBullets)
            b.draw(g2);
        if (player.alive)
            player.draw(g2);

        if (player.alive && !overheated) {
            int pcx = player.x + player.size / 2, pcy = player.y + player.size / 2;
            g2.setColor(new Color(0, 200, 255, 35));
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[] { 6f, 8f },
                    frameCount * 0.5f));
            g2.drawLine(pcx, pcy, mouseX, mouseY);
            g2.setStroke(new BasicStroke(1));
            g2.setColor(new Color(0, 220, 255, 120));
            g2.drawOval(mouseX - 8, mouseY - 8, 16, 16);
            g2.drawLine(mouseX - 12, mouseY, mouseX - 4, mouseY);
            g2.drawLine(mouseX + 4, mouseY, mouseX + 12, mouseY);
            g2.drawLine(mouseX, mouseY - 12, mouseX, mouseY - 4);
            g2.drawLine(mouseX, mouseY + 4, mouseX, mouseY + 12);
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

        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.setColor(Color.WHITE);
        g2.drawString("SCORE: " + score, 10, 24);
        g2.drawString("WAVE: " + wave, WIDTH - 100, 24);
        String[] dnames = { "EASY", "NORMAL", "HARD" };
        g2.setFont(new Font("Arial", Font.PLAIN, 11));
        g2.setColor(new Color(120, 120, 180));
        g2.drawString(dnames[difficulty], WIDTH / 2 - 20, 24);

        for (int i = 0; i < player.lives; i++)
            drawMiniShip(g2, 10 + i * 22, HEIGHT - 32);
        drawActivePowerupIcons(g2);
        drawHeatBar(g2);

        if (pickupTimer > 0) {
            float alpha = Math.min(1f, pickupTimer / 30f);
            g2.setFont(new Font("Arial", Font.BOLD, 22));
            FontMetrics fm = g2.getFontMetrics();
            int msgX = WIDTH / 2 - fm.stringWidth(pickupMsg) / 2;
            Color msgColor = overheated && pickupMsg.equals("OVERHEATED!")
                    ? new Color(255, 80, 40, (int) (alpha * 230))
                    : new Color(255, 230, 80, (int) (alpha * 230));
            g2.setColor(new Color(0, 0, 0, (int) (alpha * 130)));
            g2.fillRoundRect(msgX - 10, HEIGHT / 2 - 50, fm.stringWidth(pickupMsg) + 20, 34, 10, 10);
            g2.setColor(msgColor);
            g2.drawString(pickupMsg, msgX, HEIGHT / 2 - 26);
        }

        if (boss.alive) {
            int bw = 300, bx = WIDTH / 2 - 150;
            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(bx, HEIGHT - 26, bw, 13);
            double pct = (double) boss.hp / boss.maxHp;
            g2.setColor(pct > 0.5 ? Color.RED : pct > 0.25 ? Color.ORANGE : Color.YELLOW);
            g2.fillRect(bx, HEIGHT - 26, (int) (pct * bw), 13);
            g2.setColor(Color.WHITE);
            g2.drawRect(bx, HEIGHT - 26, bw, 13);
            g2.setFont(new Font("Arial", Font.BOLD, 11));
            g2.drawString("BOSS  HP", bx + bw / 2 - 27, HEIGHT - 14);
        }

        drawPowerupLegend(g2);

        if (bossTransition) {
            g2.setColor(new Color(0, 0, 0, 170));
            g2.fillRect(0, 0, WIDTH, HEIGHT);
            g2.setColor(Color.YELLOW);
            g2.setFont(new Font("Arial", Font.BOLD, 46));
            String wt = "WAVE " + wave + "!";
            g2.drawString(wt, WIDTH / 2 - g2.getFontMetrics().stringWidth(wt) / 2, HEIGHT / 2);
            g2.setFont(new Font("Arial", Font.PLAIN, 20));
            g2.setColor(Color.WHITE);
            g2.drawString("Brace yourself...", WIDTH / 2 - 72, HEIGHT / 2 + 40);
        }
    }

    private void drawHeatBar(Graphics2D g2) {
        int barW = 160, barH = 14, barX = WIDTH / 2 - barW / 2, barY = HEIGHT - 52;
        g2.setColor(new Color(10, 10, 30));
        g2.fillRoundRect(barX, barY, barW, barH, 7, 7);
        float heatFrac = (float) heat / MAX_HEAT;
        Color barColor;
        if (overheated) {
            int flash = (int) (Math.abs(Math.sin(frameCount * 0.18)) * 200) + 55;
            barColor = new Color(flash, 0, 0);
        } else if (heatFrac < 0.5f) {
            barColor = new Color((int) (heatFrac * 2 * 255), 220, 0);
        } else {
            barColor = new Color(255, (int) ((1f - (heatFrac - 0.5f) * 2) * 180), 0);
        }
        int fillW = (int) (barW * heatFrac);
        if (fillW > 0) {
            g2.setColor(barColor);
            g2.fillRoundRect(barX, barY, fillW, barH, 7, 7);
        }
        g2.setStroke(new BasicStroke(overheated ? 2f : 1.2f));
        g2.setColor(overheated ? new Color(255, 80, 0) : new Color(80, 80, 140));
        g2.drawRoundRect(barX, barY, barW, barH, 7, 7);
        g2.setStroke(new BasicStroke(1));
        g2.setFont(new Font("Arial", Font.BOLD, 10));
        g2.setColor(overheated ? new Color(255, 100, 0) : new Color(160, 160, 220));
        String label = overheated ? "COOLING DOWN..." : "HEAT";
        g2.drawString(label, barX + barW / 2 - g2.getFontMetrics().stringWidth(label) / 2, barY - 2);
        if (overheated) {
            float recoverFrac = 1f - (float) overheatTimer / OVERHEAT_FRAMES;
            int recW = (int) (barW * recoverFrac);
            g2.setColor(new Color(0, 160, 255, 120));
            g2.fillRoundRect(barX, barY + barH + 2, recW, 4, 3, 3);
        }
    }

    private void drawActivePowerupIcons(Graphics2D g2) {
        int[][] pus = { { PU_SPEED, speedBoosted ? 1 : 0, speedBoostTimer, 360 },
                { PU_DOUBLE_SHOT, doubleShot ? 1 : 0, doubleShotTimer, 480 },
                { PU_TRIPLE_SHOT, tripleShot ? 1 : 0, tripleShotTimer, 480 },
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
        int lx = WIDTH - 134, ly = HEIGHT - 85;
        g2.setFont(new Font("Arial", Font.PLAIN, 9));
        g2.setColor(new Color(80, 80, 130));
        g2.drawString("POWERUPS:", lx, ly);
        g2.drawString("SP=Speed Boost", lx, ly + 12);
        g2.drawString("2x=Double Shot", lx, ly + 24);
        g2.drawString("3x=Triple Shot", lx, ly + 36);
        g2.drawString("SH=Shield", lx, ly + 48);
        g2.drawString("BM=Bomb (instant)", lx, ly + 60);
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

    // ── Shared helpers (unchanged) ────────────────────────────────────
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

    // ═════════════════════════════════════════════════════════════════
    // GAME MANAGEMENT
    // ═════════════════════════════════════════════════════════════════
    private void startGame() {
        score = 0;
        wave = 1;
        frameCount = 0;
        bossTransition = false;
        heat = 0;
        overheated = false;
        overheatTimer = 0;
        hasShield = false;
        shieldTimer = 0;
        doubleShot = false;
        doubleShotTimer = 0;
        tripleShot = false;
        tripleShotTimer = 0;
        speedBoosted = false;
        speedBoostTimer = 0;
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
        gameState = STATE_PLAYING;
    }

    // ═════════════════════════════════════════════════════════════════
    // INPUT
    // ═════════════════════════════════════════════════════════════════
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
        // NEW: Enter on class select starts game
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
            } // → class select
            else if (btnDiffNormal.contains(p)) {
                difficulty = 1;
                gameState = STATE_CLASS_SEL;
            } else if (btnDiffHard.contains(p)) {
                difficulty = 2;
                gameState = STATE_CLASS_SEL;
            } else if (btnDiffBack.contains(p))
                gameState = STATE_MENU;
        } else if (gameState == STATE_CLASS_SEL) { // NEW
            if (btnClassMachineGunner.contains(p))
                startGame(); // only one class → go directly
            else if (btnClassBack.contains(p))
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

    // ═════════════════════════════════════════════════════════════════
    // POWERUP HELPERS (unchanged)
    // ═════════════════════════════════════════════════════════════════
    static Color puColor(int type) {
        switch (type) {
            case PU_SPEED:
                return new Color(255, 220, 0);
            case PU_DOUBLE_SHOT:
                return new Color(0, 220, 255);
            case PU_TRIPLE_SHOT:
                return new Color(80, 255, 80);
            case PU_SHIELD:
                return new Color(80, 120, 255);
            case PU_BOMB:
                return new Color(255, 130, 0);
            default:
                return Color.WHITE;
        }
    }

    static String puLabel(int type) {
        switch (type) {
            case PU_SPEED:
                return "SP";
            case PU_DOUBLE_SHOT:
                return "2x";
            case PU_TRIPLE_SHOT:
                return "3x";
            case PU_SHIELD:
                return "SH";
            case PU_BOMB:
                return "BM";
            default:
                return "?";
        }
    }

    static String puFullName(int type) {
        switch (type) {
            case PU_SPEED:
                return "SPEED";
            case PU_DOUBLE_SHOT:
                return "DOUBLE";
            case PU_TRIPLE_SHOT:
                return "TRIPLE";
            case PU_SHIELD:
                return "SHIELD";
            case PU_BOMB:
                return "BOMB";
            default:
                return "???";
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // INNER CLASSES (unchanged)
    // ═════════════════════════════════════════════════════════════════
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
            int glowAlpha = firingThisFrame ? (int) (120 + 80 * Math.abs(Math.sin(frameCount * 0.5))) : 120;
            g2.setColor(new Color(0, 120, 255, glowAlpha));
            g2.fillOval(x + size / 2 - 6, y + size - 4, 12, 10);
            g2.setColor(Color.CYAN);
            g2.fillPolygon(new int[] { x + size / 2, x, x + size }, new int[] { y, y + size, y + size }, 3);
            g2.setColor(new Color(200, 255, 255));
            g2.fillOval(x + size / 2 - 4, y + 7, 8, 8);
            if (overheated) {
                g2.setColor(new Color(255, 80, 0, (int) (80 * Math.abs(Math.sin(frameCount * 0.18)))));
                g2.fillOval(x, y, size, size);
            }
        }
    }

    class Boss {
        int x, y, width = 80, height = 50, hp, maxHp, moveDir = 1, speed = 2;
        boolean alive = true;
        Color color;

        Boss(int x, int y, int wave) {
            this.x = x;
            this.y = y;
            maxHp = hp = 10 + wave * 30;
            speed = 2 + wave / 2;
            color = new Color(Math.max(20, 200 - wave * 20), 40, Math.min(255, 60 + wave * 15));
        }

        void update(int frame) {
            x += speed * moveDir;
            if (x <= 0 || x + width >= WIDTH)
                moveDir *= -1;
        }

        Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }

        void draw(Graphics2D g2) {
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
            g2.fillRoundRect(x - 5, y - 5, width + 10, height + 10, 18, 18);
            g2.setColor(color);
            g2.fillRoundRect(x, y, width, height, 12, 12);
            g2.setColor(Color.YELLOW);
            g2.fillOval(x + 14, y + 13, 15, 15);
            g2.fillOval(x + width - 29, y + 13, 15, 15);
            g2.setColor(Color.BLACK);
            g2.fillOval(x + 18, y + 17, 7, 7);
            g2.fillOval(x + width - 25, y + 17, 7, 7);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(x, y, width, height, 12, 12);
            g2.setStroke(new BasicStroke(1));
        }
    }

    class Bullet {
        double x, y, dx, dy;
        int size;
        Color color;

        Bullet(double x, double y, double dx, double dy, Color c, boolean enemy) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
            color = c;
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
            g2.setFont(new Font("Arial", Font.PLAIN, 9));
            fm = g2.getFontMetrics();
            String full = puFullName(type);
            g2.setColor(new Color(255, 255, 255, (int) (200 * pulse)));
            g2.drawString(full, (int) x - fm.stringWidth(full) / 2, (int) y + 28);
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // MAIN
    // ═════════════════════════════════════════════════════════════════
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