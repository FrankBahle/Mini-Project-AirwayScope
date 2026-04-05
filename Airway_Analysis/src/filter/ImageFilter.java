package filter;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

public class ImageFilter 
{
	private Image image;
	private WritableImage grayImage;
	private PixelReader reader;
	private PixelWriter writer;
	private int width;
	private int height;
	int rgb;
	private int r, g, b, a;
	
	public ImageFilter(Image image)
	{
		this.image = image;
		this.height = (int) image.getHeight();
		this.width = (int) image.getWidth();
		this.reader = image.getPixelReader();
		rgb = 0; r = 0; g = 0; b = 0; a =0;
		this.grayImage = new WritableImage(width, height);
		this.writer = grayImage.getPixelWriter();
	}
	
	public Image grayScaleImage()
	{
		for(int h = 0; h < this.height; h++)
		{
			for(int w = 0; w < this.width; w++)
			{
				rgb = reader.getArgb(w, h);
				r = ((rgb >> 16) & 0xFF);
				g = ((rgb >> 8) & 0xFF);
				b = (rgb & 0xFF);
				//a = ((rgb >> 24) & 0xFF);
				rgb = (int)((r+g+b)/3);
				rgb = (255 << 24) | (rgb << 16) | (rgb << 8) | rgb;
				writer.setArgb(w, h, rgb);
			}
		}
		
		return grayImage;
	}
	
	public WritableImage applyMask(Image img, int threshold) 
	{
        WritableImage outputImage = new WritableImage(width, height);
        PixelWriter writer = outputImage.getPixelWriter();
        PixelReader read = image.getPixelReader();

        int argb, a, r, g, b;
        int gray;
        int newColor;

        for (int h = 0; h < height; h++) 
        {
            for (int w = 0; w < width; w++) 
            {

                argb = read.getArgb(w, h);

                a = (argb >> 24) & 0xFF;
                r = (argb >> 16) & 0xFF;
                g = (argb >> 8) & 0xFF;
                b = argb & 0xFF;

                // Convert to grayscale brightness
                gray = (r + g + b) / 3;

                // Apply black/white mask
                if (gray < threshold) {
                    // black
                    newColor = (a << 24) | (0 << 16) | (0 << 8) | 0;
                } else {
                    // white
                    newColor = (a << 24) | (255 << 16) | (255 << 8) | 255;
                }

                writer.setArgb(w, h, newColor);
            }
        }

        return outputImage;
    }
}
