"""Validate the single source of truth used by Gradle and GitHub Releases."""
import os,re
from pathlib import Path
values=dict(line.strip().split('=',1) for line in Path('version.properties').read_text().splitlines() if '=' in line)
version=values['versionName'];code=int(values['versionCode'])
assert re.fullmatch(r'\d+\.\d+\.\d+',version), 'Use a semantic version: major.minor.patch'
assert 1 <= code <= 2100000000, 'Invalid Android versionCode'
tag='v'+version
if os.environ.get('GITHUB_REF_TYPE')=='tag':
 assert os.environ['GITHUB_REF_NAME']==tag, 'Tag must match version.properties'
print('version='+version)
print('tag='+tag)
print('code='+str(code))
