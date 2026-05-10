/**
 * Experimental module.
 *
 * Script:
 *   experimental_batch_cell_classification.groovy
 *
 * Purpose:
 *   Perform batch immune-cell classification across all project images.
 *
 * Workflow position:
 *   Step 3 — Batch cell classification
 *
 * Input:
 *   - Project images
 *   - Annotations named "Normalization Patch"
 *   - Cell intensity measurements
 *
 * Output:
 *   - Updated immune-cell classifications
 *
 * Recommended usage:
 *   Verify thresholds on selected annotations before running batch analysis.
 *
 * Notes:
 *   Thresholds and channel naming are dataset-dependent.
 *   Visual QC is strongly recommended after execution.
 */

import qupath.lib.images.ImageData
import static qupath.lib.scripting.QP.*
import static qupath.lib.gui.scripting.QPEx.*

// ================== [1. 参数区] ==================

// 分类器参数
def targetMeasurement = "Cell: Opal 620 mean"   
def classifierThreshold = 20.0      
def targetClassName = "immune_cell"   

// 亮度参数 (v0.4.6 固定锚点)
def global_DAPI_min = 1.0
def global_DAPI_max = 134.0
def global_CD3_min = 0.0
def global_CD3_max = 105.0

// ===============================================

def project = getProject()
if (project == null) {
    println "错误：当前没有打开的项目！"
    return
}

def imageList = project.getImageList()
println "找到项目中的图像数量: ${imageList.size()}"

// 遍历项目中的所有图像
imageList.eachWithIndex { entry, index ->
    println "\n>>> [${index+1}/${imageList.size()}] 正在处理图像: ${entry.getImageName()}"

    // 1. 读取图像数据
    def imageData = entry.readImageData()
    def hierarchy = imageData.getHierarchy()

    // 2. 查找所有名为 "Normalization Patch" 的 Annotation
    def patches = hierarchy.getAnnotationObjects().findAll { 
        it.getName() != null && it.getName().trim().equalsIgnoreCase("Normalization Patch")
    }

    if (patches.isEmpty()) {
        println "  跳过：未找到任何 Normalization Patch"
        return // 继续下一个图像
    }

    println "  找到 ${patches.size()} 个 Normalization Patch"

    // 3. 修正亮度显示（可选，用于后续查看时的归一化效果）
    try {
        setChannelDisplayRange(imageData, "DAPI", global_DAPI_min as double, global_DAPI_max as double)
        setChannelDisplayRange(imageData, "Opal 620", global_CD3_min as double, global_CD3_max as double)
        println "  ✔ 亮度设置已同步"
    } catch (Exception e) {
        println "  ⚠️ 亮度设置失败 (可能是通道名不对): " + e.getMessage()
    }

    // 4. 分类逻辑
    def immuneClass = getPathClass(targetClassName)
    def negativeClass = getPathClass(null)

    int totalImmune = 0
    int totalCells = 0
    def firstValidValue = null  // 用于采样调试

    patches.each { patch ->
        // 获取该 Annotation 下的所有细胞检测对象
        def detections = patch.getChildObjects().findAll { it.isDetection() }

        detections.each { det ->
            def val = det.getMeasurementList().getMeasurementValue(targetMeasurement)
            if (val != null) {
                totalCells++
                if (firstValidValue == null) firstValidValue = val

                if (val >= classifierThreshold) {
                    det.setPathClass(immuneClass)
                    totalImmune++
                } else {
                    det.setPathClass(negativeClass)
                }
            }
            // 若 val == null，则跳过该细胞（保持原类，不纳入统计）
        }
    }

    // 5. 保存图像数据
    entry.saveImageData(imageData)

    // 6. 输出统计信息
    println "  处理完成：有效细胞数 = ${totalCells}，免疫细胞 = ${totalImmune}"
    if (totalCells > 0) {
        def percentage = (totalImmune / totalCells * 100).round(2)
        println "  阳性率: ${percentage}%"
        println "  [DEBUG] 第一个有效细胞的测量值: ${firstValidValue} (阈值: ${classifierThreshold})"
    } else {
        println "  [DEBUG] 未找到任何含有测量值 '${targetMeasurement}' 的细胞"
    }
}

println "\n========== 全部图像处理完毕 =========="