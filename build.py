#!/usr/bin/env python3
"""Build and remap a production Meteor addon for Minecraft 1.21.1 (Java 21)."""
import argparse, concurrent.futures, hashlib, importlib.util, json, os, pathlib, shutil, subprocess, urllib.request, zipfile
ROOT=pathlib.Path(__file__).resolve().parent
TOOLS=ROOT/'tools';TOOLS.mkdir(exist_ok=True)
p=argparse.ArgumentParser()
p.add_argument('--meteor',required=True,type=pathlib.Path,help='User-supplied meteor-client-0.5.8.jar')
p.add_argument('--test',action='store_true')
a=p.parse_args();a.meteor=a.meteor.resolve()
if hashlib.sha256(a.meteor.read_bytes()).hexdigest()!='090ad8f02273a67eca753ef37ccee2256e4dd3d715501ed75433ec8f57d7c5e9':
 p.error('Use the originally supplied Meteor Client 0.5.8 JAR: SHA-256 mismatch.')
def download(url,path,digest=None,algorithm='sha256'):
 if not path.exists():
  path.parent.mkdir(parents=True,exist_ok=True)
  with urllib.request.urlopen(url) as response:path.write_bytes(response.read())
 if digest and hashlib.new(algorithm,path.read_bytes()).hexdigest()!=digest:raise ValueError('Checksum mismatch: '+str(path))
 return path
for name,url,sha in [
 ('sponge-mixin.jar','https://maven.fabricmc.net/net/fabricmc/sponge-mixin/0.15.4+mixin.0.8.7/sponge-mixin-0.15.4+mixin.0.8.7.jar','4631f67da980ae2ac9e6e285a7df4680645e2f37a6a14acc775f97982162c2a2'),
 ('ecj.jar','https://repo.maven.apache.org/maven2/org/eclipse/jdt/ecj/3.39.0/ecj-3.39.0.jar','01f5a92ac19bb2b3bf85e295a68f2c73c264369109158b566ce9b490af982948'),
 ('tiny-remapper.jar','https://maven.fabricmc.net/net/fabricmc/tiny-remapper/0.10.4/tiny-remapper-0.10.4-fat.jar','aff82c3e052d031be8cba33f97a5e4d1b0098f6a9e942f4457706f751b4032b2'),
 ('yarn.jar','https://maven.fabricmc.net/net/fabricmc/yarn/1.21.1+build.3/yarn-1.21.1+build.3-v2.jar','4ec570889585cb958b7d3c9dd94ce4064b16ab797dffec152bc8486c59a4010b'),
 ('intermediary.jar','https://maven.fabricmc.net/net/fabricmc/intermediary/1.21.1/intermediary-1.21.1-v2.jar','e8bd6a4c39e235ee2dc73063558610775c72a2572fa0c6b32915b149074012a6')]:
 download(url,TOOLS/name,sha)
manifest=json.load(urllib.request.urlopen('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'))
v=next(v for v in manifest['versions'] if v['id']=='1.21.1')
metadata=download(v['url'],TOOLS/'version.json',v['sha1'],'sha1')
version=json.loads(metadata.read_text());client=version['downloads']['client']
download(client['url'],TOOLS/'minecraft-official.jar',client['sha1'],'sha1')
libraries=[TOOLS/'sponge-mixin.jar']
with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
 jobs=[]
 for lib in version['libraries']:
  artifact=lib.get('downloads',{}).get('artifact')
  if not artifact or '-natives-' in artifact['path']:continue
  path=TOOLS/'libraries'/pathlib.Path(artifact['path']).name;libraries.append(path)
  jobs.append(pool.submit(download,artifact['url'],path,artifact['sha1'],'sha1'))
 for job in jobs:job.result()
for name in ['intermediary','yarn']:
 with zipfile.ZipFile(TOOLS/(name+'.jar')) as z:(TOOLS/(name+'.tiny')).write_bytes(z.read('mappings/mappings.tiny'))
def remap(source,target,mapping,source_ns,target_ns,*classpath):
 subprocess.run(['java','-jar',str(TOOLS/'tiny-remapper.jar'),str(source),str(target),str(mapping),source_ns,target_ns]+list(map(str,classpath)),check=True)
if not (TOOLS/'minecraft-intermediary.jar').exists():
 remap(TOOLS/'minecraft-official.jar',TOOLS/'minecraft-intermediary.jar',TOOLS/'intermediary.tiny','official','intermediary')
if not (TOOLS/'minecraft-named.jar').exists():
 remap(TOOLS/'minecraft-intermediary.jar',TOOLS/'minecraft-named.jar',TOOLS/'yarn.tiny','intermediary','named')
if not (TOOLS/'meteor-named.jar').exists():
 remap(a.meteor,TOOLS/'meteor-named.jar',TOOLS/'yarn.tiny','intermediary','named',TOOLS/'minecraft-intermediary.jar')
compiler=['java','-jar',str(TOOLS/'ecj.jar'),'-21','-proc:none']
classes=ROOT/'build/classes'
if classes.exists():shutil.rmtree(classes)
classes.mkdir(parents=True)
cp=os.pathsep.join(map(str,[TOOLS/'minecraft-named.jar',TOOLS/'meteor-named.jar']+libraries))
subprocess.run(compiler+['-cp',cp,'-d',str(classes)]+[str(f) for f in sorted((ROOT/'src/main/java').rglob('*.java'))],check=True)
if a.test:
 tests=ROOT/'build/tests'
 if tests.exists():shutil.rmtree(tests)
 subprocess.run(compiler+['-d',str(tests)]+[str(ROOT/'src/main/java/dev/hachimi/standalone'/n) for n in ['Rules.java','Transactions.java','ChestSession.java','FlightMath.java','MiningCycle.java']]+[str(ROOT/'tests/LogicTest.java')],check=True)
 subprocess.run(['java','-cp',str(tests),'dev.hachimi.standalone.LogicTest'],check=True)
named=ROOT/'build/named.jar'
with zipfile.ZipFile(named,'w',zipfile.ZIP_DEFLATED) as z:
 z.writestr('META-INF/MANIFEST.MF','Manifest-Version: 1.0\n\n')
 for base in [classes,ROOT/'src/main/resources']:
  for f in sorted(base.rglob('*')):
   if f.is_file():z.write(f,f.relative_to(base).as_posix())
 z.write(ROOT/'LICENSE','LICENSE')
output=ROOT/'dist/meteor-1.21.1-死氪党-1.0.0.jar';output.parent.mkdir(exist_ok=True)
if output.exists():output.unlink()
remap(named,output,TOOLS/'yarn.tiny','named','intermediary',TOOLS/'minecraft-named.jar',TOOLS/'meteor-named.jar')
with zipfile.ZipFile(output) as z:
 assert z.testzip() is None
 mod=json.loads(z.read('fabric.mod.json'))
 assert 'hachimi' not in mod['depends'] and 'fabric-api' not in mod['depends']
 for n in z.namelist():
  assert not n.startswith(('pw/hachimi/','net/minecraft/','meteordevelopment/'))
  if n.endswith('.class'):assert b'pw/hachimi/' not in z.read(n) and b'me/matl114/' not in z.read(n)
if a.test:
 spec=importlib.util.spec_from_file_location('linkage',ROOT/'tests/check_linkage.py');module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
 module.check(output,[TOOLS/'minecraft-intermediary.jar',a.meteor])
print(output)
print('SHA-256:',hashlib.sha256(output.read_bytes()).hexdigest())
