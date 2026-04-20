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

    // Nova class
    static final int CLASS_MACHINE_GUNNER = 0;
    static final int CLASS_NOVA = 1;
    static final int NOVA_CHARGE_FRAMES = 180;
    static final int NOVA_COOLDOWN_FRAMES = 60;
    static final int NOVA_LASER_FRAMES = 18;
    static final int NOVA_LASER_WIDTH = 18;

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

    private final Rectangle btnClassMachineGunner = new Rectangle(WIDTH / 2 - 310, 180, 280, 380);
    private final Rectangle btnClassNova = new Rectangle(WIDTH / 2 + 30, 180, 280, 380);
    private final Rectangle btnClassBack = new Rectangle(WIDTH / 2 - 100, 660, 200, 50);

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

        if (selectedClass == CLASS_MACHINE_GUNNER) {
            firingThisFrame = wantsFire && !overheated;
            if (firingThisFrame)
                spd = Math.max(2, (int) (spd * 0.55));
            updateMachineGunner(wantsFire);
        } else {
            firingThisFrame = false;
            if (novaCharging || novaLaserActive)
                spd = Math.max(2, (int) (spd * 0.60));
            updateNova(wantsFire);
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

        // Nova laser vs boss
        if (selectedClass == CLASS_NOVA && novaLaserActive && !bossTransition && boss.alive) {
            if (laserHitsBoss(boss.getBounds())) {
                boss.hp -= 1;
                score += 6;
                if (boss.hp <= 0)
                    bossDefeated();
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
                if (boss.hp <= 0) {
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
                    gameState = STATE_GAME_OVER;
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
                        gameState = STATE_GAME_OVER;
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
    }

    private void bossDefeated() {
        boss.alive = false;
        score += 500;
        wave++;
        bossTransition = true;
        novaLaserActive = false;
        playerBullets.clear();
        enemyBullets.clear();
        Timer t = new Timer(1800, ev -> {
            boss = new Boss(WIDTH / 2 - 40, 60, wave);
            bossTransition = false;
        });
        t.setRepeats(false);
        t.start();
    }

    // ── Machine Gunner logic ──────────────────────────────────────────
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

    // ── Nova logic ────────────────────────────────────────────────────
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

    // ── Boss patterns ─────────────────────────────────────────────────
    // Wave scaling helpers:
    // Waves 1-5 = EASY bracket (low speed, low count, long intervals)
    // Waves 6-10 = MEDIUM bracket (moderate speed, moderate count)
    // Wave 11+ = HARD bracket (original feel)
    private void spawnBossPattern() {
        if (!boss.alive)
            return;
        int cx = boss.x + boss.width / 2, cy = boss.y + boss.height / 2;
        double dm = new double[] { 0.65, 0.9, 1.25 }[difficulty];

        // Determine bracket multipliers
        double speedScale, countScale;
        int ringInterval, aimedInterval, spiralInterval;
        if (wave <= 5) {
            // EASY – gentle intro
            speedScale = 0.55;
            countScale = 0.45;
            ringInterval = 130;
            aimedInterval = 90;
            spiralInterval = 22;
        } else if (wave <= 10) {
            // MEDIUM – picking up
            speedScale = 0.75;
            countScale = 0.70;
            ringInterval = 100;
            aimedInterval = 55;
            spiralInterval = 14;
        } else {
            // HARD – original feel
            speedScale = 1.0;
            countScale = 1.0;
            ringInterval = 80;
            aimedInterval = 35;
            spiralInterval = 8;
        }

        // ── Pattern 1: Ring burst (wave 1+) ──────────────────────────
        if (!boss.isApex && frameCount % ringInterval == 0) {
            int cnt = (int) Math.max(4, Math.min(8 + wave * 2, 20) * countScale);
            for (int i = 0; i < cnt; i++) {
                double a = 2 * Math.PI * i / cnt + Math.toRadians(frameCount * 2);
                double s = (2.2 + wave * 0.15) * dm * speedScale;
                enemyBullets.add(new Bullet(cx, cy, Math.cos(a) * s, Math.sin(a) * s, Color.RED, true));
            }
        }

        // ── Pattern 2: Aimed shot (wave 2+) ──────────────────────────
        if (!boss.isApex && wave >= 2 && frameCount % aimedInterval == 0) {
            double dx = player.x - cx, dy2 = player.y - cy, len = Math.sqrt(dx * dx + dy2 * dy2);
            if (len > 0) {
                double s = 3.0 * dm * speedScale;
                enemyBullets.add(new Bullet(cx, cy, dx / len * s, dy2 / len * s, Color.ORANGE, true));
            }
        }

        // ── Pattern 3: Rotating spiral (wave 3+) ─────────────────────
        if (!boss.isApex && wave >= 3 && frameCount % spiralInterval == 0) {
            double a = Math.toRadians(frameCount * 5), s = 2.5 * dm * speedScale;
            enemyBullets.add(new Bullet(cx, cy, Math.cos(a) * s, Math.sin(a) * s, Color.MAGENTA, true));
            enemyBullets.add(new Bullet(cx, cy, -Math.cos(a) * s, -Math.sin(a) * s, Color.MAGENTA, true));
        }

        // ── Pattern 4: 4-way cross (wave 4+, only medium+) ───────────
        if (!boss.isApex && wave >= 4 && wave > 5 && frameCount % 20 == 0) {
            for (int i = 0; i < 4; i++) {
                double a = Math.PI / 2 * i + Math.toRadians(frameCount * 2);
                double s = 3.0 * dm * speedScale;
                enemyBullets.add(new Bullet(cx, cy, Math.cos(a) * s, Math.sin(a) * s,
                        new Color(255, 80, 0), true));
            }
        }

        // ── Pattern 5: Flower burst (wave 5+, only medium+) ──────────
        if (!boss.isApex && wave >= 5 && wave > 5 && frameCount % 55 == 0) {
            for (int i = 0; i < 14; i++) {
                double a = 2 * Math.PI * i / 14 + Math.toRadians(frameCount);
                double s = 3.5 * dm * speedScale;
                enemyBullets.add(new Bullet(cx, cy, Math.cos(a) * s, Math.sin(a) * s, Color.YELLOW, true));
            }
        }

        // ── APEX BOSS: Empress of Light style ────────────────────────
        // Lasers are the PRIMARY threat; bullets are light supplemental.
        if (boss.isApex) {
            // Very sparse ring – only to keep player moving between laser attacks
            if (frameCount % 140 == 0) {
                int cnt = (int) Math.max(5, Math.min(8 + wave, 12) * countScale);
                for (int i = 0; i < cnt; i++) {
                    double a = 2 * Math.PI * i / cnt + Math.toRadians(frameCount * 3);
                    double s = 1.6 * dm * speedScale;
                    enemyBullets.add(new Bullet(cx, cy, Math.cos(a) * s, Math.sin(a) * s,
                            new Color(255, 100, 200), true));
                }
            }
            // Single aimed shot as supplemental pressure
            if (frameCount % 55 == 0) {
                double dx = player.x - cx, dy2 = player.y - cy, len = Math.sqrt(dx * dx + dy2 * dy2);
                if (len > 0) {
                    double s = 2.2 * dm * speedScale;
                    enemyBullets.add(new Bullet(cx, cy, dx / len * s, dy2 / len * s,
                            new Color(255, 60, 180), true));
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
        int sx = (int) ((frameCount * 2) % (WIDTH + 80)) - 40;
        g2.setColor(Color.CYAN);
        g2.fillPolygon(new int[] { sx + 7, sx, sx + 14 }, new int[] { 298, 312, 312 }, 3);
        drawBtn(g2, btnStart, "START", true);
        drawBtn(g2, btnSettings, "SETTINGS", true);
        drawBtn(g2, btnQuit, "QUIT", true);
        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        g2.setColor(new Color(80, 80, 120));
        g2.drawString("v4.3", WIDTH - 40, HEIGHT - 10);
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

    // ── Class select ──────────────────────────────────────────────────
    private void drawClassSelect(Graphics2D g2) {
        centeredTitle(g2, "SELECT CLASS", 75);
        divider(g2, 87);
        g2.setFont(new Font("Arial", Font.ITALIC, 13));
        g2.setColor(new Color(160, 160, 230));
        String sub = "Pick your pilot — click once to select, again to start";
        g2.drawString(sub, WIDTH / 2 - g2.getFontMetrics().stringWidth(sub) / 2, 118);
        drawClassCard(g2, btnClassMachineGunner, CLASS_MACHINE_GUNNER, "MACHINE\nGUNNER", new Color(0, 220, 255),
                "Spray and pray.\nOverheat at your own risk.", new int[] { 5, 4, 3, 2 },
                selectedClass == CLASS_MACHINE_GUNNER);
        drawClassCard(g2, btnClassNova, CLASS_NOVA, "NOVA", new Color(130, 80, 255),
                "Hold to charge a laser beam.\nOne shot, massive impact.", new int[] { 2, 1, 3, 4 },
                selectedClass == CLASS_NOVA);
        drawBtn(g2, btnClassBack, "BACK", true);
    }

    private void drawClassCard(Graphics2D g2, Rectangle r, int classId, String titleRaw, Color accent, String descRaw,
            int[] stats, boolean selected) {
        if (selected) {
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 30));
            g2.fillRoundRect(r.x - 8, r.y - 8, r.width + 16, r.height + 16, 22, 22);
        }
        g2.setColor(selected ? new Color(8, 15, 50) : new Color(10, 10, 28));
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 14, 14);
        g2.setColor(selected ? accent : new Color(50, 50, 100));
        g2.setStroke(new BasicStroke(selected ? 2.5f : 1.2f));
        g2.drawRoundRect(r.x, r.y, r.width, r.height, 14, 14);
        g2.setStroke(new BasicStroke(1));
        if (selected) {
            g2.setColor(accent);
            g2.fillRoundRect(r.x + r.width - 82, r.y + 10, 72, 20, 8, 8);
            g2.setFont(new Font("Arial", Font.BOLD, 10));
            g2.setColor(new Color(6, 6, 22));
            g2.drawString("SELECTED", r.x + r.width - 78, r.y + 23);
        }
        int shipCX = r.x + r.width / 2, shipY = r.y + 20;
        if (classId == CLASS_MACHINE_GUNNER) {
            g2.setColor(Color.CYAN);
            g2.fillPolygon(new int[] { shipCX, shipCX - 18, shipCX + 18 }, new int[] { shipY, shipY + 36, shipY + 36 },
                    3);
            g2.setColor(new Color(200, 255, 255));
            g2.fillOval(shipCX - 7, shipY + 9, 14, 14);
            g2.setColor(new Color(0, 100, 255, 120));
            g2.fillOval(shipCX - 7, shipY + 30, 14, 10);
        } else {
            g2.setColor(new Color(160, 100, 255));
            g2.fillPolygon(new int[] { shipCX, shipCX - 10, shipCX + 10 }, new int[] { shipY, shipY + 36, shipY + 36 },
                    3);
            g2.setColor(new Color(100, 60, 200));
            g2.fillPolygon(new int[] { shipCX - 10, shipCX - 22, shipCX - 10 },
                    new int[] { shipY + 22, shipY + 36, shipY + 36 }, 3);
            g2.fillPolygon(new int[] { shipCX + 10, shipCX + 22, shipCX + 10 },
                    new int[] { shipY + 22, shipY + 36, shipY + 36 }, 3);
            g2.setColor(new Color(200, 180, 255));
            g2.fillOval(shipCX - 5, shipY + 8, 10, 10);
            if (selected) {
                float p = (float) (0.5 + 0.5 * Math.sin(frameCount * 0.1));
                g2.setColor(new Color(130, 80, 255, (int) (55 * p)));
                g2.fillOval(shipCX - 18, shipY - 5, 36, 50);
            }
        }
        String[] titleLines = titleRaw.split("\\n");
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        g2.setColor(selected ? Color.WHITE : new Color(180, 180, 230));
        int ty = r.y + 74;
        for (String line : titleLines) {
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(line, r.x + r.width / 2 - fm.stringWidth(line) / 2, ty);
            ty += 22;
        }
        String[] descLines = descRaw.split("\\n");
        g2.setFont(new Font("Arial", Font.ITALIC, 11));
        g2.setColor(selected ? new Color(180, 210, 255) : new Color(100, 100, 160));
        int dy = ty + 4;
        for (String line : descLines) {
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(line, r.x + r.width / 2 - fm.stringWidth(line) / 2, dy);
            dy += 16;
        }
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 50));
        g2.fillRect(r.x + 14, dy + 4, r.width - 28, 1);
        String[] sLabels = { "FIRE RATE", "HEAT BUILD", "SPEED", "DEFENSE" };
        Color[] sCols = { new Color(0, 220, 255), new Color(255, 140, 0), new Color(80, 255, 80),
                new Color(180, 100, 255) };
        int sRowY = dy + 16;
        for (int i = 0; i < 4; i++) {
            drawStatRow(g2, r.x + 14, sRowY, sLabels[i], stats[i], 5, sCols[i]);
            sRowY += 26;
        }
    }

    private void drawStatRow(Graphics2D g2, int x, int y, String label, int value, int max, Color pipColor) {
        g2.setFont(new Font("Arial", Font.BOLD, 10));
        g2.setColor(new Color(120, 160, 220));
        g2.drawString(label, x, y + 11);
        int pipW = 16, pipH = 10, gap = 4, startX = x + 95;
        for (int i = 0; i < max; i++) {
            boolean f = i < value;
            g2.setColor(f ? pipColor : new Color(20, 20, 50));
            g2.fillRoundRect(startX + i * (pipW + gap), y, pipW, pipH, 4, 4);
            if (f) {
                g2.setColor(new Color(255, 255, 255, 55));
                g2.fillRoundRect(startX + i * (pipW + gap), y, pipW, pipH / 2, 4, 4);
            }
            g2.setColor(f ? pipColor.darker() : new Color(35, 35, 70));
            g2.setStroke(new BasicStroke(0.8f));
            g2.drawRoundRect(startX + i * (pipW + gap), y, pipW, pipH, 4, 4);
            g2.setStroke(new BasicStroke(1));
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

        // ── Nova FX ──────────────────────────────────────────────────
        if (selectedClass == CLASS_NOVA) {
            if (novaCharging && novaChargeTimer > 0) {
                float cf = (float) novaChargeTimer / NOVA_CHARGE_FRAMES;
                int pcx = player.x + player.size / 2, pcy = player.y + player.size / 2;
                int ringR = (int) (14 + 28 * cf);
                g2.setColor(new Color((int) (80 + 175 * cf), (int) (80 + 160 * cf), 255, (int) (60 + 100 * cf)));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawOval(pcx - ringR, pcy - ringR, ringR * 2, ringR * 2);
                g2.setStroke(new BasicStroke(1));
                g2.setColor(new Color(100, 130, 255, (int) (20 * cf * (0.5 + 0.5 * Math.sin(frameCount * 0.3)))));
                g2.fillOval(pcx - ringR, pcy - ringR, ringR * 2, ringR * 2);
                int barW = 60, barH = 6, barX = pcx - barW / 2, barY = player.y - 16;
                g2.setColor(new Color(10, 10, 40));
                g2.fillRoundRect(barX, barY, barW, barH, 4, 4);
                g2.setColor(new Color((int) (80 + 120 * cf), (int) (80 + 130 * cf), 255));
                g2.fillRoundRect(barX, barY, (int) (barW * cf), barH, 4, 4);
                g2.setColor(new Color(100, 100, 200));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(barX, barY, barW, barH, 4, 4);
                g2.setStroke(new BasicStroke(1));
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
                Color[] cols = {
                        new Color(60, 100, 255, (int) (18 * t)), new Color(100, 160, 255, (int) (50 * t)),
                        new Color(160, 210, 255, (int) (120 * t)), new Color(240, 250, 255, (int) (240 * t)) };
                for (int gi = 0; gi < 4; gi++) {
                    g2.setColor(cols[gi]);
                    g2.setStroke(new BasicStroke(widths[gi], BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(novaBeamX1, novaBeamY1, novaBeamX2, novaBeamY2);
                }
                g2.setStroke(new BasicStroke(1));
                rand.setSeed(frameCount * 7L);
                for (int sp = 0; sp < 8; sp++) {
                    double ang = rand.nextDouble() * Math.PI * 2;
                    int sx = (int) (novaBeamX1 + Math.cos(ang) * (4 + rand.nextInt(8)));
                    int sy = (int) (novaBeamY1 + Math.sin(ang) * (4 + rand.nextInt(8)));
                    g2.setColor(new Color(200, 230, 255, (int) (180 * t)));
                    g2.fillOval(sx - 2, sy - 2, 4, 4);
                }
            }
            if (novaCooldownTimer > 0 && !novaCharging && !novaLaserActive) {
                float cdf = (float) novaCooldownTimer / NOVA_COOLDOWN_FRAMES;
                int pcx = player.x + player.size / 2, barW = 50, barH = 5, barX = pcx - barW / 2, barY = player.y - 14;
                g2.setColor(new Color(10, 10, 40));
                g2.fillRoundRect(barX, barY, barW, barH, 4, 4);
                g2.setColor(new Color(200, 100, 60));
                g2.fillRoundRect(barX, barY, (int) (barW * cdf), barH, 4, 4);
                g2.setColor(new Color(120, 80, 60));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(barX, barY, barW, barH, 4, 4);
                g2.setStroke(new BasicStroke(1));
                g2.setFont(new Font("Arial", Font.BOLD, 9));
                g2.setColor(new Color(200, 120, 80));
                String cd = "COOLDOWN";
                g2.drawString(cd, barX + barW / 2 - g2.getFontMetrics().stringWidth(cd) / 2, barY - 2);
            }
        }

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

        g2.setFont(new Font("Arial", Font.BOLD, 16));
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
            if (wave % 5 == 0) {
                g2.setColor(pct > 0.5 ? new Color(220, 160, 0) : pct > 0.25 ? new Color(255, 80, 0) : Color.RED);
            } else {
                g2.setColor(pct > 0.5 ? Color.RED : pct > 0.25 ? Color.ORANGE : Color.YELLOW);
            }
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
        } else if (hf < 0.5f) {
            bc = new Color((int) (hf * 2 * 255), 220, 0);
        } else {
            bc = new Color(255, (int) ((1f - (hf - 0.5f) * 2) * 180), 0);
        }
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
        if (overheated) {
            float rf = 1f - (float) overheatTimer / OVERHEAT_FRAMES;
            int rw = (int) (bW * rf);
            g2.setColor(new Color(0, 160, 255, 120));
            g2.fillRoundRect(bX, bY + bH + 2, rw, 4, 3, 3);
        }
    }

    private void drawActivePowerupIcons(Graphics2D g2) {
        int[][] pus = {
                { PU_DOUBLE_SHOT, doubleShot ? 1 : 0, doubleShotTimer, 480 },
                { PU_SHIELD, hasShield ? 1 : 0, shieldTimer, 600 }
        };
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
            if (btnClassMachineGunner.contains(p)) {
                if (selectedClass == CLASS_MACHINE_GUNNER)
                    startGame();
                else
                    selectedClass = CLASS_MACHINE_GUNNER;
            } else if (btnClassNova.contains(p)) {
                if (selectedClass == CLASS_NOVA)
                    startGame();
                else
                    selectedClass = CLASS_NOVA;
            } else if (btnClassBack.contains(p))
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
                g2.setColor(new Color(100, 60, 200));
                g2.fillPolygon(new int[] { x + 4, x, x + 4 }, new int[] { y + size * 2 / 3, y + size, y + size }, 3);
                g2.fillPolygon(new int[] { x + size - 4, x + size, x + size - 4 },
                        new int[] { y + size * 2 / 3, y + size, y + size }, 3);
                g2.setColor(new Color(220, 200, 255));
                g2.fillOval(x + size / 2 - 4, y + 7, 8, 8);
                if (novaCharging) {
                    float cf = (float) novaChargeTimer / NOVA_CHARGE_FRAMES;
                    g2.setColor(new Color(130, 80, 255, (int) (50 * cf)));
                    g2.fillOval(x - 4, y - 4, size + 8, size + 8);
                }
            } else {
                int glowA = firingThisFrame ? (int) (120 + 80 * Math.abs(Math.sin(frameCount * 0.5))) : 120;
                g2.setColor(new Color(0, 120, 255, glowA));
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
    }

    // =================================================================
    // BOSS CLASS
    // Laser is APEX ONLY.
    // Apex beams always originate from boss and fire DOWNWARD toward player.
    // =================================================================
    class Boss {
        int x, y, width = 80, height = 50;
        double hp, maxHp;
        boolean alive = true;
        boolean isApex;

        // Movement phases
        static final int PHASE_PATROL = 0;
        static final int PHASE_CIRCLE = 1;
        static final int PHASE_DIVE = 2;
        static final int PHASE_ZIGZAG = 3;
        static final int PHASE_CHARGE = 4;
        int phase = PHASE_PATROL, phaseTimer = 0, phaseDuration = 180;
        double bx, by;
        double vx = 2, vy = 0;
        double circleAngle = 0;
        int diveTargetX, diveTargetY;
        int zigDir = 1;
        float pulsePhase = 0;
        int waveNum;

        // ── Laser state machine (APEX ONLY) ───────────────────────────
        // All angles stored as radians pointing from boss TOWARD player
        // (positive-Y = downward in screen coords, so angles near PI/2 = straight down)
        static final int LASER_NONE = 0;
        static final int LASER_TELEGRAPH = 1;
        static final int LASER_SWEEP = 2; // sweeping beam
        static final int LASER_TRACKING = 3; // real-time tracking
        static final int LASER_CHANNELING = 4; // growing-width beam
        static final int LASER_PERSISTENT = 5; // long-duration slow-tracking

        int laserState = LASER_NONE;
        int laserTimer = 0;
        boolean laserActive = false;

        // Current beam angle in radians; 0 = right, PI/2 = DOWN (toward player)
        double beamAngle = Math.PI / 2;
        // For sweep: direction of sweep
        double sweepDir = 1;
        // Channeling beam width
        int channelingBeamWidth = 2;

        int laserCooldown = 0;
        int laserInterval;

        // Which apex type cycles next (0-3)
        int apexLaserCycle = 0;

        Boss(int bx2, int by2, int wave) {
            this.bx = bx2;
            this.by = by2;
            this.x = (int) bx;
            this.y = (int) by;
            this.waveNum = wave;
            this.isApex = (wave % 5 == 0);
            maxHp = hp = isApex ? (10 + wave * 20) * 1.5 : (10 + wave * 20);
            // Normal bosses: no laser needed, set interval very high
            laserInterval = isApex ? Math.max(120, 260 - wave * 8) : 999999;
            laserCooldown = isApex ? laserInterval / 2 : 999999;
        }

        Rectangle getBounds() {
            return new Rectangle((int) bx, (int) by, width, height);
        }

        String getBossName() {
            if (isApex) {
                String[] apexNames = { "APEX DREADNOUGHT", "APEX ANNIHILATOR", "APEX NEMESIS",
                        "APEX OBLITERATOR", "APEX GOD-SLAYER" };
                int idx = Math.min(waveNum / 5 - 1, apexNames.length - 1);
                return apexNames[idx] + "  HP";
            }
            String[] names = { "VOID SCOUT", "REAPER MK-II", "CRIMSON TITAN", "OMEGA CORE", "HELL'S EYE" };
            int idx = Math.min(waveNum - 1, names.length - 1);
            return names[idx] + "  HP";
        }

        // Origin point of beam (bottom center of boss)
        int originX() {
            return (int) bx + width / 2;
        }

        int originY() {
            return (int) by + height;
        }

        // Compute angle from boss to player, clamped to lower hemisphere
        // (ensures beam always fires DOWNWARD toward player, never backward)
        double angleToPlayer(Player player) {
            int ox = originX(), oy = originY();
            int px = player.x + player.size / 2, py = player.y + player.size / 2;
            double dx = px - ox, dy = py - oy;
            // If player is above boss (shouldn't normally happen), default to straight down
            if (dy < 0)
                dy = 10;
            double angle = Math.atan2(dy, dx);
            // Clamp to [PI*0.1, PI*0.9] range — keeps beam in lower half, never fires up
            angle = Math.max(Math.PI * 0.05, Math.min(Math.PI * 0.95, angle));
            return angle;
        }

        // Get endpoint of beam given origin + angle
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
                    double tdx = diveTargetX - bx, tdy = diveTargetY - by,
                            tlen = Math.sqrt(tdx * tdx + tdy * tdy);
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
            bx = Math.max(5, Math.min(WIDTH - 5 - width, bx));
            by = Math.max(20, Math.min(HEIGHT / 2.5, by));
            x = (int) bx;
            y = (int) by;

            // Laser scheduling (APEX ONLY)
            if (!isApex)
                return;

            if (laserState == LASER_NONE) {
                laserCooldown--;
                if (laserCooldown <= 0) {
                    laserCooldown = laserInterval + rand.nextInt(40);
                    // Cycle through 4 Empress-of-Light style beam types in order
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

        // ── Laser starters (APEX ONLY) ────────────────────────────────

        // Telegraph → sweep across player from left to right
        void startSweepLaser(Player player) {
            beamAngle = angleToPlayer(player);
            // Start sweep 45 degrees to the left of player
            beamAngle -= Math.PI * 0.25;
            beamAngle = Math.max(Math.PI * 0.05, beamAngle);
            sweepDir = 1; // sweep right
            laserState = LASER_TELEGRAPH;
            laserTimer = 60;
            laserActive = false;
        }

        // Tracking: locks on and follows player smoothly
        void startTrackingBeam(Player player) {
            beamAngle = angleToPlayer(player);
            laserState = LASER_TRACKING;
            laserTimer = 180;
            laserActive = true;
            playBossLaserSound();
        }

        // Channeling: locked direction, grows in width
        void startChannelingBeam(Player player) {
            beamAngle = angleToPlayer(player);
            channelingBeamWidth = 2;
            laserState = LASER_CHANNELING;
            laserTimer = 240;
            laserActive = true;
            playBossLaserSound();
        }

        // Persistent: fires at player, slowly sweeps
        void startPersistentBeam(Player player) {
            beamAngle = angleToPlayer(player);
            sweepDir = rand.nextBoolean() ? 1 : -1;
            laserState = LASER_PERSISTENT;
            laserTimer = 300;
            laserActive = true;
            playBossLaserSound();
        }

        void updateLaserState(Player player) {
            if (laserState == LASER_TELEGRAPH) {
                // Halfway through telegraph → switch to sweep
                if (laserTimer <= 30) {
                    laserState = LASER_SWEEP;
                    laserTimer = 80;
                    laserActive = true;
                    playBossLaserSound();
                }

            } else if (laserState == LASER_SWEEP) {
                // Sweep across
                beamAngle += sweepDir * 0.030;
                // Clamp to lower hemisphere
                beamAngle = Math.max(Math.PI * 0.04, Math.min(Math.PI * 0.96, beamAngle));
                if (beamAngle >= Math.PI * 0.96 || beamAngle <= Math.PI * 0.04)
                    sweepDir *= -1;

            } else if (laserState == LASER_TRACKING) {
                // Smoothly rotate toward player each frame
                double target = angleToPlayer(player);
                double diff = target - beamAngle;
                while (diff > Math.PI)
                    diff -= 2 * Math.PI;
                while (diff < -Math.PI)
                    diff += 2 * Math.PI;
                beamAngle += 0.05 * Math.signum(diff) * Math.min(Math.abs(diff), 1.0);
                beamAngle = Math.max(Math.PI * 0.04, Math.min(Math.PI * 0.96, beamAngle));

            } else if (laserState == LASER_CHANNELING) {
                // Direction locked; width grows
                int elapsed = 240 - laserTimer;
                channelingBeamWidth = Math.min(2 + elapsed / 8, 28);

            } else if (laserState == LASER_PERSISTENT) {
                // Very slow rotation toward player
                double target = angleToPlayer(player);
                double diff = target - beamAngle;
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
            int ox = originX(), oy = originY();
            int ex = beamEndX(), ey = beamEndY();
            int hw = (laserState == LASER_CHANNELING) ? channelingBeamWidth / 2 + 4 : 10;
            Rectangle fat = new Rectangle(hitbox.x - hw, hitbox.y - hw,
                    hitbox.width + hw * 2, hitbox.height + hw * 2);
            return fat.intersectsLine(ox, oy, ex, ey);
        }

        void draw(Graphics2D g2, int frame, Player player) {
            if (!alive)
                return;
            float pulse = (float) (0.5 + 0.5 * Math.sin(pulsePhase));

            // Draw lasers behind boss body
            if (isApex && laserState != LASER_NONE) {
                drawBossLaser(g2, frame);
            }

            Color baseColor = getBossColor();
            int glowAlpha = isApex ? (int) (55 + 45 * pulse) : (int) (35 + 25 * pulse);
            g2.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), glowAlpha));
            g2.fillRoundRect((int) bx - 10, (int) by - 10, width + 20, height + 20, 22, 22);
            if (isApex) {
                g2.setColor(
                        new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), (int) (20 * pulse)));
                g2.fillRoundRect((int) bx - 24, (int) by - 24, width + 48, height + 48, 32, 32);
            }
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
                g2.setColor(new Color(base.getRed(), Math.min(255, base.getGreen() + 80),
                        Math.min(255, base.getBlue() + 80), (int) (160 * cf)));
                g2.fillOval(cxb - 10, (int) by + height / 2 - 7, 20, 14);
            }
            g2.setColor(isApex ? new Color(255, 80, 0) : Color.YELLOW);
            g2.fillOval(cxb - 22, (int) by + 14, 14, 14);
            g2.fillOval(cxb + 8, (int) by + 14, 14, 14);
            g2.setColor(new Color(0, 0, 0));
            g2.fillOval(cxb - 19, (int) by + 17, 8, 8);
            g2.fillOval(cxb + 11, (int) by + 17, 8, 8);
            int eyeGlow = (int) (120 + 100 * pulse);
            g2.setColor(isApex ? new Color(255, 120, 0, eyeGlow) : new Color(255, 200, 0, eyeGlow));
            g2.fillOval(cxb - 21, (int) by + 15, 4, 4);
            g2.fillOval(cxb + 9, (int) by + 15, 4, 4);
            drawPhaseOrbs(g2, frame, pulse);

            // Telegraph ring (APEX only)
            if (isApex && laserState == LASER_TELEGRAPH) {
                float tf = 1f - (laserTimer / 60f);
                int ra = (int) (80 + 120 * tf);
                g2.setColor(new Color(255, 30, 30, ra));
                g2.setStroke(new BasicStroke(2f + tf * 3));
                g2.drawOval(cxb - 24, (int) by - 6, 48, 48);
                g2.setStroke(new BasicStroke(1));
                g2.setColor(new Color(255, 60, 60, (int) (ra * 0.5f)));
                g2.fillOval(cxb - 20, (int) by - 2, 40, 40);
            }
        }

        private void drawPhaseOrbs(Graphics2D g2, int frame, float pulse) {
            Color[] phaseColors = { new Color(80, 200, 255), new Color(255, 200, 0), new Color(255, 80, 80),
                    new Color(180, 100, 255), new Color(255, 160, 0) };
            Color pc = phaseColors[Math.min(phase, phaseColors.length - 1)];
            float orbPulse = (float) (0.5 + 0.5 * Math.sin(frame * 0.12));
            g2.setColor(new Color(pc.getRed(), pc.getGreen(), pc.getBlue(), (int) (160 * orbPulse)));
            g2.fillOval((int) bx - 8, (int) by + height / 2 - 6, 12, 12);
            g2.fillOval((int) bx + width - 4, (int) by + height / 2 - 6, 12, 12);
            g2.setColor(new Color(pc.getRed(), pc.getGreen(), pc.getBlue(), 100));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval((int) bx - 8, (int) by + height / 2 - 6, 12, 12);
            g2.drawOval((int) bx + width - 4, (int) by + height / 2 - 6, 12, 12);
            g2.setStroke(new BasicStroke(1));
        }

        private void drawEngineTrail(Graphics2D g2, int frame, float pulse) {
            int engineCount = isApex ? 5 : 3;
            int[] ex = new int[engineCount];
            if (isApex) {
                int step = width / (engineCount - 1);
                for (int i = 0; i < engineCount; i++)
                    ex[i] = cx() - width / 2 + i * step;
            } else {
                ex = new int[] { cx() - 18, cx(), cx() + 18 };
            }
            for (int i = 0; i < engineCount; i++) {
                int len = (int) (18 + 12 * Math.abs(Math.sin(frame * 0.15 + i * 1.2)));
                if (isApex)
                    len = (int) (len * 1.5);
                g2.setColor(new Color(255, 120, 0, (int) (130 * pulse)));
                g2.fillPolygon(new int[] { ex[i] - 5, ex[i] + 5, ex[i] },
                        new int[] { (int) by + height, (int) by + height, (int) by + height + len }, 3);
                g2.setColor(new Color(255, 220, 80, (int) (90 * pulse)));
                g2.fillPolygon(new int[] { ex[i] - 2, ex[i] + 2, ex[i] },
                        new int[] { (int) by + height, (int) by + height, (int) by + height + len - 4 }, 3);
            }
        }

        // Draw the APEX laser beam based on current state
        private void drawBossLaser(Graphics2D g2, int frame) {
            int ox = originX(), oy = originY();

            if (laserState == LASER_TELEGRAPH) {
                // Dashed preview line in direction of coming sweep
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
                drawActiveBossBeam(g2, frame, ox, oy, beamEndX(), beamEndY(),
                        new Color(255, 30, 30), 80, 8f, null);

            } else if (laserState == LASER_TRACKING) {
                drawActiveBossBeam(g2, frame, ox, oy, beamEndX(), beamEndY(),
                        new Color(0, 200, 255), 180, 7f, "TRACKING");

            } else if (laserState == LASER_CHANNELING) {
                int w = channelingBeamWidth;
                float intensity = Math.min(1f, w / 28f);
                Color beamColor = new Color(
                        (int) (255 * intensity),
                        (int) (100 - 100 * intensity),
                        (int) (255 - 200 * intensity));
                int ex = beamEndX(), ey = beamEndY();
                // Outer glow
                g2.setColor(new Color(beamColor.getRed(), beamColor.getGreen(), beamColor.getBlue(),
                        (int) (20 * intensity)));
                g2.setStroke(new BasicStroke(w * 3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(ox, oy, ex, ey);
                // Core
                g2.setColor(new Color(beamColor.getRed(), beamColor.getGreen(), beamColor.getBlue(),
                        (int) (200 * intensity)));
                g2.setStroke(new BasicStroke(Math.max(1, w), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(ox, oy, ex, ey);
                // White hot center
                g2.setColor(new Color(255, 255, 255, (int) (220 * intensity)));
                g2.setStroke(new BasicStroke(Math.max(1, w / 3), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(ox, oy, ex, ey);
                g2.setStroke(new BasicStroke(1));
                g2.setFont(new Font("Arial", Font.BOLD, 11));
                g2.setColor(new Color(255, 140, 0, (int) (180 * Math.abs(Math.sin(frame * 0.2)))));
                g2.drawString("CHANNELING", ox - 28, oy - 14);

            } else if (laserState == LASER_PERSISTENT) {
                drawActiveBossBeam(g2, frame, ox, oy, beamEndX(), beamEndY(),
                        new Color(255, 80, 0), 300, 10f, "PERSISTENT");
            }
        }

        private void drawActiveBossBeam(Graphics2D g2, int frame, int x1, int y1, int x2, int y2,
                Color beamColor, int totalFrames, float baseWidth, String label) {
            float t = Math.max(0.1f, (float) laserTimer / totalFrames);
            // Outer glow
            g2.setColor(new Color(beamColor.getRed(), beamColor.getGreen(), beamColor.getBlue(), (int) (15 * t)));
            g2.setStroke(new BasicStroke(baseWidth * 4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x1, y1, x2, y2);
            // Mid glow
            g2.setColor(new Color(beamColor.getRed(), beamColor.getGreen(), beamColor.getBlue(), (int) (55 * t)));
            g2.setStroke(new BasicStroke(baseWidth * 2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x1, y1, x2, y2);
            // Core
            g2.setColor(new Color(255, 160, 80, (int) (130 * t)));
            g2.setStroke(new BasicStroke(baseWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x1, y1, x2, y2);
            // White hot center
            g2.setColor(new Color(255, 255, 255, (int) (230 * t)));
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x1, y1, x2, y2);
            g2.setStroke(new BasicStroke(1));
            // Sparkles at origin
            rand.setSeed(frame * 13L);
            for (int sp = 0; sp < 6; sp++) {
                double ang = rand.nextDouble() * Math.PI * 2, r = 5 + rand.nextInt(10);
                g2.setColor(new Color(255, 180, 80, (int) (180 * t)));
                g2.fillOval((int) (x1 + Math.cos(ang) * r) - 2, (int) (y1 + Math.sin(ang) * r) - 2, 4, 4);
            }
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

    // =================================================================
    // MAIN
    // =================================================================
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