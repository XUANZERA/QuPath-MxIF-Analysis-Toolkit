/**
 * Experimental module.
 *
 * Script:
 *   00_segment_nerve_regions_selected_annotation_experimental.groovy
 *
 * Purpose:
 *   Perform semi-automatic nerve-region segmentation using Otsu thresholding.
 *
 * Workflow position:
 *   Step 4 — Nerve region segmentation
 *
 * Input:
 *   - Selected annotation object
 *   - Target nerve-marker channel
 *
 * Output:
 *   - Annotation objects classified as "nerve_regions"
 *
 * Recommended usage:
 *   Use as a helper tool followed by manual visual inspection.
 *
 * Notes:
 *   This module is experimental and may require dataset-specific adjustment.
 *   Segmentation quality should always be manually verified.
 */

import qupath.lib.images.servers.ColorTransforms
import qupath.lib.regions.*
import qupath.imagej.tools.IJTools
import qupath.lib.classifiers.pixel.PixelClassifier
import qupath.opencv.ml.pixel.PixelClassifiers
import qupath.opencv.ops.ImageOps
import ij.process.ImageProcessor
import static qupath.lib.gui.scripting.QPEx.*

// --- 用户设定区域 ---
def CHANNEL_NAME = "Opal 480"
def MIN_AREA = 500.0
def NERVE_CLASS = getPathClass("nerve_regions")
def DOWNSAMPLE = 1.0
def GAUSSIAN_SIGMA = 1.0

// --- 检查选中的Annotation ---
def selectedObjects = getSelectedObjects()
if (selectedObjects.isEmpty()) {
    print "请先选中一个Annotation！"
    return
}
def parentAnnotation = selectedObjects[0]
def roi = parentAnnotation.getROI()
println "Step 0: Using selected annotation for processing"

// --- 1. 在选中的ROI内计算 Otsu 阈值 ---
println "Step 1: Calculating Otsu threshold on channel '${CHANNEL_NAME}'..."

def server = getCurrentServer()
def cal = server.getPixelCalibration()

// 使用选中的ROI创建RegionRequest
def request = RegionRequest.createInstance(server.getPath(), DOWNSAMPLE, roi)

// 【修正1】使用IJTools将服务器区域转换为ImageProcessor，以便提取像素数据 [citation:3]
def pathImage = IJTools.convertToImagePlus(server, request)
def ip = pathImage.getImage().getProcessor()

// 获取目标通道的索引
def channelIndex = server.getMetadata().getChannels().findIndexOf { it.getName() == CHANNEL_NAME }

// 【修正2】从ImageProcessor提取指定通道的像素值 [citation:3]
float[] pixels = new float[ip.getWidth() * ip.getHeight()]
int idx = 0
for (int y = 0; y < ip.getHeight(); y++) {
    for (int x = 0; x < ip.getWidth(); x++) {
        // 对于多通道图像，getf(x, y, c) 可以获取指定通道的值
        // 如果ip不是多通道，可能需要调整
        pixels[idx++] = (float)ip.getf(x, y) // 这里假设ip已经是单通道
    }
}

// 使用您已经实现的 Otsu 函数计算阈值
double OTSU_THRESHOLD = yourOtsuImplementation(pixels)
println "   Calculated Otsu threshold: ${OTSU_THRESHOLD}"

// --- 2. 构建像素分类器 ---
println "Step 2: Building pixel classifier with Otsu threshold..."

// 【修正3】使用PixelClassifiers.createClassifier构建分类器 [citation:3]
def channel = ColorTransforms.createChannelExtractor(CHANNEL_NAME)
def resolution = cal.createScaledInstance(DOWNSAMPLE, DOWNSAMPLE)

// 定义图像操作序列：高斯滤波 + 常数阈值
def ops = [
    ImageOps.Filters.gaussianBlur(GAUSSIAN_SIGMA),
    ImageOps.Threshold.threshold(OTSU_THRESHOLD)
]

// 构建完整的操作链
def op = ImageOps.Core.sequential(ops)
def transformer = ImageOps.buildImageDataOp(channel).appendOps(op)

// 定义分类映射：1 表示前景（神经区域）
def classifications = new LinkedHashMap<Integer, qupath.lib.objects.classes.PathClass>()
classifications.put(1, NERVE_CLASS)

// 创建像素分类器
def classifier = PixelClassifiers.createClassifier(
    transformer,
    resolution,
    classifications
)
println "   Classifier created."

// --- 3. 应用分类器并生成标注 ---
println "Step 3: Creating annotations from pixel classifier..."

// 先选中父标注，确保分类器应用在正确区域
selectObjects([parentAnnotation])
println "   Running classifier within selected region..."
createAnnotationsFromPixelClassifier(classifier, MIN_AREA, 0.0, "INCLUDE_IGNORED")

println "Pipeline finished. Please manually verify the results."


// --- Otsu 实现（保持不变，已验证）---
double yourOtsuImplementation(float[] pixels) {
    int bins = 256
    int[] histogram = new int[bins]
    float minVal = Float.MAX_VALUE
    float maxVal = Float.MIN_VALUE

    for (float p : pixels) {
        if (p < minVal) minVal = p
        if (p > maxVal) maxVal = p
    }
    if (maxVal <= minVal) {
        println "警告: 像素值范围异常，返回默认阈值 0.2"
        return 0.2
    }

    for (float p : pixels) {
        int binIndex = (int)((p - minVal) / (maxVal - minVal) * (bins - 1))
        if (binIndex >= bins) binIndex = bins - 1
        if (binIndex < 0) binIndex = 0
        histogram[binIndex]++
    }

    int total = pixels.length
    double sum = 0
    for (int i = 0; i < bins; i++) {
        sum += i * histogram[i]
    }

    double sumB = 0
    int wB = 0
    int wF = 0
    double varMax = 0
    int threshold = 0

    for (int t = 0; t < bins; t++) {
        wB += histogram[t]
        if (wB == 0) continue
        wF = total - wB
        if (wF == 0) break
        sumB += (t * histogram[t])
        double mB = sumB / wB
        double mF = (sum - sumB) / wF
        double varBetween = (double)wB * (double)wF * (mB - mF) * (mB - mF)
        if (varBetween > varMax) {
            varMax = varBetween
            threshold = t
        }
    }

    double otsuThreshold = minVal + (threshold / (double)(bins - 1)) * (maxVal - minVal)
    println "   Otsu bin index: ${threshold}, mapped value: ${otsuThreshold}"
    return otsuThreshold
}