#!/usr/bin/env python3
"""Build either Minecraft target or release both together from the same source tree."""
import argparse,concurrent.futures,hashlib,importlib.util,json,os,pathlib,shutil,subprocess,urllib.request,zipfile
ROOT=pathlib.Path(__file__).resolve().parent
RELEASE=json.loads((ROOT/'release.json').read_text())
SHARED=ROOT/'tools';SHARED.mkdir(exist_ok=True)
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--minecraft',choices=list(RELEASE['targets']),default='1.21.4')
parser.add_argument('--meteor',type=pathlib.Path,help='Optional matching Meteor JAR override; official pinned artifact is downloaded by default')
parser.add_argument('--test',action='store_true')
args=parser.parse_args()
def download(url,path,digest=None,algorithm='sha256'):
 if not path.exists():
  path.parent.mkdir(parents=True,exist_ok=True)
  temporary=path.with_name(path.name+'.download')
  with urllib.request.urlopen(urllib.request.Request(url,headers={"User-Agent":"Mozilla/5.0"})) as response:temporary.write_bytes(response.read())
  temporary.replace(path)
 if digest and hashlib.new(algorithm,path.read_bytes()).hexdigest()!=digest:raise ValueError('Checksum mismatch: '+str(path))
 return path
for name,url,sha in [
 ('sponge-mixin.jar','https://maven.fabricmc.net/net/fabricmc/sponge-mixin/0.15.4+mixin.0.8.7/sponge-mixin-0.15.4+mixin.0.8.7.jar','4631f67da980ae2ac9e6e285a7df4680645e2f37a6a14acc775f97982162c2a2'),
 ('ecj.jar','https://repo.maven.apache.org/maven2/org/eclipse/jdt/ecj/3.39.0/ecj-3.39.0.jar','01f5a92ac19bb2b3bf85e295a68f2c73c264369109158b566ce9b490af982948'),
 ('tiny-remapper.jar','https://maven.fabricmc.net/net/fabricmc/tiny-remapper/0.10.4/tiny-remapper-0.10.4-fat.jar','aff82c3e052d031be8cba33f97a5e4d1b0098f6a9e942f4457706f751b4032b2')]:
 download(url,SHARED/name,sha)
manifest=json.load(urllib.request.urlopen('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'))
compiler=['java','-jar',str(SHARED/'ecj.jar'),'-21','-proc:none']
def remap(source,target,mapping,from_ns,to_ns,*classpath):
 subprocess.run(['java','-jar',str(SHARED/'tiny-remapper.jar'),str(source),str(target),str(mapping),from_ns,to_ns]+list(map(str,classpath)),check=True)
