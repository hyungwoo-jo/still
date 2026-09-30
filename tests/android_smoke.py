"""Run only on a disposable emulator: replaces Still's test preferences."""
import json, os, re, shutil, subprocess, time, xml.etree.ElementTree as ET
from pathlib import Path
adb=os.environ.get('ADB') or shutil.which('adb')
assert adb, 'Put Android platform-tools on PATH, or set ADB.'
serial=os.environ.get('ANDROID_SERIAL','emulator-5582')
assert serial.startswith('emulator-'), 'This test writes test data; use a disposable emulator.'
package='com.hwserve.still'
def run(*args, input=None):
 return subprocess.run([adb,'-s',serial,*args],input=input,capture_output=True,check=True).stdout
def shell(*args):return run('shell',*args).decode()
def launch():shell('am','start','-n',package+'/.MainActivity');time.sleep(.7)
def prefs():
 root=ET.fromstring(shell('run-as',package,'cat','shared_prefs/still.xml'))
 return {n.attrib['name']:n.attrib.get('value',n.text) for n in root}
def click(desc):
 shell('uiautomator','dump','/sdcard/still-ui.xml')
 xml=ET.fromstring(run('exec-out','cat','/sdcard/still-ui.xml'))
 for n in xml.iter('node'):
  if n.get('content-desc')==desc or n.get('text')==desc:
   x1,y1,x2,y2=map(int,re.findall(r'\d+',n.get('bounds')));shell('input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(.3);return
 raise AssertionError('Button not found: '+desc)
def seed(**kwargs):
 shell('am','force-stop',package)
 root=ET.Element('map')
 for k,v in kwargs.items():
  tag='boolean' if isinstance(v,bool) else 'long' if k in ['duration','remaining','deadline'] else 'int' if isinstance(v,int) else 'string'
  n=ET.SubElement(root,tag,name=k)
  if tag=='string':n.text=v
  else:n.set('value',str(v).lower())
 run('shell',f"run-as {package} sh -c 'cat > shared_prefs/still.xml'",input=ET.tostring(root))
checks=[]
def check(ok,name):
 assert ok,name;checks.append(name);print('PASS:',name,flush=True)
shell('pm','grant',package,'android.permission.POST_NOTIFICATIONS')
launch();seed(mode=0,completed=0,duration=60000,remaining=60000,running=False,history='[]');launch()
click('시작');time.sleep(.8);click('일시정지');p=prefs();check(p['running']=='false' and 0<int(p['remaining'])<60000,'pause saves remaining time')
paused=int(p['remaining']);time.sleep(.5);check(int(prefs()['remaining'])==paused,'paused countdown stays fixed')
click('시작');deadline=int(prefs()['deadline']);shell('input','keyevent','KEYCODE_HOME');shell('am','kill',package);time.sleep(.8);launch();check(prefs()['running']=='true' and int(prefs()['deadline'])==deadline,'background process recreation preserves deadline')
click('일시정지')
seed(mode=0,completed=0,duration=60000,remaining=6500,running=False,history='[]',sound=False,vibration=True,autoBreak=False);launch();click('시작');shell('input','keyevent','KEYCODE_HOME');time.sleep(7.5)
p=prefs();check(p['mode']=='1' and p['running']=='false' and p['completed']=='1','system alarm completes focus in background')
check(len(json.loads(p['history']))==1,'completed focus recorded once')
check(re.search(r'NotificationRecord\([^\n]*pkg=com.hwserve.still[^\n]*id=2', shell('dumpsys','notification')) is not None,'completion notification posted')
launch();launch();check(len(json.loads(prefs()['history']))==1,'reopening does not duplicate completion')
seed(mode=0,completed=3,duration=60000,remaining=6500,running=False,history='[]',autoBreak=True);launch();click('시작');shell('input','keyevent','KEYCODE_HOME');time.sleep(7.5);p=prefs();check(p['mode']=='2' and p['running']=='true' and p['completed']=='4','fourth focus starts long break automatically')
seed(mode=0,completed=0,duration=1500000,remaining=1500000,running=False,history='[]');launch()
Path('previews').mkdir(exist_ok=True)
shell('cmd','uimode','night','no');time.sleep(.8);launch()
Path('previews/phone-light.png').write_bytes(run('exec-out','screencap','-p'))
shell('cmd','uimode','night','yes');time.sleep(.8);launch();Path('previews/phone-dark.png').write_bytes(run('exec-out','screencap','-p'))
click('▥  기록');Path('previews/phone-statistics.png').write_bytes(run('exec-out','screencap','-p'))
click('⚙  설정');Path('previews/phone-settings.png').write_bytes(run('exec-out','screencap','-p'))
Path('docs/android-smoke-results.json').write_text(json.dumps({'device':'Android 14 emulator','passed':checks},ensure_ascii=False,indent=2)+'\n')
print('PASS:',len(checks),'Android integration checks')
