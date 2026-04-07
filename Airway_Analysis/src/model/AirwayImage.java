package model;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;


public class AirwayImage {

    private int width;
    private int height;
    private Pixel[][] pixels;

    public AirwayImage() {
        width = 0;
        height = 0;
        pixels = null;
    }

    public AirwayImage(int width, int height) {
        this.width = width;
        this.height = height;
        pixels = new Pixel[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[y][x] = new Pixel(x, y, 0);
            }
        }
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Pixel[][] getPixels() {
        return pixels;
    }

    public void setPixels(Pixel[][] pixels) {
        this.pixels = pixels;
        if (pixels != null) {
            this.height = pixels.length;
            this.width = pixels[0].length;
        }
    }

    public Pixel getPixel(int x, int y) {
        return pixels[y][x];
    }

    public void setPixel(int x, int y, int value) {
        pixels[y][x].setValue(value);
    }

    public void clearImage() {
        if (pixels == null) {
            return;
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[y][x].setValue(0);
            }
        }
    }

    public void resizeImage(int newWidth, int newHeight) {
        if (newWidth < 0 || newHeight < 0) {
            throw new IllegalArgumentException("Width and height must be non-negative");
        }

        Pixel[][] newPixels = new Pixel[newHeight][newWidth];

        for (int y = 0; y < newHeight; y++) {
            for (int x = 0; x < newWidth; x++) {
                if (pixels != null && y < height && x < width) {
                    newPixels[y][x] = new Pixel(x, y, pixels[y][x].getValue());
                } else {
                    newPixels[y][x] = new Pixel(x, y, 0);
                }
            }
        }

        this.width = newWidth;
        this.height = newHeight;
        this.pixels = newPixels;
    }
    
    public WritableImage displayPixels() {
        WritableImage image = new WritableImage(width, height);
        PixelWriter writer = image.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int value = pixels[y][x].getValue();
                writer.setColor(x, y, Color.grayRgb(value));
            }
        }

        return image;
    }

    public void deleteImage() {
        width = 0;
        height = 0;
        pixels = null;
    }

    public String toString() {
        return "AirwayImage [width=" + width + ", height=" + height + "]";
    }
}