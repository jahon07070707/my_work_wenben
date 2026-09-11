"""数据采集主程序"""
import json
import yaml
import argparse
from pathlib import Path
from datetime import datetime

import sys
sys.path.insert(0, str(Path(__file__).parent))

from collectors.rss_collector import RSSCollector
from collectors.local_collector import LocalCSVCollector
from collectors.web_collector import WebNewsCollector


def load_config(config_path: str = 'config.yaml') -> dict:
    with open(config_path, 'r', encoding='utf-8') as f:
        return yaml.safe_load(f)


def run_collection(config_path: str = 'config.yaml', output_dir: str = None):
    config = load_config(config_path)
    python_home = Path(__file__).parent.parent
    out_dir = Path(output_dir) if output_dir else python_home / config['output']['dir']
    out_dir.mkdir(parents=True, exist_ok=True)

    all_data = []
    stats = {'rss': 0, 'local': 0, 'web': 0}

    # RSS 采集
    for src in config.get('sources', {}).get('rss', []):
        collector = RSSCollector(src['name'], src['url'], src.get('category'))
        items = collector.collect(max_items=30)
        all_data.extend(items)
        stats['rss'] += len(items)
        print(f"  RSS [{src['name']}]: {len(items)} 条")

    # 本地 CSV
    base = Path(__file__).parent.parent
    for src in config.get('sources', {}).get('local', []):
        path = base / src['path']
        collector = LocalCSVCollector(src['name'], str(path), src.get('type', 'comment'))
        items = collector.collect()
        all_data.extend(items)
        stats['local'] += len(items)
        print(f"  本地 [{src['name']}]: {len(items)} 条")

    # 网页采集（可选）
    web_sources = config.get('sources', {}).get('web', [])
    for src in web_sources:
        collector = WebNewsCollector(src['name'], src['url'], src.get('category', '社会'))
        items = collector.collect(max_items=15)
        all_data.extend(items)
        stats['web'] += len(items)
        print(f"  网页 [{src['name']}]: {len(items)} 条")

    # 去重
    seen_hashes = set()
    unique_data = []
    for item in all_data:
        h = item.get('content_hash')
        if h not in seen_hashes:
            seen_hashes.add(h)
            unique_data.append(item)

    timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
    out_file = out_dir / f'collected_{timestamp}.json'
    with open(out_file, 'w', encoding='utf-8') as f:
        json.dump(unique_data, f, ensure_ascii=False, indent=2)

    print(f"\n采集完成: 共 {len(unique_data)} 条 (去重后)")
    print(f"  RSS: {stats['rss']}, 本地: {stats['local']}, 网页: {stats['web']}")
    print(f"输出文件: {out_file}")
    return str(out_file), unique_data


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='文本数据采集')
    parser.add_argument('-c', '--config', default='config.yaml')
    parser.add_argument('-o', '--output', default=None)
    args = parser.parse_args()
    run_collection(args.config, args.output)
