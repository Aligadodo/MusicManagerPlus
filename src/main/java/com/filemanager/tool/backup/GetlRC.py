# -*- coding:utf-8 -*-

import os
import re
import requests
import os
import json
from concurrent.futures import ThreadPoolExecutor, as_completed
UC_PATH = 'C:/Users/28667/Downloads/musicCache/Cache/'  # 缓存路径 例 D:/CloudMusic/Cache/
MP3_PATH = 'C:/Users/28667/Downloads/lrc/'  # 存放歌曲路径

class Transform():
    def do_transform(self):
        files = os.listdir(UC_PATH)
        all_task = []
        with ThreadPoolExecutor(max_workers=3) as t:
            for file in files:
                task = t.submit(process, file)
                all_task.append(task)
        while True:
            try:
                # timeout=30 超时时间30s 表示这个线程30s 没有执行结束，抛出异常。
                for task in as_completed(all_task, timeout=30):
                    task.result()
                break
            except Exception as e:
                print(f"{e}")


def process(file):
    if not file[-3:] == '.uc':  # 后缀uc结尾为歌曲缓存
        return 1
    mp3_content = None
    try:
        with open(UC_PATH +file+"", mode='rb') as uc_file:
            mp3_content = bytearray(uc_file.read())
            for i, byte in enumerate(mp3_content):
                byte ^= 0xa3
                mp3_content[i] = byte
    except Exception as e:
        print("read "+file+" fail" + f"{e}")
    if not mp3_content:
        return 1
    song_name = get_song_info_by_file(file)


def get_song_info_by_file(file_name):
    # -前面的数字是歌曲ID，例：1347203552-320-0aa1, 1347203552是歌曲ID
    match_inst = re.match('\d*', file_name)
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36",
        "Connection": "keep-alive",
        "Accept": "text/html,application/json,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language": "zh-CN,zh;q=0.8"
    }
    if match_inst:
        song_id = match_inst.group()
    else:
        song_id = file_name

    try:
        url = 'http://music.163.com/api/song/detail/?id={0}&ids=%5B{1}%5D'.format(song_id, song_id)
        response = requests.get(url, headers=headers)
        jsons = response.json()
        song_name = jsons['songs'][0]['name']
        singer = jsons['songs'][0]['artists'][0]['name']
        get_lyric(song_id, singer + "-" + song_name)
        return singer + "-" + song_name
    except:
        return song_id

def get_lyric(song_id, name):
    try:
        headers = {
            "user-agent": "Mozilla/5.0",
            "Referer": "http://music.163.com",
            "Host": "music.163.com"
        }
        if not isinstance(song_id, str):
            song_id = str(song_id)  # 格式化参数
        # 歌词API路劲
        url = f"http://music.163.com/api/song/lyric?id={song_id}+&lv=1&tv=-1"
        response = requests.get(url, headers=headers)
        jsons = response.json()
        lrc = jsons["lrc"]["lyric"]  # 解析json
        #print(lrc)
        path = MP3_PATH + '%s.lrc' % (name)  # 路劲，可自行更改
        with open(path, "w+")as f:
            f.write(lrc)  # 写入文件
        return 1  # 返回值，暂无用处
    except:
        return 2  # 返回值，暂无用处


def check_path():
    global UC_PATH, MP3_PATH

    if not os.path.exists(UC_PATH):
        print('缓存路径错误: {}'.format(UC_PATH))
        return False
    if not os.path.exists(MP3_PATH):
        print('目标路径错误: %s' % MP3_PATH)
        return False

    if UC_PATH[-1] != '/':  # 容错处理 防止绝对路径结尾不是/
        UC_PATH += '/'
    if MP3_PATH[-1] != '/':
        MP3_PATH += '/'
    return True


if __name__ == '__main__':
    if not check_path():
        exit()

    transform = Transform()
    transform.do_transform()