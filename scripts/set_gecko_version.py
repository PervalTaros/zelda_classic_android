"""Choisit une version de GeckoView (<= MAX majeure) et l'écrit dans android/app/build.gradle."""
import re, sys, urllib.request, xml.etree.ElementTree as ET, pathlib

MAX = 140  # version majeure maximale de GeckoView
url = 'https://maven.mozilla.org/maven2/org/mozilla/geckoview/geckoview/maven-metadata.xml'
xml = urllib.request.urlopen(url, timeout=120).read()
versions = [v.text.strip() for v in ET.fromstring(xml).iter('version')]
cands = []
for v in versions:
    m = re.fullmatch(r'(\d+)\.(\d+)\.(\d+)', v)
    if m and int(m.group(1)) <= MAX:
        cands.append((int(m.group(1)), int(m.group(2)), int(m.group(3)), v))
if not cands:
    sys.exit('Aucune version de GeckoView trouvée. Versions vues : ' + ', '.join(versions[-10:]))
best = max(cands)[3]
print('GeckoView retenu :', best)
gradle = pathlib.Path('android/app/build.gradle')
text = gradle.read_text()
if '__GECKO_VERSION__' not in text:
    sys.exit('Marqueur __GECKO_VERSION__ absent de build.gradle')
gradle.write_text(text.replace('__GECKO_VERSION__', best))
