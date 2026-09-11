# 智能文本分类与情感分析系统

基于 **PyTorch + NLTK + Scikit-learn + Spring Boot + MyBatis + MySQL + Vue.js + ECharts** 的全栈文本分析平台。

## 目录结构

```
wenben/
├── python/                 # Python 服务（采集 + 机器学习）
│   ├── start.bat           # 一键训练模型并启动 ML API
│   ├── requirements.txt
│   ├── crawler/            # 数据采集
│   ├── ml/                 # 模型训练与推理 API
│   └── data/               # 样本数据与采集结果
│       ├── samples/
│       └── raw/
├── java/                   # Java 后端（Spring Boot）
│   ├── start.bat           # 启动后端 API
│   ├── sql/init.sql        # 数据库初始化
│   ├── pom.xml
│   └── src/
├── vue/                    # Vue 前端
│   ├── start.bat           # 启动前端
│   ├── package.json
│   └── src/
└── start.bat               # 启动说明
```

## 环境要求

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | **17** | Spring Boot 3.x 最低要求 |
| Node.js | **18.18+ / 22.x** | 推荐 22.22.3，见 `vue/.node-version` |
| Python | **3.11** | 见 `python/.python-version` |
| MySQL | 8.0+ | 数据库 |

## 快速启动

### 1. 初始化数据库

```bash
mysql -u root -p < java/sql/init.sql
```

修改 `java/src/main/resources/application.yml` 中的 MySQL 密码。

### 2. 启动 Python 服务（端口 8000，Python 3.11）

`start.bat` 会自动创建虚拟环境 `.venv`，避免与系统 Python 包冲突。

```bash
cd python
start.bat
```

或手动执行：

```bash
cd python
pip install -r requirements.txt
cd ml && python train.py --data-dir ../data --model-dir ./saved_models
python main.py
```

### 3. 启动 Java 后端（端口 8080）

```bash
cd java
start.bat
```

### 4. 启动 Vue 前端（端口 5173）

```bash
cd vue
start.bat
```

## 功能模块

| 模块 | 目录 | 技术 |
|------|------|------|
| 用户登录 | `java` + `vue/Login.vue` | JWT + BCrypt |
| 数据采集 | `python/crawler/` | requests, feedparser, BeautifulSoup |
| 文本预处理 | `python/ml/preprocess/` | jieba, Scikit-learn TF-IDF |
| 文本分类/情感分析 | `python/ml/` | PyTorch MLP |
| 模型评估对比 | `vue/ModelEvaluation.vue` | PyTorch vs LR 基线、混淆矩阵 |
| 批量分析 | `vue/BatchAnalysis.vue` | CSV/TXT 上传、批量推理 |
| 结果导出 | Java API | CSV 导出 |
| 后端 API | `java/` | Spring Boot, MyBatis, MySQL |
| 可视化前端 | `vue/` | Vue 3, Element Plus, ECharts 词云 |

## 企业级前沿技术

| 技术 | 说明 | 访问/使用 |
|------|------|-----------|
| **Swagger OpenAPI** | RESTful API 自动文档 | http://localhost:8080/swagger-ui.html |
| **Spring Actuator** | 服务健康监控 | http://localhost:8080/actuator/health |
| **Caffeine 缓存** | 分析结果本地缓存，减少 ML 调用 | 自动生效 |
| **WebSocket + STOMP** | 采集任务实时进度推送 | 数据采集页 |
| **Spring @Async** | 异步采集，不阻塞前端 | 自动生效 |
| **舆情预警引擎** | 负面情感阈值自动告警 | 舆情预警页 |
| **LLM 大模型对接** | DeepSeek/Qwen/OpenAI 兼容 API | AI智能解读页 |
| **Docker Compose** | MySQL 一键部署 | `docker-compose up -d` |

### 启用大模型

```yaml
# java/src/main/resources/application.yml
llm:
  enabled: true
  api-key: sk-your-key
  api-url: https://api.deepseek.com/v1/chat/completions
  model: deepseek-chat
```


### 数据库（已有库）

```bash
mysql -u root -p text_analysis < java/sql/update_enterprise.sql
```

| 功能 | 说明 |
|------|------|
| 用户登录 | 默认 admin / admin123，JWT 鉴权 |
| 文本预处理预览 | 分词、去停用词、关键词提取可视化 |
| 批量分析 | 多行文本 / CSV / TXT 文件批量分析 |
| 模型评估 | 准确率、F1、混淆矩阵、与 LR 基线对比 |
| 词云可视化 | 热点关键词词云图 |
| 结果导出 | 分析结果一键导出 CSV |

## 默认账号

- 用户名：`admin`
- 密码：`admin123`

若数据库已初始化，执行 `java/sql/update_admin.sql` 更新密码。

## 数据采集来源

| 数据源 | 类型 | 地址 |
|--------|------|------|
| 新浪科技 | RSS | rss.sina.com.cn/tech/rollnews.xml |
| 新浪财经 | RSS | 股票财经频道 |
| 新浪体育 | RSS | 体育赛事新闻 |
| 新浪娱乐 | RSS | 娱乐资讯 |
| 评论样本 | CSV | python/data/samples/comments.csv |
| 新闻样本 | CSV | python/data/samples/news_samples.csv |

## 使用流程

1. 按顺序启动 `python` → `java` → `vue` 三个服务
2. 打开 http://localhost:5173
3. 进入「数据采集」→ 点击「一键采集全部」
4. 在「数据概览」查看分类分布、情感分布、热点话题
5. 在「文本分析」手动输入文本进行实时分析

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/analysis/text | 实时文本分析 |
| POST | /api/analysis/text/save | 分析并保存 |
| GET | /api/analysis/results | 分页查询结果 |
| GET | /api/dashboard/stats | 仪表盘统计 |
| GET | /api/collection/sources | 数据源列表 |
| POST | /api/collection/start-all | 一键采集 |
