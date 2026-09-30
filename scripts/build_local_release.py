"""Build a release using the private local key, without printing credentials."""
import json,os,subprocess
from pathlib import Path
root=Path(__file__).resolve().parents[1]
env=os.environ.copy()
env['STILL_KEYSTORE']=str(root/'.signing/still-release.jks')
env['STILL_KEYSTORE_PASSWORD']=json.loads((root/'.signing/local.json').read_text())['password']
subprocess.run([str(root/'gradlew'),':phone:assembleRelease',':wear:assembleRelease',':phone:lintRelease',':wear:lintRelease','--no-daemon'],cwd=root,env=env,check=True)