def build(mc,meteor_override=None):
 cfg=RELEASE['targets'][mc]
 tools=SHARED if mc=='1.21.1' else SHARED/mc
 tools.mkdir(exist_ok=True)
 print('Building Minecraft',mc,'with Meteor',cfg['meteor_version'],flush=True)
 meteor=meteor_override.resolve() if meteor_override else download(cfg['meteor_url'],tools/f'meteor-client-{mc}.jar',cfg['meteor_sha256'])
 if hashlib.sha256(meteor.read_bytes()).hexdigest()!=cfg['meteor_sha256']:raise ValueError('Incorrect Meteor JAR for '+mc)
 yarn=cfg['yarn']
 download(f'https://maven.fabricmc.net/net/fabricmc/yarn/{yarn}/yarn-{yarn}-v2.jar',tools/'yarn.jar',cfg['yarn_sha256'])
 download(f'https://maven.fabricmc.net/net/fabricmc/intermediary/{mc}/intermediary-{mc}-v2.jar',tools/'intermediary.jar',cfg['intermediary_sha256'])
 v=next(v for v in manifest['versions'] if v['id']==mc)
 metadata=download(v['url'],tools/'version.json',v['sha1'],'sha1')
 version=json.loads(metadata.read_text());client=version['downloads']['client']
 download(client['url'],tools/'minecraft-official.jar',client['sha1'],'sha1')
 libraries=[SHARED/'sponge-mixin.jar']
 with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
  jobs=[]
  for lib in version['libraries']:
   artifact=lib.get('downloads',{}).get('artifact')
   if not artifact or '-natives-' in artifact['path']:continue
   path=tools/'libraries'/pathlib.Path(artifact['path']).name;libraries.append(path)
   jobs.append(pool.submit(download,artifact['url'],path,artifact['sha1'],'sha1'))
  for job in jobs:job.result()
 for name in ['intermediary','yarn']:
  with zipfile.ZipFile(tools/(name+'.jar')) as z:(tools/(name+'.tiny')).write_bytes(z.read('mappings/mappings.tiny'))
 if not (tools/'minecraft-intermediary.jar').exists():remap(tools/'minecraft-official.jar',tools/'minecraft-intermediary.jar',tools/'intermediary.tiny','official','intermediary')
 if not (tools/'minecraft-named.jar').exists():remap(tools/'minecraft-intermediary.jar',tools/'minecraft-named.jar',tools/'yarn.tiny','intermediary','named')
 if not (tools/'meteor-named.jar').exists():remap(meteor,tools/'meteor-named.jar',tools/'yarn.tiny','intermediary','named',tools/'minecraft-intermediary.jar')
 work=ROOT/'build'/mc;classes=work/'classes'
 if classes.exists():shutil.rmtree(classes)
 classes.mkdir(parents=True)
 cp=os.pathsep.join(map(str,[tools/'minecraft-named.jar',tools/'meteor-named.jar']+libraries))
 (work/'classpath.txt').write_text(cp)
 sources=sorted((ROOT/'src/main/java').rglob('*.java'))+sorted((ROOT/'src/platform'/mc/'java').rglob('*.java'))
 subprocess.run(compiler+['-cp',cp,'-d',str(classes)]+list(map(str,sources)),check=True)
 if args.test:
  tests=work/'tests'
  if tests.exists():shutil.rmtree(tests)
  pure=['Rules.java','Transactions.java','ChestSession.java','FlightMath.java','MiningCycle.java']
  subprocess.run(compiler+['-d',str(tests)]+[str(ROOT/'src/main/java/dev/hachimi/standalone'/n) for n in pure]+[str(ROOT/'tests/LogicTest.java')],check=True)
  subprocess.run(['java','-cp',str(tests),'dev.hachimi.standalone.LogicTest'],check=True)
 named=work/'named.jar'
 mod=json.loads((ROOT/'src/main/resources/fabric.mod.json').read_text())
 mod['version']=RELEASE['version'];mod['depends']['minecraft']=mc;mod['depends']['fabricloader']=cfg['loader'];mod['depends']['meteor-client']=cfg['meteor_version']
 mod['description']=f'Ten independent Hachimi and Slime modules for Minecraft {mc} / Meteor {cfg["meteor_version"]}.'
 with zipfile.ZipFile(named,'w',zipfile.ZIP_DEFLATED) as z:
  z.writestr('META-INF/MANIFEST.MF','Manifest-Version: 1.0\n\n')
  for base in [classes,ROOT/'src/main/resources']:
   for f in sorted(base.rglob('*')):
    if f.is_file() and f.name!='fabric.mod.json':z.write(f,f.relative_to(base).as_posix())
  z.writestr('fabric.mod.json',json.dumps(mod,ensure_ascii=False,indent=2))
  z.write(ROOT/'LICENSE','LICENSE')
 output=ROOT/'dist'/RELEASE['artifact'].format(minecraft=mc,version=RELEASE['version']);output.parent.mkdir(exist_ok=True)
 output.unlink(missing_ok=True)
 remap(named,output,tools/'yarn.tiny','named','intermediary',tools/'minecraft-named.jar',tools/'meteor-named.jar')
 with zipfile.ZipFile(output) as z:
  assert z.testzip() is None
  assert set(mod['depends'])=={'java','minecraft','fabricloader','meteor-client'}
  for n in z.namelist():
   assert not n.startswith(('pw/hachimi/','me/matl114/','net/minecraft/','meteordevelopment/','dev/hachimi/test/'))
   if n.endswith('.class'):assert b'pw/hachimi/' not in z.read(n) and b'me/matl114/' not in z.read(n)
 if args.test:
  spec=importlib.util.spec_from_file_location('linkage',ROOT/'tests/check_linkage.py');module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
  module.check(output,[tools/'minecraft-intermediary.jar',meteor])
 print(output,flush=True)
 return {'minecraft':mc,'meteor':cfg['meteor_version'],'file':output.name,'sha256':hashlib.sha256(output.read_bytes()).hexdigest()}
print(json.dumps(build(args.minecraft,args.meteor),ensure_ascii=False,indent=2))
