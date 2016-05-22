package chatup.backend;

import java.awt.image.BufferedImage;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics;
import java.awt.Transparency;
import java.net.URL;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

public class GraphicsUtilities
{
    public static BufferedImage loadCompatibleImage(final URL resource) throws IOException
    {
	return toCompatibleImage(ImageIO.read(resource));
    }

    public static BufferedImage loadCompatibleImage(final InputStream stream) throws IOException
    {
	return toCompatibleImage(ImageIO.read(stream));
    }

    public static BufferedImage createCompatibleImage(int width, int height)
    {
	return getGraphicsConfiguration().createCompatibleImage(width, height);
    }

    public static BufferedImage toCompatibleImage(final BufferedImage image)
    {
	if (isHeadless())
	{
	    return image;
	}

	if (image.getColorModel().equals(getGraphicsConfiguration().getColorModel()))
	{
	    return image;
	}

	final BufferedImage compatibleImage = getGraphicsConfiguration().createCompatibleImage(image.getWidth(), image.getHeight(), image.getTransparency());
	final Graphics g = compatibleImage.getGraphics();

	g.drawImage(image, 0, 0, null);
	g.dispose();

	return compatibleImage;
    }

    public static BufferedImage createCompatibleImage(final BufferedImage image, int width, int height)
    {
	return getGraphicsConfiguration().createCompatibleImage(width, height, image.getTransparency());
    }

    private static GraphicsConfiguration getGraphicsConfiguration()
    {
	return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
    }

    private static boolean isHeadless()
    {
	return GraphicsEnvironment.isHeadless();
    }

    public static BufferedImage createTranslucentCompatibleImage(int width, int height)
    {
	return getGraphicsConfiguration().createCompatibleImage(width, height, Transparency.TRANSLUCENT);
    }

    public static int[] getPixels(BufferedImage img, int x, int y, int w, int h, int[] pixels)
    {
	if (w == 0 || h == 0)
	{
	    return new int[0];
	}

	if (pixels == null)
	{
	    pixels = new int[w * h];
	}
	else if (pixels.length < w * h)
	{
	    throw new IllegalArgumentException("Pixels array must have a length >= w * h");
	}

	int imageType = img.getType();

	if (imageType == BufferedImage.TYPE_INT_ARGB || imageType == BufferedImage.TYPE_INT_RGB)
	{
	    return (int[]) img.getRaster().getDataElements(x, y, w, h, pixels);
	}

	return img.getRGB(x, y, w, h, pixels, 0, w);
    }
}