import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.imageio.ImageIO;

import com.ericbarnhill.niftijio.NiftiVolume;

public class ConvertCT {

    public static void main(String[] args) {
        try {
            Config config = parseArgs(args);
            Path caseDir = Paths.get(config.baseDir, config.caseNumber);
            Path outputRoot = Paths.get(config.outputRoot);

            ConversionResult result = convertCaseFromFolder(
                    caseDir,
                    outputRoot,
                    config.onlyNonemptyAirway,
                    config.clipMin,
                    config.clipMax
            );

            System.out.println("Done. Saved " + result.savedCount + " coronal slices into: " + result.outputDir);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class ConversionResult {
        public final int savedCount;
        public final Path outputDir;

        public ConversionResult(int savedCount, Path outputDir) {
            this.savedCount = savedCount;
            this.outputDir = outputDir;
        }
    }

    static class Config {
        String caseNumber;
        String baseDir = "data";
        String outputRoot = "output";
        boolean onlyNonemptyAirway = false;
        double clipMin = -1000.0;
        double clipMax = 400.0;
    }

    private static Config parseArgs(String[] args) {
        Config config = new Config();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            switch (arg) {
                case "--case":
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("Missing value after --case");
                    }
                    config.caseNumber = args[++i];
                    break;

                case "--base_dir":
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("Missing value after --base_dir");
                    }
                    config.baseDir = args[++i];
                    break;

                case "--output_root":
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("Missing value after --output_root");
                    }
                    config.outputRoot = args[++i];
                    break;

                case "--only_nonempty_airway":
                    config.onlyNonemptyAirway = true;
                    break;

                case "--clip_min":
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("Missing value after --clip_min");
                    }
                    config.clipMin = Double.parseDouble(args[++i]);
                    break;

