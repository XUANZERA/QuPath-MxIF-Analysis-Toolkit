/**
 * Script:
 *   01_move_nerve_regions_to_parent_annotation.groovy
 *
 * Purpose:
 *   Organize nerve-region annotations under a selected parent annotation.
 *
 * Workflow position:
 *   Step 4 — Nerve region organization
 *
 * Input:
 *   - Selected parent annotation
 *   - Annotation objects classified as "nerve_regions"
 *
 * Output:
 *   - Updated annotation hierarchy
 *
 * Recommended usage:
 *   Use after nerve segmentation or manual annotation editing.
 *
 * Notes:
 *   Existing hierarchy relationships will be modified.
 *   Verify results in the Hierarchy panel.
 */

import qupath.lib.objects.PathObject
import static qupath.lib.scripting.QP.*

/**
 * 针对当前选中的标注，将全图中分类为 nerve_regions 的标注归入其层级下
 * 适用版本：QuPath v0.5.1
 */

// 1. 获取当前图像和选中的标注
def imageData = getCurrentImageData()
if (imageData == null) {
    println "❌ 错误：没有打开的图像！"
    return
}

def parentAnnotation = getSelectedObject()

// 验证选中对象
if (parentAnnotation == null || !parentAnnotation.isAnnotation()) {
    println "❌ 错误：请先在图像中【选中】作为父层级的标注（即 Normalization Patch）。"
    return
}

def hierarchy = imageData.getHierarchy()
def targetClassName = "nerve_regions"
def parentName = parentAnnotation.getName() ?: "未命名标注"

println ">>> 开始处理层级合并"
println "父层级对象: ${parentName}"
println "目标子分类: ${targetClassName}"

// 2. 找出全图中所有分类为 nerve_regions 的标注
// 注意：排除掉父标注本身，防止自循环
def nerveRegions = hierarchy.getAnnotationObjects().findAll { 
    it.getPathClass() != null && 
    it.getPathClass().getName() == targetClassName &&
    it != parentAnnotation
}

if (nerveRegions.isEmpty()) {
    println "  ⚠️ 提示：全图中未发现分类为 ${targetClassName} 的标注。"
    return
}

println "  找到 ${nerveRegions.size()} 个 ${targetClassName} 标注，准备移动..."

// 3. 执行层级移动
// 在 v0.5.1 中，直接使用 addPathObjects 是最标准、最简洁的做法
try {
    // 这一步会自动处理 removeObject(child) 和 addChildObject(child)
    parentAnnotation.addPathObjects(nerveRegions)
    
    // 强制刷新层级视图，让左侧列表刷新
    fireHierarchyUpdate()
    
    println "✅ 成功移动 ${nerveRegions.size()} 个标注到 [${parentName}] 下方。"
    println "💡 提示：请查看左侧 Hierarchy 面板，确认 ${parentName} 左侧是否出现了可展开的箭头。"
    
} catch (Exception e) {
    println "  ❌ 移动过程中出错: ${e.getMessage()}"
}

// 4. 可选：自动解析几何包含关系（如果需要更严格的检查）
// hierarchy.resolveHierarchy()