// Draws the Google Play icon (512 x 512) and feature graphic (1024 x 500) from the same shapes as the
// launcher icon (res/drawable/ic_launcher_foreground.xml). JDK only:
//   java mobile/android/play-listing/PlayImages.java mobile/android/play-listing/metadata/android
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class PlayImages {
    static final Color GREEN = new Color(0x1B6B4F);
    static final Color LIGHT = new Color(0x8BD6B4);

    /** The wallet in the launcher icon's 108-unit square. */
    static void wallet(Graphics2D g) {
        g.setColor(Color.WHITE);
        Area body = new Area(new RoundRectangle2D.Double(28, 38, 48, 42, 12, 12));
        Area notch = new Area(new Rectangle2D.Double(58, 48, 19, 16));
        notch.add(new Area(new Ellipse2D.Double(50, 48, 16, 16)));
        body.subtract(notch);
        g.fill(body);
        Area clasp = new Area(new Rectangle2D.Double(58, 52, 20, 8));
        clasp.add(new Area(new Ellipse2D.Double(54, 52, 8, 8)));
        g.fill(clasp);
        Area flap = new Area(new RoundRectangle2D.Double(34, 30, 34, 12, 8, 8));
        flap.intersect(new Area(new Rectangle2D.Double(34, 30, 34, 6)));
        g.fill(flap);
        g.setColor(GREEN);
        g.fill(new Ellipse2D.Double(57.5, 53.5, 5, 5));
    }

    static Graphics2D smooth(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        return g;
    }

    public static void main(String[] args) throws Exception {
        File root = new File(args[0]);
        // Icon: full-bleed green square (Play rounds the corners), wallet scaled from the 108 grid.
        BufferedImage icon = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = smooth(icon);
        g.setColor(GREEN);
        g.fillRect(0, 0, 512, 512);
        g.scale(512 / 108.0, 512 / 108.0);
        wallet(g);
        g.dispose();
        // Feature graphic: green band, the wallet on the left, the name and one line on the right.
        BufferedImage feature = new BufferedImage(1024, 500, BufferedImage.TYPE_INT_RGB);
        g = smooth(feature);
        g.setPaint(new GradientPaint(0, 0, GREEN, 1024, 500, GREEN.darker()));
        g.fillRect(0, 0, 1024, 500);
        AffineTransform t = g.getTransform();
        g.translate(40, 70);
        g.scale(360 / 108.0, 360 / 108.0);
        wallet(g);
        g.setTransform(t);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 64));
        g.drawString("KS Expense", 420, 215);
        g.drawString("Manager", 420, 290);
        g.setColor(LIGHT);
        g.setFont(new Font("SansSerif", Font.PLAIN, 30));
        g.drawString("Private. Offline. In rupees.", 422, 350);
        g.dispose();
        for (String lang : new String[] {"en-US", "hi-IN"}) {
            File dir = new File(root, lang + "/images");
            dir.mkdirs();
            ImageIO.write(icon, "png", new File(dir, "icon.png"));
            ImageIO.write(feature, "png", new File(dir, "featureGraphic.png"));
        }
    }
}
