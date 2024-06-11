from cassandra.cluster import Cluster
from cassandra.policies import DCAwareRoundRobinPolicy

cluster = Cluster(['127.0.0.1'], port=9042)

session = cluster.connect()
rows = session.execute('SELECT cluster_name, listen_address FROM system.local')
for row in rows:
    print(row.cluster_name)

import redis
r = redis.StrictRedis(host='localhost', port=6379, db=0)
r.set('foo', 'bar')
r.get('foo')
