import requests
from PIL import Image
import io

f = open('/Users/james/Downloads/tuziki_maimeng.jpg', 'rb')
pic = f.read()
mime = Image.open(io.BytesIO(pic)).format
pid = 5
lvid = 1

params = {'pid': pid, 'lvid': lvid, 'mime': mime}
r = requests.post('http://{}:{}/upload'.format('ghc45.ghc.andrew.cmu.edu', 4747), params=params, data=pic)
print(r.status_code)
