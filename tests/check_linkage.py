"""Check every Minecraft/Meteor method and field referenced by the production JAR."""
import pathlib,struct,zipfile,sys
class ClassFile:
 def __init__(self,data):
  self.data=data;self.pos=8;self.cp=[None];count=self.u2()
  while len(self.cp)<count:
   tag=self.u1()
   if tag==1:
    n=self.u2();entry=(tag,self.read(n).decode('utf8',errors='replace'))
   elif tag in (3,4):entry=(tag,self.read(4))
   elif tag in (5,6):self.read(8);self.cp.extend([None,None]);continue
   elif tag in (7,8,16,19,20):entry=(tag,self.u2())
   elif tag in (9,10,11,12,17,18):entry=(tag,self.u2(),self.u2())
   elif tag==15:entry=(tag,self.u1(),self.u2())
   else:raise ValueError(tag)
   self.cp.append(entry)
  self.access=self.u2();self.name=self.cls(self.u2());parent=self.u2();self.parents=[self.cls(parent)] if parent else []
  self.parents += [self.cls(self.u2()) for _ in range(self.u2())]
  self.fields=self.members();self.methods=self.members()
 def read(self,n):r=self.data[self.pos:self.pos+n];self.pos+=n;return r
 def u1(self):return self.read(1)[0]
 def u2(self):return int.from_bytes(self.read(2),'big')
 def u4(self):return int.from_bytes(self.read(4),'big')
 def utf(self,i):return self.cp[i][1]
 def cls(self,i):return self.utf(self.cp[i][1])
 def members(self):
  values=set()
  for _ in range(self.u2()):
   self.u2();name=self.utf(self.u2());desc=self.utf(self.u2());values.add((name,desc))
   for _ in range(self.u2()):self.u2();self.read(self.u4())
  return values
 def refs(self):
  for x in self.cp:
   if x and x[0] in (9,10,11):
    owner=self.cls(x[1]);nt=self.cp[x[2]]
    yield x[0],owner,self.utf(nt[1]),self.utf(nt[2])
def check(addon,dependencies):
 jars=[zipfile.ZipFile(p) for p in [addon]+dependencies];cache={}
 def load(name):
  if name not in cache:
   cache[name]=None
   for z in jars:
    try:cache[name]=ClassFile(z.read(name+'.class'));break
    except KeyError:pass
  return cache[name]
 def resolve(owner,kind,name,desc,seen=None):
  seen=set() if seen is None else seen
  if owner in seen:return False
  seen.add(owner);c=load(owner)
  if not c:
   object_methods={('<init>','()V'),('getClass','()Ljava/lang/Class;'),('hashCode','()I'),
       ('equals','(Ljava/lang/Object;)Z'),('clone','()Ljava/lang/Object;'),('toString','()Ljava/lang/String;'),
       ('notify','()V'),('notifyAll','()V'),('wait','()V'),('wait','(J)V'),('wait','(JI)V'),('finalize','()V')}
   return kind!=9 and owner=='java/lang/Object' and (name,desc) in object_methods
  if (name,desc) in (c.fields if kind==9 else c.methods):return True
  return any(resolve(p,kind,name,desc,seen) for p in c.parents)
 errors=[];refs=0
 for n in jars[0].namelist():
  if not n.endswith('.class'):continue
  data=jars[0].read(n)
  if b'pw/hachimi/' in data or b'me/matl114/' in data:errors.append('Original client reference: '+n)
  c=ClassFile(data)
  for kind,owner,name,desc in c.refs():
   if not owner.startswith(('net/minecraft/','meteordevelopment/')):continue
   refs+=1
   if not load(owner):errors.append('Missing class '+owner)
   elif not resolve(owner,kind,name,desc):errors.append(f'{n}: unresolved {owner}.{name}{desc}')
 if errors:raise AssertionError('\n'.join(errors))
 print(f'PASS: {refs} production Minecraft/Meteor field and method references; no OpenHachimi/SlimefunHelper references.')
 for z in jars:z.close()
if __name__=='__main__':check(pathlib.Path(sys.argv[1]),[pathlib.Path(p) for p in sys.argv[2:]])
