package model;

import java.nio.file.Path;
import java.nio.file.Paths;
import SimilarityDetection.PipelineResult;
import SimilarityDetection.SimilarityFeatureExtractor;
import SimilarityDetection.SimilarityFeatures;

public class CaseProcessor {
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java CaseProcessor <caseNumber> <basePath>");
            System.out.println("Example: java CaseProcessor 1 C:\\Users\\USER\\Desktop\\AeroPath\\AeroPath");
            return;
        }
        
        String caseNum = args[0];
        String basePath = args[1];
        
        try {
            Path caseDir = Paths.get(basePath, caseNum);
            Path outputRoot = Paths.get("output");
            
            System.out.println("Processing case " + caseNum);
            
            // Convert
            ConvertCT.ConversionResult result = ConvertCT.convertCaseFromFolder(
                caseDir, outputRoot, true, -1000.0, 400.0
            );
            
            // Compress
            PngCaseCompressor.CompressionResult compression = 
                PngCaseCompressor.compressAirwayMasks(outputRoot.resolve(caseNum));
            
            // Extract features
            PipelineResult pipeline = SimilarityFeatureExtractor.runFullPipeline(compression.getFinalImage());
            SimilarityFeatures features = pipeline.getFeatures();
            
            // Print features
            System.out.println(caseNum + " | " + features.getBranchCount() + " | " + 
                features.getBranchPointCount() + " | " + features.getAverageTapering() + " | " +
                features.getAverageTortuosity() + " | " + features.getAbruptEndingRatio());
            
            System.out.println("DONE");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}