/**
 * Script:
 *   01_classify_cd3_cells_selected_annotation.groovy
 *
 * Purpose:
 *   Classify immune cells using CD3 marker intensity inside the selected annotation.
 *
 * Workflow position:
 *   Step 3 — Cell classification
 *
 * Input:
 *   - Selected annotation object
 *   - Cell measurement: "Cell: Opal 620 mean"
 *
 * Output:
 *   - Cell classification labels such as "immune_cell"
 *
 * Recommended usage:
 *   Run after cell segmentation and verify classification visually.
 *
 * Notes:
 *   Thresholds are dataset-dependent and may require tuning.
 *   Channel display ranges are synchronized for visualization consistency.
 */

import qupath.lib.images.ImageData
import static qupath.lib.scripting.QP.*
import static qupath.lib.gui.scripting.QPEx.* // v0.4.6 必须导入此项以修正亮度设置

// ================== [1. 参数区] ==================

// 分类器参数
def targetMeasurement = "Cell: Opal 620 mean"   
def classifierThreshold = 10
def targetClassName = "immune_cell"   

// 亮度参数 (v0.4.6 固定锚点)
def global_DAPI_min = 1.0
def global_DAPI_max = 134.0
def global_CD3_min = 0.0
def global_CD3_max = 105.0

// ===============================================

// 1. 获取当前图像和选中的对象
def imageData = getCurrentImageData()
def selectedObject = getSelectedObject()

if (selectedObject == null || !selectedObject.isAnnotation()) {
    println "❌ 错误：请先在图像中【点击选中】一个 Annotation 区域！"
    return
}

println ">>> 开始处理选中区域: ${selectedObject.getName() ?: '未命名'}"

// 2. 修正亮度显示 (使用 v0.4.6 正确的 API: setChannelDisplayRange)
// 这样你分类完观察时，颜色对比是基于你的固定锚点的
try {
    setChannelDisplayRange(imageData, "DAPI", global_DAPI_min as double, global_DAPI_max as double)
    setChannelDisplayRange(imageData, "Opal 620", global_CD3_min as double, global_CD3_max as double)
    println "  ✔ 亮度设置已同步"
} catch (Exception e) {
    println "  ⚠️ 亮度设置失败 (可能是通道名不对): " + e.getMessage()
}

// 3. 执行分类逻辑
def immuneClass = getPathClass(targetClassName)
def negativeClass = getPathClass(null)

// 获取该选中区域下属的所有细胞
def detections = selectedObject.getChildObjects().findAll { it.isDetection() }

int countImmune = 0
int countTotal = 0

println "--- 正在对 ${detections.size()} 个细胞进行分类 ---"

detections.each { det ->
    // 获取测量值
    def val = det.getMeasurementList().getMeasurementValue(targetMeasurement)
    
    if (val != null) {
        countTotal++
        // 应用阈值
        if (val >= classifierThreshold) {
            det.setPathClass(immuneClass)
            countImmune++
        } else {
            det.setPathClass(negativeClass)
        }
    }
}

// 4. 更新界面并提示保存
fireHierarchyUpdate() // 立即在屏幕上刷新颜色变化

println "--- 处理完成 ---"
println "  区域名称: ${selectedObject.getName()}"
println "  有效细胞数: ${countTotal}"
println "  判定为 ${targetClassName} 的数量: ${countImmune}"
println "  阳性率: ${countTotal > 0 ? (countImmune/countTotal*100).round(2) : 0}%"
println "  💡 请按 Ctrl+S 保存结果到项目。"

// 采样调试：如果还是全阳性，看这里输出的数值
if (countTotal > 0) {
    def sampleVal = detections[0].getMeasurementList().getMeasurementValue(targetMeasurement)
    println "  [DEBUG] 采样第一个细胞的原始数值为: ${sampleVal} (你的阈值是 ${classifierThreshold})"
}