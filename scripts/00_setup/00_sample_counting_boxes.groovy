/**
 * Script:
 *   00_sample_counting_boxes.groovy
 *
 * Purpose:
 *   Randomly sample fixed-size counting boxes inside selected annotations.
 *
 * Workflow position:
 *   Step 0 — Setup / ROI preparation
 *
 * Input:
 *   - Selected annotation objects
 *   - Calibrated pixel size
 *
 * Output:
 *   - Child annotation objects named "Counting Box X"
 *
 * Recommended usage:
 *   Use for normalization patches or ROI sampling before downstream analysis.
 *
 * Notes:
 *   Existing counting boxes under the selected annotation will be removed.
 *   Visual QC is recommended after sampling.
 */

import qupath.lib.roi.ROIs
import qupath.lib.objects.PathObjects
import static qupath.lib.scripting.QP.*

// ================== [参数区：保持不变] ==================
def boxSize_μm = 100.0
def numBoxesPerPatch = 3
def rngSeed = 12345

// ======================================================

// 1. 获取当前图像上下文
def imageData = getCurrentImageData()
if (imageData == null) {
    println "❌ 错误：没有打开的图像！"
    return
}
def hierarchy = imageData.getHierarchy()
def server = imageData.getServer()

// 2. 核心修正：获取当前【选中】的标注对象 (而非遍历项目)
// 依据: QP.getSelectedObjects()
def selectedAnnotations = getSelectedObjects().findAll { it.isAnnotation() }

if (selectedAnnotations.isEmpty()) {
    println "⚠️ 警告：未选中任何 Annotation！请先在界面中选中一个区域（如 Normalization Patch）。"
    return
}

// 像素校准检查
def pixelCal = server.getPixelCalibration()
if (!pixelCal.hasPixelSizeMicrons()) {
    println "❌ 错误：图像未校准像素大小，无法根据 µm 计算框大小。"
    return
}
def pixelWidth = pixelCal.getPixelWidthMicrons()
def boxSize_pix = boxSize_μm / pixelWidth

println ">>> 开始在 ${selectedAnnotations.size()} 个选中区域中采样..."
println "    像素大小: ${pixelWidth} µm/px, 框大小: ${boxSize_pix} px"

// 3. 遍历选中的标注
selectedAnnotations.eachWithIndex { parentAnnot, idx ->
    println "\n处理选中区域: '${parentAnnot.getName() ?: '未命名'}'"
    
    def annotROI = parentAnnot.getROI()
    def annotGeometry = annotROI.getGeometry()
    def plane = annotROI.getImagePlane()
    
    def bx = annotROI.getBoundsX()
    def by = annotROI.getBoundsY()
    def bw = annotROI.getBoundsWidth()
    def bh = annotROI.getBoundsHeight()
    
    // 清除该区域下的旧 Counting Box
    def oldBoxes = parentAnnot.getChildObjects().findAll { it.getName()?.startsWith("Counting Box") }
    parentAnnot.removePathObjects(oldBoxes)
    println "    已清除 ${oldBoxes.size()} 个旧计数框。"
    
    def boxes = []
    def geometries = []
    def rng = new Random(rngSeed + idx) // 为每个区域设置不同种子
    int maxAttempts = 2000
    int foundCount = 0
    
    // 采样逻辑循环 (保持不变)
    while (foundCount < numBoxesPerPatch && maxAttempts > 0) {
        maxAttempts--
        def x = bx + rng.nextDouble() * (bw - boxSize_pix)
        def y = by + rng.nextDouble() * (bh - boxSize_pix)
        
        // 依据: ROIs.createRectangleROI
        def boxROI = ROIs.createRectangleROI(x, y, boxSize_pix, boxSize_pix, plane)
        def candidateGeometry = boxROI.getGeometry()
        
        // 检查：是否在标注内 && 是否与现有框重叠
        if (annotGeometry.contains(candidateGeometry)) {
            boolean overlaps = false
            for (existingGeom in geometries) {
                if (candidateGeometry.intersects(existingGeom)) {
                    overlaps = true
                    break
                }
            }
            if (!overlaps) {
                // 依据: PathObjects.createAnnotationObject
                def boxAnnot = PathObjects.createAnnotationObject(boxROI)
                boxAnnot.setName("Counting Box ${foundCount + 1}")
                boxAnnot.setColorRGB(getColorRGB(255, 165, 0)) // 橙色
                
                boxes.add(boxAnnot)
                geometries.add(candidateGeometry)
                foundCount++
            }
        }
    }
    
    // 添加到层级
    parentAnnot.addPathObjects(boxes)
    println "    ✅ 成功生成 ${foundCount} 个计数框。"
}

// 4. 刷新视图
fireHierarchyUpdate()
println "\n[任务完成] 采样已结束，请检查选中区域。"