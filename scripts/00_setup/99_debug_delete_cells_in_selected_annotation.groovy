/**
 * Debug utility.
 *
 * Script:
 *   99_debug_delete_cells_in_selected_annotation.groovy
 *
 * Purpose:
 *   Delete detection objects inside the selected annotation.
 *
 * Input:
 *   - Selected annotation object
 *
 * Output:
 *   - Detection objects removed from hierarchy
 *
 * Notes:
 *   This operation cannot be easily undone.
 *   Save backups before use.
 */

import static qupath.lib.scripting.QP.*

// 1. 获取当前选中的对象
def selected = getSelectedObject()

// 验证选中对象是否存在且为 Annotation
if (selected == null || !selected.isAnnotation()) {
    println "错误：请先选中一个 Annotation 区域！"
    return
}

// 2. 获取该 Annotation 下所有的子对象，并筛选出检测对象（Detections）
// 在 QuPath 中，细胞通常被视为检测对象 (Detection Objects)
def toRemove = selected.getChildObjects().findAll { it.isDetection() }

// 如果只想删除明确定义为“细胞”的对象，可以使用：
// def toRemove = selected.getChildObjects().findAll { it.isCell() }

// 3. 执行删除操作
// 第二个参数 true 表示保留这些被删除对象的子对象（如果存在），false 表示一并删除。
// 细胞通常没有子对象，因此建议设为 true 以符合通用脚本习惯。
removeObjects(toRemove, true)

println "已从选中区域删除 ${toRemove.size()} 个检测对象。"