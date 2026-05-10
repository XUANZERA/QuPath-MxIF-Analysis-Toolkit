/**
 * Script:
 *   00_estimate_display_anchors_from_reference_patches.groovy
 *
 * Purpose:
 *   Estimate display normalization anchors from reference annotation patches.
 *
 * Workflow position:
 *   Step 1 — Normalization / threshold estimation
 *
 * Input:
 *   - Project images
 *   - Annotations named "Normalization Patch"
 *   - Target image channels
 *
 * Output:
 *   - Percentile-based display anchor estimates
 *   - Suggested normalization ranges
 *
 * Recommended usage:
 *   Run before segmentation and classification for intensity standardization.
 *
 * Notes:
 *   Requires calibrated MxIF images.
 *   Channel names may need adjustment for different datasets.
 */

import qupath.lib.regions.RegionRequest
import java.text.SimpleDateFormat
import static qupath.lib.scripting.QP.*

// --- 1. 参数定义 ---
def targetPatchName = "Normalization Patch"
def channelsToProcess = ["DAPI", "Opal 620"] 
double pLowVal = 1.0      // 1% 分位数
double pHighVal = 99.9    // 99.9% 分位数

def project = getProject()
// 按照名字排序并取前两个图像入口
def imageEntries = project.getImageList().sort { it.getImageName() }.take(2) 

if (imageEntries.size() < 2) {
    println "错误：项目内图像少于 2 张，请检查项目内容。"
    return
}

// 像素存储池
Map<String, List<Float>> pixelPools = channelsToProcess.collectEntries { [(it): []] }

println "正在从 2 张图像中提取像素数据..."

// --- 2. 遍历并提取 ---
imageEntries.each { entry ->
    // 正确读取项目条目的 ImageData 和 Hierarchy
    def imageData = entry.readImageData()
    def server = imageData.getServer()
    def hierarchy = imageData.getHierarchy()
    
    // 获取所有 Annotation，并进行更宽松的名字匹配（忽略大小写和首尾空格）
    def patches = hierarchy.getAnnotationObjects().findAll { 
        it.getName() != null && it.getName().trim().equalsIgnoreCase(targetPatchName)
    }
    
    println "图像 [${entry.getImageName()}]：找到 ${patches.size()} 个符合条件的 Patch"

    patches.take(3).each { patch ->
        def roi = patch.getROI()
        // 以原图分辨率（downsample = 1.0）读取区域
        def request = RegionRequest.createInstance(server.getPath(), 1.0, roi)
        
        channelsToProcess.each { channelName ->
            // 查找通道索引（不区分大小写）
            int channelIdx = server.getMetadata().getChannels().findIndexOf { 
                it.getName().toUpperCase().contains(channelName.toUpperCase()) 
            }
            
            if (channelIdx >= 0) {
                try {
                    def img = server.readBufferedImage(request)
                    def raster = img.getRaster()
                    int w = img.getWidth()
                    int h = img.getHeight()
                    float[] pixels = new float[w * h]
                    raster.getSamples(0, 0, w, h, channelIdx, pixels)
                    
                    // 快速存入池中
                    def list = pixelPools[channelName]
                    for (float p : pixels) {
                        list.add(p)
                    }
                } catch (Exception e) {
                    println "读取像素失败: " + e.getMessage()
                }
            } else {
                println "警告：在图像中未找到通道 [${channelName}]"
            }
        }
    }
}

// --- 3. 计算分位数并打印 ---
// 修正 Date.format 报错，使用 Java 原生 SimpleDateFormat
def sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
String timestamp = sdf.format(new Date())

println "\n" + "="*30
println "   归一化锚点参数 (Normalization Anchors)"
println "="*30
println "计算时间: ${timestamp}"

StringBuilder wechatMsg = new StringBuilder()
wechatMsg.append("📌【归一化参数 - 手动保存】\n")
wechatMsg.append("时间: ${timestamp}\n")

boolean hasData = false
pixelPools.each { channelName, pixels ->
    if (pixels.size() > 0) {
        hasData = true
        Collections.sort(pixels)
        int n = pixels.size()
        float pLow = pixels[(int) (n * pLowVal / 100.0)]
        float pHigh = pixels[(int) (n * pHighVal / 100.0)]
        
        String result = "通道: ${channelName}\n   - Min (1%): ${String.format('%.2f', pLow)}\n   - Max (99.9%): ${String.format('%.2f', pHigh)}\n"
        println result
        wechatMsg.append(result)
    }
}

if (!hasData) {
    println "错误：未能从任何 Patch 中提取到像素，请检查 Annotation 名字是否准确！"
} else {
    println "="*30
    println "请将下方文字复制到微信："
    println "\n" + wechatMsg.toString()
}
println "="*30