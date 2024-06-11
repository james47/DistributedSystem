import yaml
import requests
import json
from flask import Flask, request, jsonify, Response
import sys
import os

app = Flask(__name__)
app_host = '0.0.0.0'
app_port = 4747

'''
  Store the picture data into a logical volume specified by lvid

  @Params
   - img:   bytes data of the picture
   - lvid:  id of logical volume where the picture is stored
'''
def store_pic(img, lvid):
    f = fd_pool[lvid]
    f.seek(lv_size[lvid])
    f.write(img)

'''
  Given logical volume id, picture offset and size
  retrieve the picture data

  @Params
   - lvid:    id of logical volume where the picture is stored
   - offset:  offset of the file where the picture is stored
   - size:    size of the picture
'''
def retrieve_pic(lvid, offset, size):
    f = fd_pool[lvid]
    f.seek(offset)
    data = f.read(size)
    return data


@app.route('/get', methods = ['GET'])
def get_pic():
    lvid = request.args.get('lvid')
    pid = request.args.get('pid')

    if lvid in lv_pids and pid in lv_pids[lvid]: # valid lvid and pid
        pic_info = lv_pids[lvid][pid]
        if not pic_info['is_deleted']:
            pic_offset = pic_info['offset']
            pic_size = pic_info['size']
            pic_mime = pic_info['mime']
            pic = retrieve_pic(lvid, pic_offset, pic_size)
            resp = Response(pic, mimetype = "image/{}".format(pic_mime))
            return resp

    return Response("{'status':'Picture not found'}", status=404, mimetype='application/json')

# http://url/upload?lvid=1&pid=2
@app.route('/upload', methods = ['POST'])
def upload_pic():
    lvid = request.args.get('lvid')
    pid = request.args.get('pid')
    pic_mime = request.args.get('mime')

    pic_data = request.get_data()
    pic_size = len(pic_data)

    if lvid in lv_pids:
        pic_offset = lv_size[lvid]
        # store the picture into disk and update in-memory map
        store_pic(pic_data, lvid)
        lv_size[lvid] += pic_size
        lv_pids[lvid][pid] = {}
        lv_pids[lvid][pid]['offset'] = pic_offset
        lv_pids[lvid][pid]['size'] = pic_size
        lv_pids[lvid][pid]['is_deleted'] = False
        lv_pids[lvid][pid]['mime'] = pic_mime
        return Response("{'status':'success'}", status=200, mimetype='application/json')

    return Response("{'status':'failed'}", status=400, mimetype='application/json')


@app.route('/delete', methods = ['GET'])
def delete_pic():
    lvid = request.args.get('lvid')
    pid = request.args.get('pid')

    if lvid in lv_pids and pid in lv_pids[lvid]: # valid lvid and pid
        lv_pids[lvid][pid]['is_deleted'] = True
        return Response("{'status':'success'}", status=200, mimetype='application/json')

    return Response("{'status':'failed'}", status=400, mimetype='application/json')

if __name__ == '__main__':

    config_path = 'config.yaml'
    conf = yaml.load(open(config_path))

    lvspath = conf['lvspath']
    os.system('rm -r ' + lvspath)
    os.system('mkdir -p ' + lvspath)

    store_id = '0' if len(sys.argv) < 2 else sys.argv[1]
    for store in conf['store']:
        if store_id == store['name']:
            app_port = store['port']

    # in-memory index
    lv_pids = {}    # pid : {'offset': 'int', 'size': 'int', 'is_deleted': 'boolean', 'mime': 'str'}
    lv_size = {}    # size of volume
    fd_pool = {}

    for lv in conf['lvs']:
        lvid = str(lv['id'])
        file_path = lvspath + lvid
        fd_pool[lvid] = open(file_path, 'ab+')
        if store_id in lv['stores']:
            lv_size[lv['id']] = 0
            lv_pids[lv['id']] = {}

    app.run(host=app_host, port=app_port)




