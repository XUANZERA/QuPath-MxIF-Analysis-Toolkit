/**
 * Script:
 *   01_merge_counting_boxes_to_parent.groovy
 *
 * Purpose:
 *   Merge all counting-box annotations into a target parent annotation hierarchy.
 *
 * Workflow position:
 *   Step 0 — Setup / hierarchy organization
 *
 * Input:
 *   - Annotation named "Normalization Patch"
 *   - Child annotations starting with "Counting Box"
 *
 * Output:
 *   - Updated annotation hierarchy
 *
 * Recommended usage:
 *   Run after counting-box generation if hierarchy organization is incorrect.
 *
 * Notes:
 *   This script modifies QuPath hierarchy relationships.
 *   Save the project after execution.
 */

import static qupath.lib.scripting.QP.*

// --- 1. 配置参数 ---
def parentName = "Normalization Patch"
def childNamePrefix = "Counting Box"

// --- 2. 获取当前图像数据 ---
def imageData = getCurrentImageData()
if (imageData == null) {
    println "❌ 错误：没有打开的图像！"
    return
}
def hierarchy = imageData.getHierarchy()

println ">>> 正在当前图像中合并层级: ${getProjectEntry()?.getImageName() ?: '未保存的图像'}"

// --- 3. 定位父对象 (Normalization Patch) ---
def parentAnnotation = hierarchy.getAnnotationObjects().find { 
    it.getName()?.trim()?.equalsIgnoreCase(parentName) 
}

if (parentAnnotation == null) {
    println "  ⏭️ 跳过：找不到名为 '${parentName}' 的 Annotation。请检查名称是否准确。"
    return
}

// --- 4. 查找所有待移动的 Counting Box ---
// 排除父对象本身，且只查找名称开头的盒子
def boxesToMove = hierarchy.getAnnotationObjects().findAll { 
    it.getName()?.startsWith(childNamePrefix) && it != parentAnnotation
}

if (boxesToMove.isEmpty()) {
    println "  ⏭️ 跳过：未发现名称以 '${childNamePrefix}' 开头的对象。"
    return
}

// --- 5. 执行核心层级操作 ---
// 依据: PathObject.addPathObjects() 会自动处理层级关系转移
parentAnnotation.addPathObjects(boxesToMove)
println "  ✅ 成功将 ${boxesToMove.size()} 个 Counting Box 移动到 ${parentName} 下"

// --- 6. 刷新界面显示 ---
// 依据: QP.fireHierarchyUpdate()
fireHierarchyUpdate()

println "\n[任务完成] 请查看左侧层级面板（Hierarchy）确认合并效果。"
println "💡 记得按 Ctrl+S 保存更改。"