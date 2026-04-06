public class Pixel {
    private int x;
    private int y;
    private int value;

    public Pixel() {
        x = 0;
        y = 0;
        value = 0;
    }

    public Pixel(int x, int y, int value) {
        this.x = x;
        this.y = y;
        this.value = value;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        if (value < 0) {
            this.value = 0;
        }
        else if (value > 255) {
            this.value = 255;
        }
        else {
            this.value = value;
        }
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + "," + value + ")";
    }
}