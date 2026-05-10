# 🧫 QuPath MxIF Analysis Toolkit

<div align="center">

<a href="#chinese">中文</a> | <a href="#english">English</a>

[![QuPath](https://img.shields.io/badge/QuPath-v0.5.1-blue.svg)]()
[![Language](https://img.shields.io/badge/Language-Groovy-orange.svg)]()
[![Workflow](https://img.shields.io/badge/Workflow-MxIF%20WSI-success.svg)]()
[![Status](https://img.shields.io/badge/Status-Research%20Use-important.svg)]()
[![License](https://img.shields.io/badge/License-MIT-green.svg)]()

</div>

---

<a id="chinese"></a>

# 🧫 QuPath MxIF 分析工具包（中文）

<div align="center">

**半自动化 QuPath 工作流 · 多重免疫荧光全切片图像分析**

**🔬 面向肿瘤-神经-免疫微环境研究**

</div>

---

## 📌 工具包能做什么？

- 🧪 **DAPI 细胞核分割**
- 🏷️ **基于标记物的细胞分类** (CD3·CK)
- 📏 **参考区域归一化与阈值估计**
- 🧠 **神经区域组织** (分割或标注整理)
- 📐 **距离测量**
- 📤 **CSV 与 GeoJSON 导出**
- 🛠️ **层级修复工具**

---

## 🎯 设计用途

本项目专为多重免疫荧光全切片图像的计算病理学工作流设计，重点分析**神经周围浸润 (PNI)** 区域的**肿瘤-神经-免疫微环境**。  
工作流为半自动流程，需要手动绘制标注并进行视觉质控。

---

## 🧠 为什么需要这个工作流？

在 MxIF 全切片图像分析中：

- 固定阈值容易受到批次效应与自发荧光影响
- annotation 与 hierarchy 经常混乱
- 神经区域与免疫区域关系复杂
- 下游空间统计需要结构化导出

本工具包的目标是：

> 将病理人工标注与可复现的计算特征提取连接起来。

---

## ⚙️ 环境要求

| 项目 | 说明 |
|------|------|
| **QuPath** | v0.5.1 |
| **图像** | 已校准的 MxIF 全切片图像 |
| **脚本引擎** | 启用 Groovy 脚本 |

### 🧬 推荐图像通道

| 生物学靶标 | 默认通道名 |
|------------|----------------|
| 细胞核 | `DAPI` / `DAPI (C1)` |
| 免疫标记 | `Opal 620` |
| 肿瘤标记 | `Opal 780 (C6)` |
| 神经标记 | `Opal 480` |

---

## 🏷️ 标注规范

### 必需标注类

| 类名 | 含义 |
|------|------|
| `PNI-` | PNI 阴性区域 |
| `PNI+: Immune Responsive` | PNI 阳性伴免疫应答 |
| `PNI+: Immune Excluded` | PNI 阳性伴免疫排斥 |

### 可选辅助标注

| 名称 / 类 | 含义 |
|------------|------|
| `Normalization Patch` | 用于强度归一化或质控的区域 |
| `dark_ref` | 暗场 / 背景参考区域 |
| `bright_ref` | 亮场 / 阳性参考区域 |
| `nerve_regions` | 神经区域标注 |

---

## 🔄 工作流全景

```mermaid
flowchart LR
    A[手动标注] --> B[归一化 / 阈值锚点]
    B --> C[DAPI 细胞分割]
    C --> D[基于标记物的细胞分类]
    D --> E[神经区域组织]
    E --> F[距离测量]
    F --> G[CSV 与 GeoJSON 导出]
    G --> H[下游空间分析]
```

> *建议始终对分割与分类结果进行可视化检查。*

---

## 🚀 Quick Start（推荐）

### Step 1 — 绘制 PNI Annotation

至少创建：

- `PNI-`
- `PNI+: Immune Responsive`
- `PNI+: Immune Excluded`

---

### Step 2 — 运行细胞分割

先运行：

`scripts/02_cell_segmentation/00_segment_cells_selected_annotations.groovy`

用于调试参数。

---

### Step 3 — 运行细胞分类

运行：

`scripts/03_cell_classification/01_classify_cd3_cells_selected_annotations.groovy`

---

### Step 4 — 导出结果

运行：

`scripts/06_export/02_export_project_cell_measurements_batch.groovy`

---

### Step 5 — 下游分析

在 Python / R 中进行空间统计分析。

---

## 🖼️ Example Results

### Cell segmentation

![segmentation](docs/screenshots/cell_segmentation_example.png)

---

### Cell classification

![classification](docs/screenshots/classification_example.png)

---

### QuPath hierarchy organization

![hierarchy](docs/screenshots/hierarchy_example.png)

---

## 📁 仓库结构

```
QuPath-MxIF-Analysis-Toolkit/
├── README.md
├── LICENSE
├── .gitignore
├── docs/
│   ├── annotation_convention.md
│   ├── channel_convention.md
│   ├── output_schema.md
│   ├── troubleshooting.md
│   └── screenshots/
├── scripts/
│   ├── 00_setup/
│   ├── 01_normalization/
│   ├── 02_cell_segmentation/
│   ├── 03_cell_classification/
│   ├── 04_nerve_region/
│   ├── 05_spatial_measurement/
│   ├── 06_export/
│   ├── 99_debug_utils/
│   └── experimental/
```

---

## 🧩 脚本模块详解

### 00_setup – 准备

标注层级工具、统计框数、建立神经区域的父-子关系。

### 01_normalization – 归一化

利用参考区域估算显示锚点与自适应阈值。

### 02_cell_segmentation – 细胞分割

基于 DAPI 的细胞分割。  
👉 建议先在选中标注上运行以调试参数，再批量运行。

### 03_cell_classification – 细胞分类

按标记强度分类：

- `immune_cell`
- `tumor_cell`
- `unclassified` / `none`

### 04_nerve_region – 神经区域

神经区域分割与层级整理。  
⚠️ 神经分割脚本属于实验性模块，运行后需视觉检查。

### 05_spatial_measurement – 空间测量

计算细胞到神经区域标注的距离。

### 06_export – 数据导出

导出细胞级测量值与标注级几何信息。

| 输出文件 | 描述 |
|----------|------|
| `export_cells_by_annotation_*.csv` | 细胞级测量值，按父级 PNI 分组 |
| `geojson_<annotation_id>.geojson` | 每个标注关联的神经区域几何 |
| `all_shapes.json` | 全部导出的标注几何 |
| `all_cells.csv` | 全部导出的细胞测量值 |

### 99_debug_utils – 调试工具

用于调试与清理的脚本。  
⚠️ 可能删除、移动或重置对象，请谨慎使用。

---

## 🚦 推荐运行顺序

1. 绘制或加载目标 PNI 标注。
2. （可选）绘制 `Normalization Patch`，`dark_ref` 和 `bright_ref`。
3. 在选中标注上运行细胞分割，进行参数调试。
4. 对所有 PNI 标注批量运行细胞分割。
5. 运行基于标记物的细胞分类。
6. 整理或分割神经区域。
7. 计算距离到标注的测量值。
8. 导出 CSV 与 GeoJSON。
9. 在 Python、R 或其他工具中进行下游分析。

---

## 📚 Additional Documentation

| Document | Description |
|---|---|
| `docs/annotation_convention.md` | Annotation naming and hierarchy rules |
| `docs/channel_convention.md` | Recommended image channel naming |
| `docs/output_schema.md` | Export CSV / GeoJSON schema |
| `docs/troubleshooting.md` | Common workflow issues |

---

## ⚠️ 重要提示

- 🔍 始终对分割与分类结果进行可视化检查。
- 📊 阈值依赖于具体数据集，需按需调整。
- 📛 通道名称可能需要根据您的图像数据修改。
- 🧪 本工具包适用于 QuPath v0.5.1，其他版本未测试。
- 🧬 这是一个研究工作流，并非临床诊断流水线。

---

## 📜 License

MIT License.

---

## 📖 引用

如果您的研究受益于本工具包，请引用此仓库。

---

<a id="english"></a>

# 🧫 QuPath MxIF Analysis Toolkit（English）

<div align="center">

**A semi-automated QuPath workflow for multiplex immunofluorescence whole-slide image analysis**

**🔬 Designed for tumor-nerve-immune microenvironment research around perineural invasion**

</div>

---

## 📌 What this toolkit does

- 🧪 DAPI-based cell / nucleus segmentation
- 🏷️ Marker-based immune and tumor cell classification
- 📏 Reference-region-based normalization and threshold estimation
- 🧠 Nerve region segmentation or annotation organization
- 📐 Spatial distance-to-annotation measurement
- 📤 CSV & GeoJSON export
- 🛠️ QuPath hierarchy repair utilities

---

## 🎯 Intended use

This project is built for computational pathology workflows involving multiplex immunofluorescence whole-slide images, with a focus on the tumor-nerve-immune microenvironment around PNI regions.

Manual annotation and visual quality control are expected throughout the workflow.

---

## 🧠 Why this workflow exists

MxIF whole-slide image analysis often suffers from:

- batch effects and autofluorescence
- inconsistent annotation hierarchy
- complex nerve-immune spatial organization
- lack of structured export for downstream spatial analysis

This toolkit aims to bridge:

> manual pathological annotation and reproducible computational feature extraction.

---

## 🚀 Quick Start

1. Draw PNI annotations.
2. Run segmentation on selected annotations.
3. Run marker-based classification.
4. Export measurements.
5. Perform downstream spatial analysis in Python / R.

---

## 📚 Additional Documentation

| Document | Description |
|---|---|
| `docs/annotation_convention.md` | Annotation naming and hierarchy rules |
| `docs/channel_convention.md` | Recommended image channel naming |
| `docs/output_schema.md` | Export CSV / GeoJSON schema |
| `docs/troubleshooting.md` | Common workflow issues |

---

## 📜 License

MIT License.

---

## 📖 Citation

If this toolkit contributes to your research, please cite this repository.
=======
