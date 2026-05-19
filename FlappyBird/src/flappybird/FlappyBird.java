import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FlappyBird extends JPanel implements ActionListener, KeyListener, MouseListener {

    // --- Constants ---
    static final int WIDTH = 400, HEIGHT = 600;
    static final int BIRD_X = 80, BIRD_SIZE = 28;
    static final int PIPE_WIDTH = 60, PIPE_GAP = 160;
    static final int PIPE_SPEED = 3;
    static final int GRAVITY = 1;
    static final int JUMP_STRENGTH = -13;
    static final int GROUND_HEIGHT = 80;
    static final int PIPE_INTERVAL = 90; // frames between pipes

    // --- Colors ---
    static final Color SKY_TOP    = new Color(0x6EC6E6);
    static final Color SKY_BTM    = new Color(0xB8E8FF);
    static final Color GROUND_COL = new Color(0xDEB887);
    static final Color GRASS_COL  = new Color(0x78C850);
    static final Color PIPE_COL   = new Color(0x3CB04A);
    static final Color PIPE_DARK  = new Color(0x2A8035);
    static final Color PIPE_LIGHT = new Color(0x5DD46A);
    static final Color BIRD_BODY  = new Color(0xFFD700);
    static final Color BIRD_WING  = new Color(0xFFA500);
    static final Color BIRD_EYE   = new Color(0xFFFFFF);
    static final Color BIRD_PUPIL = new Color(0x222222);
    static final Color BIRD_BEAK  = new Color(0xFF6622);
    static final Color BIRD_BELLY = new Color(0xFFEE88);

    // --- Game State ---
    enum State { MENU, PLAYING, DEAD }
    State state = State.MENU;

    // --- Bird ---
    double birdY = HEIGHT / 2.0;
    double birdVY = 0;
    double birdAngle = 0;
    int wingFrame = 0;

    // --- Pipes ---
    static class Pipe {
        int x, topH;
        boolean scored = false;
        Pipe(int x, int topH) { this.x = x; this.topH = topH; }
    }
    List<Pipe> pipes = new ArrayList<>();
    int frameCount = 0;
    int score = 0;
    int bestScore = 0;
    Random rand = new Random();

    // --- Background clouds ---
    int[] cloudX = {50, 180, 310};
    int[] cloudY = {60, 100, 50};

    // --- Animation ---
    Timer timer;
    double deathTimer = 0;
    double flashAlpha = 0;

    public FlappyBird() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);
        timer = new Timer(16, this); // ~60 fps
        timer.start();
    }

    // =========================================================
    //  GAME LOOP
    // =========================================================
    @Override
    public void actionPerformed(ActionEvent e) {
        if (state == State.PLAYING) {
            update();
        }
        // Animate clouds always
        for (int i = 0; i < cloudX.length; i++) {
            cloudX[i] -= 1;
            if (cloudX[i] < -80) cloudX[i] = WIDTH + 60;
        }
        wingFrame++;
        if (flashAlpha > 0) flashAlpha -= 0.05;
        repaint();
    }

    void update() {
        frameCount++;

        // Bird physics
        birdVY += GRAVITY;
        birdY += birdVY;

        // Angle based on velocity
        birdAngle = Math.min(Math.max(birdVY * 3.5, -30), 90);

        // Spawn pipes
        if (frameCount % PIPE_INTERVAL == 0) {
            int topH = 80 + rand.nextInt(HEIGHT - GROUND_HEIGHT - PIPE_GAP - 120);
            pipes.add(new Pipe(WIDTH + 10, topH));
        }

        // Move pipes & score
        for (Pipe p : pipes) {
            p.x -= PIPE_SPEED;
            if (!p.scored && p.x + PIPE_WIDTH < BIRD_X) {
                p.scored = true;
                score++;
            }
        }
        pipes.removeIf(p -> p.x + PIPE_WIDTH < -10);

        // Collision
        if (birdY + BIRD_SIZE / 2.0 >= HEIGHT - GROUND_HEIGHT) {
            birdY = HEIGHT - GROUND_HEIGHT - BIRD_SIZE / 2.0;
            die();
        }
        if (birdY - BIRD_SIZE / 2.0 <= 0) {
            die();
        }
        for (Pipe p : pipes) {
            if (birdCollidesWithPipe(p)) {
                die();
                break;
            }
        }
    }

    boolean birdCollidesWithPipe(Pipe p) {
        int r = BIRD_SIZE / 2 - 4; // small margin for forgiveness
        int bx = BIRD_X, by = (int) birdY;
        // Top pipe rect
        if (bx + r > p.x && bx - r < p.x + PIPE_WIDTH && by - r < p.topH) return true;
        // Bottom pipe rect
        int botPipeTop = p.topH + PIPE_GAP;
        if (bx + r > p.x && bx - r < p.x + PIPE_WIDTH && by + r > botPipeTop) return true;
        return false;
    }

    void die() {
        if (state == State.DEAD) return;
        state = State.DEAD;
        if (score > bestScore) bestScore = score;
        flashAlpha = 1.0;
        birdVY = JUMP_STRENGTH / 2.0;
    }

    void jump() {
        if (state == State.MENU) {
            startGame();
        } else if (state == State.PLAYING) {
            birdVY = JUMP_STRENGTH;
            flashAlpha = 0;
        } else if (state == State.DEAD) {
            startGame();
        }
    }

    void startGame() {
        birdY = HEIGHT / 2.0;
        birdVY = 0;
        birdAngle = 0;
        pipes.clear();
        frameCount = 0;
        score = 0;
        state = State.PLAYING;
    }

    // =========================================================
    //  RENDERING
    // =========================================================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawBackground(g2);
        drawClouds(g2);
        for (Pipe p : pipes) drawPipe(g2, p);
        drawGround(g2);
        drawBird(g2);

        // Flash on death
        if (flashAlpha > 0) {
            g2.setColor(new Color(1f, 1f, 1f, (float) Math.min(flashAlpha, 1.0)));
            g2.fillRect(0, 0, WIDTH, HEIGHT);
        }

        drawHUD(g2);
        if (state == State.MENU) drawMenu(g2);
        if (state == State.DEAD) drawGameOver(g2);
    }

    void drawBackground(Graphics2D g2) {
        GradientPaint sky = new GradientPaint(0, 0, SKY_TOP, 0, HEIGHT - GROUND_HEIGHT, SKY_BTM);
        g2.setPaint(sky);
        g2.fillRect(0, 0, WIDTH, HEIGHT - GROUND_HEIGHT);
    }

    void drawClouds(Graphics2D g2) {
        g2.setColor(new Color(255, 255, 255, 200));
        for (int i = 0; i < cloudX.length; i++) {
            drawCloud(g2, cloudX[i], cloudY[i]);
        }
    }

    void drawCloud(Graphics2D g2, int x, int y) {
        g2.fillOval(x, y, 50, 28);
        g2.fillOval(x + 18, y - 14, 40, 32);
        g2.fillOval(x + 36, y, 40, 24);
    }

    void drawPipe(Graphics2D g2, Pipe p) {
        int capH = 20, capW = PIPE_WIDTH + 10;
        int capX = p.x - 5;

        // Top pipe body
        g2.setColor(PIPE_COL);
        g2.fillRect(p.x, 0, PIPE_WIDTH, p.topH);
        // Top pipe cap
        g2.fillRect(capX, p.topH - capH, capW, capH);

        // Shading
        g2.setColor(PIPE_LIGHT);
        g2.fillRect(p.x + 4, 0, 8, p.topH - capH);
        g2.setColor(PIPE_DARK);
        g2.fillRect(p.x + PIPE_WIDTH - 8, 0, 8, p.topH - capH);

        // Cap shading
        g2.setColor(PIPE_LIGHT);
        g2.fillRect(capX + 4, p.topH - capH, 8, capH);
        g2.setColor(PIPE_DARK);
        g2.fillRect(capX + capW - 8, p.topH - capH, 8, capH);

        // Bottom pipe body
        int botTop = p.topH + PIPE_GAP;
        int botH = HEIGHT - GROUND_HEIGHT - botTop;
        g2.setColor(PIPE_COL);
        g2.fillRect(p.x, botTop, PIPE_WIDTH, botH);
        // Bottom pipe cap
        g2.fillRect(capX, botTop, capW, capH);

        // Shading
        g2.setColor(PIPE_LIGHT);
        g2.fillRect(p.x + 4, botTop + capH, 8, botH - capH);
        g2.setColor(PIPE_DARK);
        g2.fillRect(p.x + PIPE_WIDTH - 8, botTop + capH, 8, botH - capH);

        g2.setColor(PIPE_LIGHT);
        g2.fillRect(capX + 4, botTop, 8, capH);
        g2.setColor(PIPE_DARK);
        g2.fillRect(capX + capW - 8, botTop, 8, capH);
    }

    void drawGround(Graphics2D g2) {
        int gy = HEIGHT - GROUND_HEIGHT;
        g2.setColor(GRASS_COL);
        g2.fillRect(0, gy, WIDTH, 18);
        g2.setColor(GROUND_COL);
        g2.fillRect(0, gy + 18, WIDTH, GROUND_HEIGHT - 18);

        // Ground detail lines
        g2.setColor(new Color(0xC8A060));
        for (int x = 0; x < WIDTH; x += 30) {
            g2.drawLine(x, gy + 22, x + 15, gy + 22);
        }
    }

    void drawBird(Graphics2D g2) {
        int bx = BIRD_X, by = (int) birdY;
        int r = BIRD_SIZE / 2;

        Graphics2D bg = (Graphics2D) g2.create();
        bg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        bg.translate(bx, by);
        bg.rotate(Math.toRadians(birdAngle));

        // Wing
        double wingOffset = Math.sin(wingFrame * 0.25) * 6;
        bg.setColor(BIRD_WING);
        bg.fillOval(-r + 2, (int)(-4 + wingOffset), r + 2, r - 2);

        // Body
        bg.setColor(BIRD_BODY);
        bg.fillOval(-r, -r, BIRD_SIZE, BIRD_SIZE);

        // Belly
        bg.setColor(BIRD_BELLY);
        bg.fillOval(-r + 5, 0, BIRD_SIZE - 10, r - 2);

        // Eye white
        bg.setColor(BIRD_EYE);
        bg.fillOval(2, -r + 4, 12, 10);
        // Pupil
        bg.setColor(BIRD_PUPIL);
        bg.fillOval(5, -r + 6, 5, 5);
        // Eye shine
        bg.setColor(Color.WHITE);
        bg.fillOval(8, -r + 6, 2, 2);

        // Beak
        bg.setColor(BIRD_BEAK);
        int[] bkX = {r - 4, r + 8, r - 4};
        int[] bkY = {-4, 0, 4};
        bg.fillPolygon(bkX, bkY, 3);

        bg.dispose();
    }

    void drawHUD(Graphics2D g2) {
        if (state == State.PLAYING || state == State.DEAD) {
            // Score
            g2.setFont(new Font("Arial Black", Font.BOLD, 36));
            String s = String.valueOf(score);
            drawTextShadow(g2, s, WIDTH / 2, 70, Color.WHITE, new Color(0, 0, 0, 100), 3);
        }
    }

    void drawMenu(Graphics2D g2) {
        // Panel
        drawPanel(g2, WIDTH / 2 - 140, HEIGHT / 2 - 110, 280, 200);

        g2.setFont(new Font("Arial Black", Font.BOLD, 36));
        drawTextShadow(g2, "FLAPPY", WIDTH / 2, HEIGHT / 2 - 60, new Color(0xFFD700), new Color(0x8B6000), 3);
        g2.setFont(new Font("Arial Black", Font.BOLD, 36));
        drawTextShadow(g2, "BIRD", WIDTH / 2, HEIGHT / 2 - 20, new Color(0xFF6622), new Color(0x8B2000), 3);

        // Bouncing prompt
        double bounce = Math.sin(System.currentTimeMillis() / 300.0) * 5;
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        drawTextShadow(g2, "Click or SPACE to start", WIDTH / 2, (int)(HEIGHT / 2 + 40 + bounce),
                Color.WHITE, new Color(0,0,0,120), 2);

        if (bestScore > 0) {
            g2.setFont(new Font("Arial", Font.BOLD, 13));
            drawTextShadow(g2, "Best: " + bestScore, WIDTH / 2, HEIGHT / 2 + 70, new Color(0xFFD700), Color.DARK_GRAY, 1);
        }
    }

    void drawGameOver(Graphics2D g2) {
        drawPanel(g2, WIDTH / 2 - 140, HEIGHT / 2 - 120, 280, 230);

        g2.setFont(new Font("Arial Black", Font.BOLD, 34));
        drawTextShadow(g2, "GAME OVER", WIDTH / 2, HEIGHT / 2 - 65, new Color(0xFF3333), new Color(0x800000), 3);

        g2.setFont(new Font("Arial", Font.BOLD, 18));
        drawTextShadow(g2, "Score: " + score, WIDTH / 2, HEIGHT / 2 - 20, Color.WHITE, Color.DARK_GRAY, 2);

        g2.setFont(new Font("Arial", Font.BOLD, 16));
        drawTextShadow(g2, "Best:  " + bestScore, WIDTH / 2, HEIGHT / 2 + 15, new Color(0xFFD700), Color.DARK_GRAY, 2);

        // Medal
        if (score >= 10) {
            drawMedal(g2, WIDTH / 2, HEIGHT / 2 + 55, score);
        }

        double bounce = Math.sin(System.currentTimeMillis() / 300.0) * 4;
        g2.setFont(new Font("Arial", Font.BOLD, 13));
        drawTextShadow(g2, "Click or SPACE to retry", WIDTH / 2, (int)(HEIGHT / 2 + 90 + bounce),
                new Color(0xCCFFCC), new Color(0,0,0,120), 1);
    }

    void drawMedal(Graphics2D g2, int cx, int cy, int sc) {
        Color medalColor = sc >= 40 ? new Color(0xFFD700) :
                           sc >= 20 ? new Color(0xC0C0C0) :
                                      new Color(0xCD7F32);
        g2.setColor(medalColor.darker());
        g2.fillOval(cx - 18, cy - 18, 36, 36);
        g2.setColor(medalColor);
        g2.fillOval(cx - 15, cy - 15, 30, 30);
        g2.setColor(medalColor.brighter());
        g2.fillOval(cx - 10, cy - 12, 10, 8);
    }

    void drawPanel(Graphics2D g2, int x, int y, int w, int h) {
        // Shadow
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillRoundRect(x + 5, y + 5, w, h, 20, 20);
        // Panel
        g2.setColor(new Color(0xDEB887));
        g2.fillRoundRect(x, y, w, h, 20, 20);
        g2.setColor(new Color(0xC8A070));
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(x, y, w, h, 20, 20);
        g2.setStroke(new BasicStroke(1));
    }

    void drawTextShadow(Graphics2D g2, String text, int cx, int cy, Color fill, Color shadow, int offset) {
        FontMetrics fm = g2.getFontMetrics();
        int tx = cx - fm.stringWidth(text) / 2;
        int ty = cy + fm.getAscent() / 2 - 2;
        g2.setColor(shadow);
        g2.drawString(text, tx + offset, ty + offset);
        g2.setColor(fill);
        g2.drawString(text, tx, ty);
    }

    // =========================================================
    //  INPUT
    // =========================================================
    @Override public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_UP
                || e.getKeyCode() == KeyEvent.VK_W) jump();
    }
    @Override public void mousePressed(MouseEvent e) { jump(); }
    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    // =========================================================
    //  MAIN
    // =========================================================
    public static void main(String[] args) {
        JFrame frame = new JFrame("Flappy Bird");
        FlappyBird game = new FlappyBird();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        game.requestFocusInWindow();
    }
}
