/**
 * Debug utility.
 *
 * Script:
 *   99_debug_delete_immune_classification.groovy
 *
 * Purpose:
 *   Delete objects classified as "immune_cell" under the selected annotation.
 *
 * Input:
 *   - Selected parent annotation
 *
 * Output:
 *   - immune_cell objects removed
 *
 * Notes:
 *   This operation modifies the hierarchy permanently unless reverted manually.
 */

import static qupath.lib.scripting.QP.*

// 1. 获取当前选中的大 Annotation
def parentAnnot = getSelectedObject()
if (parentAnnot == null || !parentAnnot.isAnnotation()) {
    println "错误：请先选中那个‘大 Annotation’！"
    return
}

// 2. 获取该大框下的所有子对象
def allChildren = parentAnnot.getChildObjects()

// 3. 筛选出需要删除的对象
def toDelete = allChildren.findAll { 

    def pathClass = it.getPathClass()
    boolean isImmune = pathClass != null && pathClass.toString().contains("immune_cell")

    return isImmune
}

// 4. 执行删除操作
if (!toDelete.isEmpty()) {
    // removeObjects 会自动从层级和图像中移除这些对象
    removeObjects(toDelete, true) 
    fireHierarchyUpdate()
    println "成功删除：${toDelete.size()} 个对象（包含计数框和免疫细胞）。"
    println "已保留神经分割对象。"
} else {
    println "未发现符合条件的子对象。"
}