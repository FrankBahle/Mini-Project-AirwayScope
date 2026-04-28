package filter;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import model.AirwayImage;

public class ImageFilter 
{
    private AirwayImage image;
    private AirwayImage grayImage;
    private int[][] maskedImage;
    private int width;
    private int height;
    private int threshold;

    public ImageFilter(AirwayImage image)
    {
        if (image == null) {
            throw new IllegalArgumentException("Image cannot be null");
        }

        this.image = image;
        this.height = image.getHeight();
        this.width = image.getWidth();
        this.grayImage = new AirwayImage(width, height);
        this.maskedImage = new int[height][width];
        this.threshold = 128;
        
    }

    public void applyGrayScale()
    {
        for (int h = 0; h < this.height; h++)
        {
            for (int w = 0; w < this.width; w++)
            {
                int value = image.getPixel(w, h).getValue();

                if (value < 0) {
                    value = 0;
                }
                if (value > 255) {
                    value = 255;
                }

                grayImage.setPixel(w, h, value);
            }
        }
    }

    public void applyMask() 
    {
        applyGrayScale();

        for (int h = 0; h < this.height; h++) 
        {
            for (int w = 0; w < this.width; w++) 
            {
                int gray = grayImage.getPixel(w, h).getValue();

                if (gray < threshold) {
                    maskedImage[h][w] = 0;
                } else {
                    maskedImage[h][w] = 1;
                }
            }
        }
    }

    public int[][] applyBinaryMask()
    {
        applyMask();
        return maskedImage;
    }

    public AirwayImage getGreyScaleImage() {
    	applyGrayScale();
        return grayImage;
    }

    public int[][] getBinaryMask() {
        applyMask();
        return maskedImage;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }
    
    public void printBinaryMaskToFile(String fileName) {
        if (maskedImage == null) {
            System.out.println("null");

            try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
                writer.println("null");
            } catch (IOException e) {
                System.out.println("Error writing binary mask to file: " + e.getMessage());
            }

            return;
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            for (int h = 0; h < maskedImage.length; h++) {
                for (int w = 0; w < maskedImage[h].length; w++) {
                    writer.print(maskedImage[h][w] + " ");
                }
                writer.println();
            }

        } catch (IOException e) {
            System.out.println("Error writing binary mask to file: " + e.getMessage());
        }
    }
}