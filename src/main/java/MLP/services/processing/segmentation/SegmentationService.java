package MLP.services.processing.segmentation;


import MLP.exception.ErrorCode;
import MLP.exception.RecognitionException;
import MLP.model.HieroglyphRecognitionModel;
import MLP.services.fileManagerService.IFileManagerService;
import MLP.utility.ImageUtility;
import MLP.utility.RecognitionModelMapUtility;
import lombok.extern.log4j.Log4j2;
import marvin.color.MarvinColorModelConverter;
import marvin.image.MarvinImage;
import marvin.image.MarvinSegment;
import marvin.io.MarvinImageIO;
import marvin.math.MarvinMath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static MLP.services.processing.segmentation.common.SegmentationConstants.*;
import static marvinplugins.MarvinPluginCollection.floodfillSegmentation;
import static marvinplugins.MarvinPluginCollection.morphologicalClosing;

/**
 * Service for segmentation hieroglyph for simple codes
 * Usage: Marvin Framework
 * author: ElinaValieva on 06.04.2019
 */
@Log4j2
@Service
public class SegmentationService {

    private final IFileManagerService fileManagerService;

    private List<HieroglyphRecognitionModel> hieroglyphRecognitionModels;

    @Autowired
    public SegmentationService(IFileManagerService fileManagerService) {
        this.fileManagerService = fileManagerService;
    }

    public List<HieroglyphRecognitionModel> segment(String imagePath) throws RecognitionException, IOException {
        log.debug("Start segmenting process for image: {}", imagePath);
        MarvinImage loadImage = MarvinImageIO.loadImage(imagePath);

        if (loadImage == null)
            throw new RecognitionException(ErrorCode.ERROR_CODE_FILE_NOT_FOUND.getMessage());

        HieroglyphRecognitionModel hieroglyphRecognitionModel = RecognitionModelMapUtility.mapToModel(loadImage, imagePath);
        hieroglyphRecognitionModels = new ArrayList<>();

        MarvinImage image = loadImage.clone();
        filterGreen(image);
        MarvinImage marvinImage = MarvinColorModelConverter.rgbToBinary(image, THRESHOLD);
        morphologicalClosing(marvinImage.clone(), marvinImage, MarvinMath.getTrueMatrix(MATRIX_ROW_SIZE_X, MATRIX_ROW_SIZE_Y));
        image = MarvinColorModelConverter.binaryToRgb(marvinImage);
        MarvinSegment[] segments = floodfillSegmentation(image);

        IntStream.range(1, segments.length).forEach(i -> {
            MarvinSegment segmentResult = segments[i];
            formResult(hieroglyphRecognitionModel, segmentResult);
            loadImage.drawRect(segmentResult.x1, segmentResult.y1, segmentResult.width, segmentResult.height, COLOR_SEGMENTS);
        });
        MarvinImageIO.saveImage(loadImage, fileManagerService.getFileResourceDirectory(SEGMENTATION_RESULT_FILE_NAME));
        
        return hieroglyphRecognitionModels;
    }

    private void filterGreen(MarvinImage image) {
        IntStream.range(0, image.getHeight()).forEach(height ->
                IntStream.range(0, image.getWidth()).forEach(width -> {
                    int redColor = image.getIntComponent0(width, height);
                    int greenColor = image.getIntComponent1(width, height);
                    int blueColor = image.getIntComponent2(width, height);
                    if (greenColor > redColor * 1.5 && greenColor > blueColor * 1.5)
                        image.setIntColor(width, height, 255, 255, 255);
                })
        );
    }

    private boolean isAvailable(MarvinSegment marvinSegment) {
        return marvinSegment.area > THRESHOLD_FOR_RESING;
    }

    private void formResult(HieroglyphRecognitionModel hieroglyphRecognitionModel, MarvinSegment segmentResult) {
        if (!isAvailable(segmentResult))
            return;

        int[][] resizingVector = ImageUtility.resizeVector(hieroglyphRecognitionModel, segmentResult);
        HieroglyphRecognitionModel hieroglyphResizingRecognitionModel = RecognitionModelMapUtility.mapToModel(resizingVector);
        hieroglyphRecognitionModels.add(hieroglyphResizingRecognitionModel);
    }
}
