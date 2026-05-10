/**
 * Script:
 *   02_export_project_cell_measurements_batch.groovy
 *
 * Purpose:
 *   Export project-wide cell measurements into a combined CSV file.
 *
 * Workflow position:
 *   Step 6 — Export
 *
 * Input:
 *   - QuPath project
 *   - Cell / detection objects
 *
 * Output:
 *   - combined_cell_measurements.csv
 *
 * Recommended usage:
 *   Run after segmentation, classification, and spatial measurement steps.
 *
 * Notes:
 *   Missing distance measurements will be computed automatically if possible.
 *   Exported files are intended for downstream Python / R analysis.
 */

import qupath.lib.gui.tools.MeasurementExporter
import qupath.lib.objects.PathCellObject
import java.io.File

// --- 1. 参数设置 ---
def project = getProject()
if (project == null) {
    print "请先打开一个项目！"
    return
}

// 导出文件保存路径 (保存在项目文件夹下的 measurements.csv)
def outputPath = buildFilePath(PROJECT_BASE_DIR, "combined_cell_measurements.csv")
def outputFile = new File(outputPath)

// --- 2. 遍历项目确认/计算距离特征 ---
print "正在检查项目内所有图像的距离特征..."

for (entry in project.getImageList()) {
    def imageData = entry.readImageData()
    def hierarchy = imageData.getHierarchy()
    
    // 检查是否已有距离测量值（取前10个检测对象进行抽样检查）
    def detections = hierarchy.getDetectionObjects()
    if (detections.isEmpty()) continue
    
    // 检查第一个检测对象是否包含 "Distance to annotation" 开头的测量项
    boolean hasDistance = detections[0].getMeasurementList().getMeasurementNames().any { it.startsWith("Distance to annotation") }
    
    if (!hasDistance) {
        print "图像 [${entry.getImageName()}] 缺失距离特征，正在计算..."
        // 将该图像设为当前处理图像以运行插件
        // 使用 runPlugin 调用空间分析，参数留空使用默认设置（计算到所有标注的距离）
        // 或者直接调用 QPEx 的 addDistanceToAnnotations2D()
        try {
            // 需要在对应的 ImageData 上下文中运行
            // 这里使用更底层的命令确保在 headless 状态下也能运行
            addDistanceToAnnotations2D(imageData) 
            entry.saveImageData(imageData)
            print "图像 [${entry.getImageName()}] 距离计算完成并保存。"
        } catch (Exception e) {
            print "图像 [${entry.getImageName()}] 计算失败: " + e.getMessage()
        }
    }
}

// --- 3. 使用 MeasurementExporter 执行合并导出 ---
print "正在导出合并的 CSV 文件..."

def exporter = new MeasurementExporter()
    .imageList(project.getImageList())     // 导出项目中所有图像
    .separator(",")                        // 设置逗号分隔符（CSV）
    .exportType(PathCellObject.class)      // 导出类型：Cells
    .exportMeasurements(outputFile)        // 开始执行导出

print "任务完成！合并后的 CSV 文件已保存至: " + outputPath