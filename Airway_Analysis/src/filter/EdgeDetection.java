package filter;

import model.AirwayImage;
import model.Pixel;

public class EdgeDetection {

	private AirwayImage OImage;
	private AirwayImage EImage;
	
    private int[][] kernelX;
    private int[][] kernelY;
    private int threshold;
    
    private int height;
    private int width;
	
	public EdgeDetection(AirwayImage originalImage, int threshold) {
		this.OImage = originalImage;
		this.kernelX = new int[][] {
            {-1, 0, 1},
            {-2, 0, 2},
            {-1, 0, 1}
        };

        this.kernelY = new int[][] {
            {-1, -2, -1},
            { 0,  0,  0},
            { 1,  2,  1}
        };
        
        this.threshold = threshold;
        this.height = originalImage.getHeight();
        this.width = originalImage.getWidth();
        
        this.EImage = new AirwayImage(originalImage.getWidth(), originalImage.getHeight());
        applySobel();
	}
	
	private void applySobel() {
		for (int row = 1; row < OImage.getHeight() - 1; row++) {
		    for (int col = 1; col < OImage.getWidth() - 1; col++) {

		    	Pixel p00 = OImage.getPixel(col - 1, row - 1);
		    	Pixel p01 = OImage.getPixel(col, row - 1);
		    	Pixel p02 = OImage.getPixel(col + 1, row - 1);

		    	Pixel p10 = OImage.getPixel(col - 1, row);
		    	Pixel p11 = OImage.getPixel(col, row);
		    	Pixel p12 = OImage.getPixel(col + 1, row);

		    	Pixel p20 = OImage.getPixel(col - 1, row + 1);
		    	Pixel p21 = OImage.getPixel(col, row + 1);
		    	Pixel p22 = OImage.getPixel(col + 1, row + 1);
		    	
		        int gx =
		            (p00.getValue() * kernelX[0][0]) + (p01.getValue() * kernelX[0][1]) + (p02.getValue() * kernelX[0][2]) +
		            (p10.getValue() * kernelX[1][0]) + (p11.getValue() * kernelX[1][1]) + (p12.getValue() * kernelX[1][2]) +
		            (p20.getValue() * kernelX[2][0]) + (p21.getValue() * kernelX[2][1]) + (p22.getValue() * kernelX[2][2]);

		        int gy =
		            (p00.getValue() * kernelY[0][0]) + (p01.getValue() * kernelY[0][1]) + (p02.getValue() * kernelY[0][2]) +
		            (p10.getValue() * kernelY[1][0]) + (p11.getValue() * kernelY[1][1]) + (p12.getValue() * kernelY[1][2]) +
		            (p20.getValue() * kernelY[2][0]) + (p21.getValue() * kernelY[2][1]) + (p22.getValue() * kernelY[2][2]);

		        int magnitude = Math.abs(gx) + Math.abs(gy);

		        if (magnitude < threshold) {
		            magnitude = 0;
		        } else {
		            magnitude = 255;
		        }

		        EImage.setPixel(col, row, magnitude);		
		        }
		}
	}
	
	
	
	public AirwayImage getEdgeImage() {
		return EImage;
	}
	
	public void setThreshold(int threshold) {
	    this.threshold = threshold;
	    this.EImage = new AirwayImage(OImage.getWidth(), OImage.getHeight());
	    applySobel();
	}
	
	public void setKernelX(int[][] kernelX) {
	    this.kernelX = kernelX;
	    this.EImage = new AirwayImage(OImage.getWidth(), OImage.getHeight());
	    applySobel();
	}

	public void setKernelY(int[][] kernelY) {
	    this.kernelY = kernelY;
	    this.EImage = new AirwayImage(OImage.getWidth(), OImage.getHeight());
	    applySobel();
	}
	
}
