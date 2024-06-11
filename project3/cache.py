import redis
import yaml
import requests
import json
from flask import Flask, request, jsonify, Response
from PIL import Image
import io

app = Flask(__name__)
app_host = '0.0.0.0'
app_port = 4747

# url foramt : http://Cache_URL/GET/<machine_id>/<lvid, pid>
@app.route('/get', methods = ['GET'])
def get_pic():
    machine_id = request.args.get('machine')
    lvid = request.args.get('lvid')
    pid = request.args.get('pid')

    r = redis.Redis(connection_pool=pool)
    # try to get the picture from in-memory cache
    pic = r.get(pid)
    # if not got, try to get picture from store server
    if pic is None:
        pic = get_pic_from_store(machine_id, lvid, pid)
    if pic is None:
        return Response("{'status':'Picture not found'}", status=404, mimetype='application/json')
    r.set(pid, pic)
    mime = Image.open(io.BytesIO(pic)).format
    resp = Response(pic, mimetype = "image/{}".format(mime))
    return resp

# url format : http://Cache_URL/DELETE/<pid>
@app.route('/DELETE', methods = ['GET'])
def delete_pic():
    pid = request.args.get('pid')
    r = redis.Redis(connection_pool=pool)
    r.delete(pid)
    return Response("{'status':'success'}", status=200, mimetype='application/json')

'''
  Cache Server try to get the picture from Store Server
  Return None if the picture is deleted
'''
def get_pic_from_store(machine_id, lvid, pid):
    machine_host = None
    machine_port = None
    for store in conf['store']:
        if store['name'] == machine_id:
            machine_host = store['host']
            machine_port = store['port']

    params = {
        'lvid' : lvid,
        'pid' : pid
    }

    request_url = 'http://{}:{}/get'.format(machine_host, machine_port)
    response = requests.get(request_url, params = params)

    if response.status_code != requests.codes.ok:
        return None
    return response.content

if __name__ == '__main__':

    config_path = 'config.yaml'
    conf = yaml.load(open(config_path))

    # global redis connection pool
    pool = redis.ConnectionPool(host=conf['redis']['host'], port=conf['redis']['port'], db=0)

    app_port = conf['cache']['port']
    app.run(host=app_host, port=app_port)
