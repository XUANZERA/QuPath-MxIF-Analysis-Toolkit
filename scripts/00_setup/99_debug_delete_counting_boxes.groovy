/**
 * Debug utility.
 *
 * Script:
 *   99_debug_delete_counting_boxes.groovy
 *
 * Purpose:
 *   Delete counting-box annotations under the selected parent annotation.
 *
 * Input:
 *   - Selected parent annotation
 *
 * Output:
 *   - Counting-box child annotations removed
 *
 * Notes:
 *   Uses regex matching for "Counting Box [1-8]".
 *   Visual confirmation is recommended before saving.
 */

import static qupath.lib.scripting.QP.*

// 1. 获取当前选中的 Annotation (作为父对象)
def parentAnnot = getSelectedObject()

// 安全检查：确保选中了对象且它是 Annotation
if (parentAnnot == null || !parentAnnot.isAnnotation()) {
    println "错误：请先选中一个作为父级的 Annotation！"
    return
}

// 2. 获取该 Annotation 下的所有直接子对象 (Next layer only)
def allChildren = parentAnnot.getChildObjects()

// 3. 筛选出名字匹配 "Counting Box 1" 到 "Counting Box 8" 的 Annotation
def toDelete = allChildren.findAll {
    // 检查是否为 Annotation
    if (!it.isAnnotation()) return false
    
    def name = it.getName()
    if (name == null) return false
    
    // 使用正则表达式匹配 "Counting Box " 后跟数字 1 到 8
    // ^ 表示开始，$ 表示结束
    return name ==~ /Counting Box [1-8]/
}

// 4. 执行删除操作
if (!toDelete.isEmpty()) {
    // removeObjects 是 QP 类的静态方法
    // 第二个参数为 true 表示如果这些框下面还有子对象，也一并删除
    removeObjects(toDelete, true)
    
    // 5. 必须调用此方法通知 QuPath 刷新界面显示
    fireHierarchyUpdate()
    
    println "成功删除 ${toDelete.size()} 个计数框。"
} else {
    println "未在选中区域下发现名为 'Counting Box 1-8' 的子对象。"
}