                case "--clip_max":
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("Missing value after --clip_max");
                    }
                    config.clipMax = Double.parseDouble(args[++i]);
                    break;

                default:
                    throw new IllegalArgumentException("Unknown argument: " + arg);
            }
        }

        if (config.caseNumber == null) {
            throw new IllegalArgumentException("Required argument missing: --case");
        }

        return config;
    }

    public static ConversionResult convertCaseFromFolder(
            Path caseDir,
            Path outputRoot,
            boolean onlyNonemptyAirway,
            double clipMin,
            double clipMax) throws Exception {

        if (caseDir == null || !Files.exists(caseDir) || !Files.isDirectory(caseDir)) {
            throw new IOException("Selected case folder is invalid: " + caseDir);
        }

        String caseNumber = caseDir.getFileName().toString();

        Path ctPath = findNiftiFile(caseDir, caseNumber + "_CT_HR");
        Path airwayPath = findNiftiFile(caseDir, caseNumber + "_CT_HR_label_airways");
        Path lungPath = findNiftiFile(caseDir, caseNumber + "_CT_HR_label_lungs");

        double[][][] ct = loadVolume(ctPath);
        double[][][] airway = loadVolume(airwayPath);
        double[][][] lung = loadVolume(lungPath);

        checkSameShape(ct, airway, "CT", "airway");
        checkSameShape(ct, lung, "CT", "lung");

        int yDim = ct[0].length;

        Path outDir = outputRoot.resolve(caseNumber);
        Path ctDir = outDir.resolve("ct_coronal_png");
        Path airwayDir = outDir.resolve("airway_coronal_png");
        Path overlayDir = outDir.resolve("overlay_coronal_png");

        Files.createDirectories(ctDir);
        Files.createDirectories(airwayDir);
        Files.createDirectories(overlayDir);

        int savedCount = 0;

        for (int y = 0; y < yDim; y++) {
            double[][] ctSlice = getCoronalSlice(ct, y);
            double[][] airwaySlice = getCoronalSlice(airway, y);
            double[][] lungSlice = getCoronalSlice(lung, y);

            boolean[][] airwayMask = greaterThanZero(airwaySlice);
            boolean[][] lungMask = greaterThanZero(lungSlice);

            if (onlyNonemptyAirway && !anyTrue(airwayMask)) {
                continue;
            }

            double[][] maskedCtSlice = applyMaskOrFill(ctSlice, lungMask, clipMin);

            int[][] ctU8 = normalizeCtSlice(maskedCtSlice, clipMin, clipMax);
            int[][] airwayU8 = maskToUInt8(airwayMask);

            ctU8 = rotate90CounterClockwise(ctU8);
            airwayU8 = rotate90CounterClockwise(airwayU8);

            boolean[][] airwayMaskDisplay = intImageToMask(airwayU8);
            int[][][] overlay = makeOverlay(ctU8, airwayMaskDisplay);

            String filename = String.format("coronal_%03d.png", savedCount);

            saveGrayPng(ctU8, ctDir.resolve(filename));
            saveGrayPng(airwayU8, airwayDir.resolve(filename));
            saveRgbPng(overlay, overlayDir.resolve(filename));

            savedCount++;
        }

        return new ConversionResult(savedCount, outDir);
    }

    private static Path findNiftiFile(Path caseDir, String baseName) throws IOException {
        Path nii = caseDir.resolve(baseName + ".nii");
        Path niiGz = caseDir.resolve(baseName + ".nii.gz");

        if (Files.exists(nii)) return nii;
        if (Files.exists(niiGz)) return niiGz;

        throw new IOException("Could not find " + baseName + ".nii or .nii.gz in " + caseDir);
    }

    private static double[][][] loadVolume(Path path) throws IOException {
        NiftiVolume volume = NiftiVolume.read(path.toString());

        int xDim = volume.header.dim[1];
        int yDim = volume.header.dim[2];
        int zDim = volume.header.dim[3];

        double[][][] out = new double[xDim][yDim][zDim];

        for (int x = 0; x < xDim; x++) {
            for (int y = 0; y < yDim; y++) {
                for (int z = 0; z < zDim; z++) {
                    out[x][y][z] = volume.data.get(x, y, z, 0);
                }
            }
        }

        return out;
    }

    private static void checkSameShape(double[][][] a, double[][][] b, String aName, String bName) {
        if (a.length != b.length || a[0].length != b[0].length || a[0][0].length != b[0][0].length) {
            throw new IllegalArgumentException("Shape mismatch: " + aName + " != " + bName);
        }
    }

    private static double[][] getCoronalSlice(double[][][] volume, int yIndex) {
        int xDim = volume.length;
        int zDim = volume[0][0].length;

        double[][] slice = new double[xDim][zDim];

        for (int x = 0; x < xDim; x++) {
            for (int z = 0; z < zDim; z++) {
                slice[x][z] = volume[x][yIndex][z];
            }
        }

        return slice;
    }

    private static boolean[][] greaterThanZero(double[][] slice) {
        int h = slice.length;
        int w = slice[0].length;
        boolean[][] mask = new boolean[h][w];

        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                mask[i][j] = slice[i][j] > 0.0;
            }
        }

        return mask;
    }

    private static boolean anyTrue(boolean[][] mask) {
        for (boolean[] row : mask) {
            for (boolean value : row) {
                if (value) return true;
            }
        }
        return false;
    }

    private static double[][] applyMaskOrFill(double[][] slice, boolean[][] mask, double fillValue) {
        int h = slice.length;
        int w = slice[0].length;
        double[][] out = new double[h][w];

        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                out[i][j] = mask[i][j] ? slice[i][j] : fillValue;
            }
        }

        return out;
    }

    private static int[][] normalizeCtSlice(double[][] ctSlice, double clipMin, double clipMax) {
        int h = ctSlice.length;
        int w = ctSlice[0].length;
        int[][] out = new int[h][w];

        double denom = clipMax - clipMin;
        if (denom == 0.0) denom = 1e-8;

        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                double value = ctSlice[i][j];

                if (value < clipMin) value = clipMin;
                if (value > clipMax) value = clipMax;

                double scaled = (value - clipMin) / denom;
                if (scaled < 0.0) scaled = 0.0;
                if (scaled > 1.0) scaled = 1.0;

                out[i][j] = (int) Math.round(scaled * 255.0);
            }
        }

        return out;
    }

    private static int[][] maskToUInt8(boolean[][] mask) {
        int h = mask.length;
        int w = mask[0].length;
        int[][] out = new int[h][w];

        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                out[i][j] = mask[i][j] ? 255 : 0;
            }
        }

        return out;
    }

    private static boolean[][] intImageToMask(int[][] img) {
        int h = img.length;
        int w = img[0].length;
        boolean[][] mask = new boolean[h][w];

        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                mask[i][j] = img[i][j] > 0;
            }
        }

        return mask;
    }

    private static int[][] rotate90CounterClockwise(int[][] img) {
        int h = img.length;
        int w = img[0].length;
        int[][] out = new int[w][h];

        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                out[w - 1 - j][i] = img[i][j];
            }
        }

        return out;
    }

    private static int[][][] makeOverlay(int[][] ctGray, boolean[][] airwayMask) {
        int h = ctGray.length;
        int w = ctGray[0].length;
        int[][][] rgb = new int[h][w][3];

        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                int gray = ctGray[i][j];
                rgb[i][j][0] = gray;
                rgb[i][j][1] = gray;
                rgb[i][j][2] = gray;

                if (airwayMask[i][j]) {
                    rgb[i][j][0] = 255;
                    rgb[i][j][1] = Math.min(rgb[i][j][1], 80);
                    rgb[i][j][2] = Math.min(rgb[i][j][2], 80);
                }
            }
        }

        return rgb;
    }

    private static void saveGrayPng(int[][] img, Path path) throws IOException {
        int h = img.length;
        int w = img[0].length;

        BufferedImage buffered = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster = buffered.getRaster();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                raster.setSample(x, y, 0, img[y][x]);
            }
        }

        ImageIO.write(buffered, "png", path.toFile());
    }

    private static void saveRgbPng(int[][][] rgb, Path path) throws IOException {
        int h = rgb.length;
        int w = rgb[0].length;

        BufferedImage buffered = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int r = clamp255(rgb[y][x][0]);
                int g = clamp255(rgb[y][x][1]);
                int b = clamp255(rgb[y][x][2]);
                int packed = (r << 16) | (g << 8) | b;
                buffered.setRGB(x, y, packed);
            }
        }

        ImageIO.write(buffered, "png", path.toFile());
    }

    private static int clamp255(int value) {
        if (value < 0) return 0;
        if (value > 255) return 255;
        return value;
    }
}
