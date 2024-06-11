from flask import request, Flask
import yaml
from cassandra.cluster import Cluster
import random
from PIL import Image
import io
import requests

app = Flask(__name__)
app.debug = False
app_host = '0.0.0.0'
app_port = 4747

def pid_query(pid):
    if pid is None or len(pid) < 1:
        return None
    rows = session.execute("select * from pids where pid = '{}'".format(pid))
    for row in rows:
        return row.lvid
    return None

def pid_insert(pid, lvid):
    pids[pid] = lvid
    session.execute("insert into pids(pid, lvid) values('{}', '{}')".format(pid, lvid))
    return True

def pid_gen():
    pid = random.randint(0, 1e9)
    while pid in pids:
        pid = random.randint(0, 1e9)
    return str(pid)

def lvid_rand(lvs):
    index = random.randint(1, len(lvs)) - 1
    return lvs[index]['id']

def get_store_has_lvid(lvid):
    for lv in conf['lvs']:
        if lv['id'] == lvid:
            return lv['stores']
    return None

@app.route('/upload', methods=['POST'])
def upload():
    """
        generate pid and make sure no duplicate
        randomly choose one lvid
        send http post upload(picture, pic, lvid) to all machines that have lvid
        update db: insert pid
        return success: pid / failed to client
    """
    f = request.files['picture_uploaded']
    pic = f.read()
    mime = Image.open(io.BytesIO(pic)).format
    pid = pid_gen()
    lvid = lvid_rand(conf['lvs'])
    stores = get_store_has_lvid(lvid)

    all_success = True
    for store in conf['store']:
        if store['name'] in stores:
            params = {'pid': pid, 'lvid': lvid, 'mime': mime}
            r = requests.post('http://{}:{}/upload'.format(store['host'], store['port']), params=params, data=pic)
            if r.status_code != requests.codes.ok:
                all_success = False

    if all_success:
        all_success = pid_insert(pid, lvid)

    return 'success! pid = {}'.format(pid) if all_success else 'failed'

@app.route('/get', methods=['GET'])
def get():
    """
        get_db(), query pid, get lvid
        randomly choose one machine
        construct URL http://cache/get?lvid=&pid=&machine=
        return URL to client
        browser should automatically visit cache by requesting URL
    """
    pid = request.args.get('pid')
    lvid = pid_query(pid)
    if lvid is None:
        return 'failed, no picture with pid {}!'.format(pid)

    machine = None
    for lv in conf['lvs']:
        if lv['id'] == lvid:
            machine = random.choice(lv['stores'])
    url = 'http://{}:{}/get?machine={}&pid={}&lvid={}'.format(
        conf['cache']['host'], conf['cache']['port'], machine, pid, lvid)
    return '''
    <!doctype html>
    <html>
    <head>
         <script language="JavaScript">
            function imgError() 
            {
                alert('Picture not found');
            }
        </script>
    </head>
    <body>
    <img src="%s" onerror="imgError()" />
    </body>
    ''' % url

@app.route('/delete', methods=['GET'])
def delete():
    """
        get_db(), query pid, get lvid
        send http delete(pid) get to cache
        send http delete(pid) get to machines that have lvid (i.e., conf[lvs][lvid][stores])
        return success / failed to client
    """
    pid = request.args.get('pid')
    lvid = pid_query(pid)
    if lvid is None:
        return 'failed! no picture with pid {}!'.format(pid)
    stores = get_store_has_lvid(lvid)

    all_success = True
    # delete from all machines with lv[lvid]
    for store in conf['store']:
        if store['name'] in stores:
            params = {'pid': pid, 'lvid': lvid}
            r = requests.get('http://{}:{}/delete'.format(store['host'], store['port']), params)
            if r.status_code != requests.codes.ok:
                all_success = False

    # delete from cache
    params = {'pid': pid}
    r = requests.get('http://{}:{}/DELETE'.format(conf['cache']['host'], conf['cache']['port']), params)
    if r.status_code != requests.codes.ok:
        all_success = False

    return 'success' if all_success else 'failed'

@app.route('/')
def index():
    return '''
    <!doctype html>
    <html>
    <body>
    <form action='/upload' method='post'>
        upload picture: <input type='file' name='picture_uploaded'>
        <input type='submit' formenctype='multipart/form-data' value='upload'>
    </form>
    <form action='/get' method='get'>
        get picture pid: <input type='text' name='pid'>
        <input type='submit' value='get'>
    </form>
    <form action='/delete' method='get'>
        delete picture pid: <input type='text' name='pid'>
        <input type='submit' value='delete'>
    </form>
    '''

if __name__ == '__main__':

    config_path = 'config.yaml'
    conf = yaml.load(open(config_path))

    pids = {}
    HAYSTACK_KEYSPACE = "haystack"

    # directory
    cluster = Cluster([conf['cassandra']['host']], port=conf['cassandra']['port'])

    # use one session for each process
    session = cluster.connect()

    # create keyspace
    session.execute("CREATE KEYSPACE %s "
                    "WITH replication = { 'class' : 'SimpleStrategy', 'replication_factor' : '3'}"
                    % HAYSTACK_KEYSPACE)

    # use keyspace
    session.set_keyspace(HAYSTACK_KEYSPACE)

    # create table
    session.execute("create table pids (pid text PRIMARY KEY, lvid text)")

    app_port = conf['webserver']['port']
    app.run(host=app_host, port=app_port)
